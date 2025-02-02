use actix_web::{web, App, HttpResponse, HttpServer, Result};
use actix_web::middleware::Logger;
use serde::{Deserialize, Serialize};
use mysql::{Pool, TxOpts};
use mysql::prelude::*;
use std::env;
use std::collections::HashMap;
use dotenv::dotenv;
use log;

#[derive(Debug, Serialize, Deserialize)]
struct TaskAssignmentRequest {
    task_id: i32,
    user_ids: Vec<i32>
}

#[derive(Debug, Serialize, Deserialize)]
struct UpdateTaskStatusRequest {
    status_id: i64  // Using i64 to match MySQL's BIGINT
}

#[derive(Debug, Serialize)]
struct OneSignalNotification {
    app_id: String,
    contents: HashMap<String, String>,
    headings: HashMap<String, String>,
    include_player_ids: Vec<String>,
    data: HashMap<String, serde_json::Value>,
    url: Option<String>
}

    async fn update_task_status(
        pool: web::Data<Pool>,
        task_id: web::Path<i64>,
        req: web::Json<UpdateTaskStatusRequest>
    ) -> Result<HttpResponse> {
        let task_id = task_id.into_inner();
        let mut conn = pool.get_conn()
        .map_err(|e| actix_web::error::ErrorInternalServerError(e.to_string()))?;

    // Start transaction
    let mut tx = conn.start_transaction(TxOpts::default())
        .map_err(|e| actix_web::error::ErrorInternalServerError(e.to_string()))?;

    // Validate status exists
    let status_exists: Option<i64> = tx.exec_first(
        "SELECT id FROM task_statuses WHERE id = ?",
        (req.status_id,)
    ).map_err(|e| actix_web::error::ErrorInternalServerError(e.to_string()))?;

    if status_exists.is_none() {
        return Ok(HttpResponse::BadRequest().json(serde_json::json!({
            "success": false,
            "message": "Estado de tarea inválido"
        })));
    }

    // Get task information before update
    let task_info: Option<(String, String)> = tx.exec_first(
        "SELECT t.title, ts.name FROM tasks t 
        INNER JOIN task_statuses ts ON ts.id = ? 
        WHERE t.id = ?",
        (req.status_id, task_id)
    ).map_err(|e| actix_web::error::ErrorInternalServerError(e.to_string()))?;

    let (task_title, new_status) = match task_info {
        Some(info) => info,
        None => return Ok(HttpResponse::NotFound().json(serde_json::json!({
            "success": false,
            "message": "Tarea no encontrada"
        })))
    };

    // Update task status
    tx.exec_drop(
        "UPDATE tasks SET status_id = ? WHERE id = ?",
        (req.status_id, task_id)
    ).map_err(|e| actix_web::error::ErrorInternalServerError(e.to_string()))?;

    // Get assigned users
    let assigned_users: Vec<(i64, String, Option<String>)> = tx.exec_map(
        "SELECT u.id, u.name, d.player_id 
        FROM users u 
        INNER JOIN task_assignments ta ON ta.user_id = u.id 
        LEFT JOIN devices d ON d.user_id = u.id
        WHERE ta.task_id = ?",
        (task_id,),
        |(id, name, player_id)| (id, name, player_id)
    ).map_err(|e| actix_web::error::ErrorInternalServerError(e.to_string()))?;

    // Commit transaction
    tx.commit().map_err(|e| actix_web::error::ErrorInternalServerError(e.to_string()))?;

    // Send notifications to assigned users
    for (_, name, player_id) in assigned_users {
        if let Some(pid) = player_id {
            let mut contents = HashMap::new();
            let mut headings = HashMap::new();
            let mut data = HashMap::new();

            contents.insert("en".to_string(), 
                format!("Hola {}, el estado de la tarea '{}' ha cambiado a '{}'", 
                    name, task_title, new_status));
            headings.insert("en".to_string(), "Actualización de Tarea".to_string());
            data.insert("task_id".to_string(), serde_json::json!(task_id));

            let notification = OneSignalNotification {
                app_id: env::var("ONESIGNAL_APP_ID").expect("ONESIGNAL_APP_ID must be set"),
                contents,
                headings,
                include_player_ids: vec![pid],
                data,
                url: None
            };

            // Send notification asynchronously
            let client = reqwest::Client::new();
            let api_key = env::var("ONESIGNAL_REST_API_KEY")
                .expect("ONESIGNAL_REST_API_KEY must be set");
            
            tokio::spawn(async move {
                let _ = client
                    .post("https://onesignal.com/api/v1/notifications")
                    .header("Authorization", format!("Basic {}", api_key))
                    .json(&notification)
                    .send()
                    .await;
            });
        }
    }

    Ok(HttpResponse::Ok().json(serde_json::json!({
        "success": true,
        "message": "Estado de tarea actualizado exitosamente"
    })))
}

