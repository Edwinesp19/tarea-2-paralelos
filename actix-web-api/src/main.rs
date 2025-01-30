use actix_web::{web, App, HttpResponse, HttpServer, Responder};
use serde::{Deserialize, Serialize};
use mysql::*;
use mysql::prelude::*;
use dotenv::dotenv;
use std::env;
use reqwest::Client;

#[derive(Debug, Serialize, Deserialize)]
struct TaskAssignment {
    task_id: i32,
    user_ids: Vec<i32>
}

#[derive(Debug, Serialize, Deserialize)]
struct User {
    id: i32,
    name: String,
    email: String,
}

#[derive(Debug, Serialize, Deserialize)]
struct Task {
    id: i32,
    title: String,
    description: String,
}

async fn send_notification(user: &User, task: &Task) -> Result<(), Box<dyn std::error::Error>> {
    log::info!("Sending notification to user {} for task {}", user.id, task.id);
    let client = Client::new();
    let app_id = env::var("ONESIGNAL_APP_ID")?;
    let api_key = env::var("ONESIGNAL_REST_API_KEY")?;
    
    let notification = serde_json::json!({
        "app_id": app_id,
        "include_external_user_ids": [user.id.to_string()],
        "contents": {
            "en": format!("You have been assigned to task: {}", task.title)
        },
        "headings": {
            "en": "New Task Assignment"
        }
    });

    client
        .post("https://onesignal.com/api/v1/notifications")
        .header("Authorization", format!("Basic {}", api_key))
        .json(&notification)
        .send()
        .await?;
        
    log::info!("Successfully sent notification to user {} for task {}", user.id, task.id);

    Ok(())
}

async fn get_user(pool: &Pool, user_id: i32) -> Result<User, mysql::Error> {
    log::debug!("Fetching user details for user_id: {}", user_id);
    let mut conn = pool.get_conn()?;
    let user = conn.exec_first(
        "SELECT id, name, email FROM users WHERE id = ?",
        (user_id,)
    )?.ok_or(mysql::Error::MySqlError(mysql::MySqlError{
        state: String::from("02000"),
        message: format!("User {} not found", user_id),
        code: 100
    }))?;
    
    let (id, name, email) = mysql::from_row(user);
    let user = User { id, name, email };
    log::debug!("Successfully retrieved user: {}", user.id);
    Ok(user)
}

async fn get_task(pool: &Pool, task_id: i32) -> Result<Task, mysql::Error> {
    log::debug!("Fetching task details for task_id: {}", task_id);
    let mut conn = pool.get_conn()?;
    let task = conn.exec_first(
        "SELECT id, title, description FROM tasks WHERE id = ?",
        (task_id,)
    )?.ok_or(mysql::Error::MySqlError(mysql::MySqlError{
        state: String::from("02000"),
        message: format!("Task {} not found", task_id),
        code: 100
    }))?;
    
    let (id, title, description) = mysql::from_row(task);
    let task = Task { id, title, description };
    log::debug!("Successfully retrieved task: {}", task.id);
    Ok(task)
}

#[derive(Debug, Serialize)]
struct ErrorResponse {
    message: String,
    error_code: String,
}

async fn validate_task_assignment(pool: &Pool, task_id: i32, user_ids: &[i32]) -> Result<(), ErrorResponse> {
    log::info!("Validating task assignment - task_id: {}, user_ids: {:?}", task_id, user_ids);
    // Validate task exists
    if get_task(pool, task_id).await.is_err() {
        return Err(ErrorResponse {
            message: "Task not found".to_string(),
            error_code: "TASK_NOT_FOUND".to_string(),
        });
    }

    // Validate all users exist
    for user_id in user_ids {
        if get_user(pool, *user_id).await.is_err() {
            return Err(ErrorResponse {
                message: format!("User with id {} not found", user_id),
                error_code: "USER_NOT_FOUND".to_string(),
            });
        }
    }

    Ok(())
}

