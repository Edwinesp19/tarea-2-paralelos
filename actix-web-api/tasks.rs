use actix_web::{web, HttpResponse};
use serde::{Deserialize, Serialize};
use mysql::{Pool, Row, prelude::*};
use serde_json::json;

#[derive(Deserialize)]
pub struct UpdateTaskStatusRequest {
    status_id: i32
}

#[derive(Serialize)]
pub struct ApiResponse {
    success: bool,
    message: String
}

pub async fn update_task_status(
    pool: web::Data<Pool>,
    task_id: web::Path<i32>,
    req: web::Json<UpdateTaskStatusRequest>
) -> HttpResponse {
    // Start transaction
    let mut conn = match pool.get_conn() {
        Ok(conn) => conn,
        Err(_) => return HttpResponse::InternalServerError().json(ApiResponse {
            success: false,
            message: "Error al obtener conexión".to_string()
        })
    };

    // Start transaction
    if let Err(_) = conn.query_drop("START TRANSACTION") {
        return HttpResponse::InternalServerError().json(ApiResponse {
            success: false,
            message: "Error al iniciar la transacción".to_string()
        });
    }

    // Validate status exists
    let status_exists: Option<bool> = match conn.query_first(
        "SELECT EXISTS(SELECT 1 FROM task_statuses WHERE id = ?)"
    ) {
        Ok(row) => row,
        Err(_) => {
            return HttpResponse::InternalServerError().json(ApiResponse {
                success: false,
                message: "Error al validar el estado".to_string()
            })
        }
    };

    if !status_exists {
        return HttpResponse::BadRequest().json(ApiResponse {
            success: false,
            message: "El estado especificado no existe".to_string()
        });
    }

    // Update task status
    if let Err(_) = conn.exec_drop(
        "UPDATE tasks SET status_id = ? WHERE id = ?",
        (req.status_id, *task_id)
    ) {
        return HttpResponse::InternalServerError().json(ApiResponse {
            success: false,
            message: "Error al actualizar el estado de la tarea".to_string()
        });
    }

    // Get assigned users
    let assigned_users = match conn.exec::<Row, _, _>(
        "SELECT u.id, u.player_id 
        FROM users u
        INNER JOIN task_assignments ta ON ta.user_id = u.id
        WHERE ta.task_id = ?",
        (*task_id,)
    ) {
        Ok(rows) => rows,
        Err(_) => {
            return HttpResponse::InternalServerError().json(ApiResponse {
                success: false,
                message: "Error al obtener usuarios asignados".to_string()
            });
        }
    };

    // Get task and status info for notification
    let task_info = match conn.exec_first::<Row, _, _>(
        "SELECT t.title, ts.name as status_name 
        FROM tasks t
        INNER JOIN task_statuses ts ON ts.id = ?
        WHERE t.id = ?",
        (req.status_id, *task_id)
    ) {
        Ok(Some(row)) => row,
        Err(_) => {
            return HttpResponse::InternalServerError().json(ApiResponse {
                success: false,
                message: "Error al obtener información de la tarea".to_string()
            });
        }
    };

    // Commit transaction
    if let Err(_) = conn.query_drop("COMMIT") {
        let _ = conn.query_drop("ROLLBACK");
        return HttpResponse::InternalServerError().json(ApiResponse {
            success: false,
            message: "Error al confirmar la transacción".to_string()
        });
    }

    // Send notifications to assigned users
    for user in assigned_users {
        if let Some(player_id) = user.get::<String, _>("player_id") {
            // Send OneSignal notification
            let notification = json!({
                "app_id": std::env::var("ONESIGNAL_APP_ID").unwrap_or_default(),
                "include_player_ids": [player_id],
                "contents": {
                    "en": format!(
                        "La tarea '{}' ha cambiado su estado a '{}'",
                        task_info.get::<String, _>("title").unwrap_or_default(),
                        task_info.get::<String, _>("status_name").unwrap_or_default()
                    )
                }
            });

            // Send notification asynchronously
            tokio::spawn(async move {
                let client = reqwest::Client::new();
                let _ = client
                    .post("https://onesignal.com/api/v1/notifications")
                    .header("Authorization", format!("Basic {}", std::env::var("ONESIGNAL_API_KEY").unwrap_or_default()))
                    .json(&notification)
                    .send()
                    .await;
            });
        }
    }

    HttpResponse::Ok().json(ApiResponse {
        success: true,
        message: "Status de tarea actualizado exitosamente".to_string()
    })
}