async fn assign_task(
    pool: web::Data<Pool>,
    assignment: web::Json<TaskAssignmentRequest>,
) -> Result<HttpResponse> {
    let mut conn = pool.get_conn()
        .map_err(|e| actix_web::error::ErrorInternalServerError(e.to_string()))?;

    let mut tx = conn.start_transaction(TxOpts::default())
        .map_err(|e| actix_web::error::ErrorInternalServerError(e.to_string()))?;

    let task_id = assignment.task_id;

    // Get task title for notifications
    let task_title: String = tx
        .exec_first(
            "SELECT title FROM tasks WHERE id = ?",
            (task_id,)
        )
        .map_err(|e| actix_web::error::ErrorInternalServerError(e.to_string()))?
        .ok_or_else(|| actix_web::error::ErrorNotFound("Tarea no encontrada"))?;

    // Get current assignments for this task
    let current_assignments: Vec<i32> = tx
        .exec_map(
            "SELECT user_id FROM task_assignments WHERE task_id = ?",
            (task_id,),
            |user_id: i32| user_id
        )
        .map_err(|e| actix_web::error::ErrorInternalServerError(e.to_string()))?;

    // Users to be removed (current users not in new list)
    let users_to_remove: Vec<i32> = current_assignments.clone()
        .into_iter()
        .filter(|user_id| !assignment.user_ids.contains(user_id))
        .collect();

    // Remove assignments for users not in new list
    if !users_to_remove.is_empty() || assignment.user_ids.is_empty() {
        if assignment.user_ids.is_empty() {
            // If users array is empty, remove all assignments for this task
            tx.exec_drop(
                "DELETE FROM task_assignments WHERE task_id = ?",
                (task_id,)
            )
        } else {
            // Convert Vec<i32> to a comma-separated string of user IDs
            let user_ids_str: String = users_to_remove
                .iter()
                .map(|id| id.to_string())
                .collect::<Vec<String>>()
                .join(",");
            
            tx.exec_drop(
                "DELETE FROM task_assignments WHERE task_id = ? AND user_id IN (?)",
                (task_id, user_ids_str)
            )
        }
        .map_err(|e| actix_web::error::ErrorInternalServerError(e.to_string()))?;
    }

    // Find new assignments (users in new list that weren't in current list)
    let new_assignments: Vec<i32> = assignment.user_ids
        .iter()
        .filter(|&user_id| !current_assignments.contains(user_id))
        .cloned()
        .collect();

    // Get user data only for new assignments
    let mut user_data = Vec::new();
    for user_id in &new_assignments {
        let devices: Vec<(i32, String, String)> = tx
            .exec_map(
                "SELECT u.id, u.name, d.player_id 
                FROM users u 
                INNER JOIN devices d ON u.id = d.user_id 
                WHERE u.id = ?",
                (*user_id,),
                |(id, name, player_id): (i32, String, String)| (id, name, player_id)
            )
            .map_err(|e| actix_web::error::ErrorInternalServerError(e.to_string()))?;
        
        user_data.extend(devices);
    }

    // Insert only new assignments
    for user_id in &new_assignments {
        tx.exec_drop(
            "INSERT INTO task_assignments (task_id, user_id) VALUES (?, ?)",
            (task_id, user_id)
        )
        .map_err(|e| actix_web::error::ErrorInternalServerError(e.to_string()))?;
    }

    // Commit database transaction
    tx.commit()
        .map_err(|e| actix_web::error::ErrorInternalServerError(e.to_string()))?;

    // Send notifications only to new users
    if !user_data.is_empty() {
        let mut contents = HashMap::new();
        let mut headings = HashMap::new();
        let mut data = HashMap::new();

        data.insert("assignment".to_string(), serde_json::json!(format!("/tasks/{}", task_id)));
        data.insert("task_id".to_string(), serde_json::json!(task_id));
        headings.insert("en".to_string(), "Nueva Tarea Asignada".to_string());

        for (_, name, _) in &user_data {
            contents.insert("en".to_string(), format!("Hola {}, se te ha asignado la tarea: {}. Toca para ver los detalles.", name, task_title));
        }

        let notification = OneSignalNotification {
            app_id: env::var("ONESIGNAL_APP_ID").expect("ONESIGNAL_APP_ID must be set"),
            contents,
            headings,
            include_player_ids: user_data.iter().map(|(_, _, player_id)| player_id.clone()).collect(),
            data,
            url: None
        };

        let client = reqwest::Client::new();
        let _res = client
            .post("https://onesignal.com/api/v1/notifications")
            .header("Authorization", format!("Basic {}", env::var("ONESIGNAL_REST_API_KEY").expect("ONESIGNAL_REST_API_KEY must be set")))
            .json(&notification)
            .send()
            .await
            .map_err(|e| actix_web::error::ErrorInternalServerError(e.to_string()))?;
    }

    let message = if assignment.user_ids.is_empty() {
        "Todas las asignaciones han sido eliminadas.".to_string()
    } else {
        format!(
            "Tarea actualizada exitosamente. {} nuevas asignaciones, {} asignaciones eliminadas.",
            new_assignments.len(),
            users_to_remove.len()
        )
    };

    Ok(HttpResponse::Ok().json(serde_json::json!({
        "success": true,
        "message": message
    })))
}

fn get_connection_string() -> String {
    env::var("DATABASE_URL").expect("DATABASE_URL must be set")
}

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    dotenv().ok();
    env_logger::init_from_env(env_logger::Env::default().default_filter_or("info"));

    let pool = mysql::Pool::new(get_connection_string().as_str())
        .expect("Failed to create pool");

    log::info!("Starting Task Assignment service at http://127.0.0.1:8081");
    log::info!("Available endpoints:");
    log::info!("  POST /api/taskAssignments/assign - Assign tasks to users and send notifications");
    log::info!("  PUT /api/taskStatus/<id> - Update task status with notifications");

    HttpServer::new(move || {
        App::new()
            .wrap(Logger::default())  // Add default request logger
            .wrap(Logger::new("%a \"%r\" %s %b \"%{Referer}i\" \"%{User-Agent}i\" %T"))  // Add custom format logger
            .app_data(web::Data::new(pool.clone()))
            .service(
                web::scope("/api")
                    .service(
                        web::scope("/taskAssignments")
                            .route("/assign", web::post().to(assign_task))
                    )
                    .service(
                        web::resource("/taskStatus/{id}")
                            .route(web::put().to(update_task_status))
                    )
            )
    })
    .bind("0.0.0.0:8081")?
    .run()
    .await
}