/// Assigns a task to one or more users and sends notifications
///
/// # Request Body
/// ```json
/// {
///     "task_id": 1,
///     "user_ids": [1, 2, 3]
/// }
/// ```
///
/// # Responses
/// - 200 OK: Task successfully assigned
/// - 400 Bad Request: Invalid input or duplicate assignment
/// - 404 Not Found: Task or user not found
/// - 500 Internal Server Error: Database or notification error
///
/// # Error Codes
/// - TASK_NOT_FOUND: The specified task does not exist
/// - USER_NOT_FOUND: One or more users do not exist
/// - DUPLICATE_ASSIGNMENT: User already assigned to the task
/// - DB_CONNECTION_ERROR: Failed to connect to database
/// - ASSIGNMENT_ERROR: Error during task assignment
/// - COMMIT_ERROR: Transaction commit failed
async fn assign_task(
    pool: web::Data<Pool>,
    assignment: web::Json<TaskAssignment>
) -> impl Responder {
    log::info!("Starting task assignment process for task_id: {}", assignment.task_id);

    // Validate assignment request
    if let Err(err) = validate_task_assignment(&pool, assignment.task_id, &assignment.user_ids).await {
        log::error!("Validation failed: {:?}", err);
        return HttpResponse::BadRequest().json(err);
    }

    // Get database connection
    let mut conn = match pool.get_conn() {
        Ok(conn) => conn,
        Err(e) => {
            log::error!("Database connection error: {}", e);
            return HttpResponse::InternalServerError().json(ErrorResponse {
                message: "Failed to connect to database".to_string(),
                error_code: "DB_CONNECTION_ERROR".to_string(),
            });
        }
    };

    // Start transaction
    let mut tx = match conn.start_transaction(TxOpts::default()) {
        Ok(tx) => tx,
        Err(e) => {
            log::error!("Transaction start error: {}", e);
            return HttpResponse::InternalServerError().json(ErrorResponse {
                message: "Failed to start database transaction".to_string(), 
                error_code: "TRANSACTION_ERROR".to_string(),
            });
        }
    };

    // Get task details
    let task = match get_task(&pool, assignment.task_id).await {
        Ok(task) => task,
        Err(e) => {
            log::error!("Task retrieval error: {}", e);
            return HttpResponse::NotFound().json(ErrorResponse {
                message: "Task not found".to_string(),
                error_code: "TASK_NOT_FOUND".to_string(),
            });
        }
    };

    // Process each user assignment
    for user_id in &assignment.user_ids {
        // Check for existing assignment
        let exists: Option<i32> = match tx.exec_first(
            "SELECT 1 FROM task_assignments WHERE task_id = ? AND user_id = ?",
            (assignment.task_id, user_id)
        ) {
            Ok(result) => result,
            Err(e) => {
                log::error!("Database query error: {}", e);
                if let Err(e) = tx.rollback() {
                    log::error!("Transaction rollback failed: {}", e);
                }
                return HttpResponse::InternalServerError().json(ErrorResponse {
                    message: "Failed to check existing assignment".to_string(),
                    error_code: "DB_QUERY_ERROR".to_string(),
                });
            }
        };
        
        if exists.is_some() {
            log::warn!("User {} already assigned to task {}", user_id, assignment.task_id);
            if let Err(e) = tx.rollback() {
                log::error!("Transaction rollback failed: {}", e);
            }
            return HttpResponse::BadRequest().json(ErrorResponse {
                message: format!("User {} is already assigned to this task", user_id),
                error_code: "DUPLICATE_ASSIGNMENT".to_string(),
            });
        }

        // Insert into task_assignments table
        if let Err(e) = tx.exec_drop(
            "INSERT INTO task_assignments (task_id, user_id) VALUES (?, ?)",
            (assignment.task_id, user_id)
        ) {
            log::error!("Failed to assign task to user {}: {}", user_id, e);
            if let Err(e) = tx.rollback() {
                log::error!("Transaction rollback failed: {}", e);
            }
            return HttpResponse::InternalServerError().json(ErrorResponse {
                message: "Failed to assign task".to_string(),
                error_code: "ASSIGNMENT_ERROR".to_string(),
            });
        }

        // Get user details and send notification
        match get_user(&pool, *user_id).await {
            Ok(user) => {
                if let Err(e) = send_notification(&user, &task).await {
                    log::error!("Failed to send notification to user {}: {}", user_id, e);
                    // Continue execution - notification failure shouldn't rollback assignment
                } else {
                    log::info!("Notification sent successfully to user {}", user_id);
                }
            }
            Err(e) => {
                log::error!("Failed to get user details for notification: {}", e);
                // Continue execution - notification failure shouldn't rollback assignment
            }
        }
    }

    // Commit transaction
    if let Err(e) = tx.commit() {
        log::error!("Transaction commit error: {}", e);
        return HttpResponse::InternalServerError().json(ErrorResponse {
            message: "Failed to commit transaction".to_string(),
            error_code: "COMMIT_ERROR".to_string(),
        });
    }

    log::info!("Successfully assigned task {} to {} users", assignment.task_id, assignment.user_ids.len());
    HttpResponse::Ok().json(serde_json::json!({
        "success": true,
        "data": {
            "message": "Task assigned successfully",
            "task_id": task.id,
            "assigned_users": assignment.user_ids
        }
    }))
}

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    dotenv().ok();
    
    // Configure logging with timestamp and level
    env_logger::Builder::from_env(env_logger::Env::default().default_filter_or("info"))
        .format(|buf, record| {
            use std::io::Write;
            writeln!(
                buf,
                "{} [{}] - {}",
                chrono::Local::now().format("%Y-%m-%d %H:%M:%S"),
                record.level(),
                record.args()
            )
        })
        .init();
        
    log::info!("Starting task assignment service");
    env_logger::init();

    let opts = OptsBuilder::new()
        .ip_or_hostname(Some("localhost"))
        .tcp_port(3306)
        .user(Some("root"))
        .pass(Some(""))
        .db_name(Some("rest_db"));

    let pool = Pool::new(opts)
        .expect("Failed to create pool");

    HttpServer::new(move || {
        App::new()
            .wrap(actix_web::middleware::Logger::default())
            .app_data(web::Data::new(pool.clone()))
            .service(
                web::resource("/api/task-assignments")
                    .route(web::post().to(assign_task))
            )
    })
    .bind(("127.0.0.1", 8080))?
    .run()
    .await
}
