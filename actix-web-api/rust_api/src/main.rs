use actix_web::{web, App, HttpResponse, HttpServer, middleware::Logger};
use actix_multipart::Multipart;
use futures::TryStreamExt;
use uuid::Uuid;
use std::io::Write;
use std::path::Path;
use mysql::{Pool, TxOpts};
use actix_web::error::{Error, ErrorInternalServerError};
use std::env;
use serde::{Deserialize, Serialize};
use chrono::NaiveDateTime;
use mysql::prelude::FromRow;
use mysql::*;
use mysql::prelude::*;
use bcrypt::{verify, DEFAULT_COST};
use std::fmt;
use log;

#[derive(Debug)]
pub enum ApiError {
    DbError(mysql::Error),
    BcryptError(bcrypt::BcryptError),
    ValidationError(String),
    FileError(String),
}

impl fmt::Display for ApiError {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        match self {
            ApiError::DbError(e) => write!(f, "Database error: {}", e),
            ApiError::BcryptError(e) => write!(f, "Bcrypt error: {}", e),
            ApiError::ValidationError(e) => write!(f, "{}", e),
            ApiError::FileError(e) => write!(f, "File error: {}", e),
        }
    }
}

impl From<mysql::Error> for ApiError {
    fn from(error: mysql::Error) -> Self {
        ApiError::DbError(error)
    }
}

impl From<bcrypt::BcryptError> for ApiError {
    fn from(error: bcrypt::BcryptError) -> Self {
        ApiError::BcryptError(error)
    }
}

impl From<ApiError> for Error {
    fn from(error: ApiError) -> Self {
        ErrorInternalServerError(error.to_string())
    }
}

#[derive(Debug, Serialize, Deserialize, FromRow)]
struct TaskStatus {
    id: i32,
    name: String,
}

#[derive(Debug, Serialize, Deserialize)]
struct UserUpdateRequest {
    name: String,
    email: String,
}

#[derive(Debug, Serialize, Deserialize)]
struct TaskAssignmentRequest {
    task_id: i32,
    user_ids: Vec<i32>,
}

#[derive(Debug, Serialize, Deserialize)]
struct AssignedUser {
    id: i32,
    name: String,
    email: String,
}

#[derive(Debug, Serialize, Deserialize)]
struct TaskWithAssignments {
    id: i32,
    title: String,
    description: String,
    status_id: i32,
    status: String,
    date_from: String,
    due_date: String,
    assigned_users: Vec<AssignedUser>,
}

#[derive(Debug, Serialize, Deserialize)]
struct UserAssignments {
    user_id: i32,
    user: AssignedUser,
    tasks: Vec<TaskWithAssignments>,
}

#[derive(Debug, Serialize)]
struct UserTaskAssignment {
    user_id: i32,
    name: String,
    email: String,
    tasks: Vec<TaskWithAssignments>,
}

#[derive(Debug, Serialize, Deserialize)]
struct Task {
    id: Option<i32>,
    title: String,
    description: Option<String>,
    status_id: i32,
    status: Option<String>,
    date_from: String,
    due_date: String,
}

#[derive(Debug, Serialize, Deserialize)]
struct TaskCreateRequest {
    title: String,
    description: Option<String>,
    status_id: i32,
    date_from: String,
    due_date: String,
}

fn is_valid_date(date_str: &str) -> bool {
    // Try different date formats
    NaiveDateTime::parse_from_str(date_str, "%Y-%m-%d %H:%M:%S").is_ok() ||
    NaiveDateTime::parse_from_str(date_str, "%d-%m-%Y %H:%M:%S").is_ok() ||
    NaiveDateTime::parse_from_str(&format!("{} 00:00:00", date_str), "%Y-%m-%d %H:%M:%S").is_ok() ||
    NaiveDateTime::parse_from_str(&format!("{} 00:00:00", date_str), "%d-%m-%Y %H:%M:%S").is_ok()
}

async fn create_task(task: web::Json<TaskCreateRequest>, pool: web::Data<Pool>) -> Result<HttpResponse, Error> {
    log::debug!("Received task creation request: {:?} to URL /api/tasks", task);
    
    // Validate title
    if task.title.trim().is_empty() {
        return Ok(HttpResponse::BadRequest().json(serde_json::json!({
            "success": false,
            "message": "El título es requerido"
        })));
    }
    
    if task.title.len() < 3 {
        return Ok(HttpResponse::BadRequest().json(serde_json::json!({
            "success": false,
            "message": "El título debe tener al menos 3 caracteres"
        })));
    }
    
    // Validate dates
    if !is_valid_date(&task.date_from) {
        return Ok(HttpResponse::BadRequest().json(serde_json::json!({
            "success": false,
            "message": "Formato de fecha inicial inválido. Formatos permitidos: YYYY-MM-DD, DD-MM-YYYY, o con hora (HH:MM:SS)",
            "example": {
                "title": "Ejemplo de tarea",
                "description": "Descripción de ejemplo",
                "status_id": 1,
                "date_from": "2024-01-21 00:00:00",
                "due_date": "2024-01-27 00:00:00"
            }
        })));
    }
    
    if !is_valid_date(&task.due_date) {
        return Ok(HttpResponse::BadRequest().json(serde_json::json!({
            "success": false,
            "message": "Formato de fecha final inválido. Formatos permitidos: YYYY-MM-DD, DD-MM-YYYY, o con hora (HH:MM:SS)",
            "example": {
                "title": "Ejemplo de tarea",
                "description": "Descripción de ejemplo",
                "status_id": 1,
                "date_from": "2024-01-21 00:00:00",
                "due_date": "2024-01-27 00:00:00"
            }
        })));
    }
    
    let mut conn = pool.get_conn()
        .map_err(|e| ApiError::DbError(e))?;
    
    // Validate status exists
    let status_exists: Option<i32> = conn.exec_first(
        "SELECT id FROM task_statuses WHERE id = ?",
        (task.status_id,)
    ).map_err(|e| ApiError::DbError(e))?;
    
    if status_exists.is_none() {
        return Ok(HttpResponse::BadRequest().json(serde_json::json!({
            "success": false,
            "message": "Estado de tarea inválido. Estados disponibles: 1 (Pendiente), 2 (Completado)",
            "example": {
                "title": "Ejemplo de tarea",
                "description": "Descripción de ejemplo",
                "status_id": 1,
                "date_from": "2024-01-21 00:00:00",
                "due_date": "2024-01-27 00:00:00"
            }
        })));
    }
    
    // Insert task
    let result = conn.exec_drop(
        "INSERT INTO tasks (title, description, status_id, date_from, due_date) VALUES (?, ?, ?, ?, ?)",
        (
            task.title.clone(),
            task.description.clone().unwrap_or_default(),
            task.status_id,
            task.date_from.clone(),
            task.due_date.clone()
        )
    ).map_err(|e| ApiError::DbError(e));
    
    match result {
        Ok(_) => Ok(HttpResponse::Ok().json(serde_json::json!({
            "success": true,
            "message": "Tarea creada exitosamente"
        }))),
        Err(e) => Ok(HttpResponse::InternalServerError().json(serde_json::json!({
            "success": false,
            "message": format!("Error al crear la tarea: {}", e)
        })))
    }
}

async fn get_task(id: web::Path<i32>, pool: web::Data<Pool>) -> Result<HttpResponse, Error> {
    let mut conn = pool.get_conn()
        .map_err(|e| ApiError::DbError(e))?;
    
    let task: Option<(i32, String, String, i32, String, String, String)> = conn
        .exec_first(
            r"SELECT t.id, t.title, t.description, t.status_id, ts.name as status,
            DATE_FORMAT(t.date_from, '%Y-%m-%d') as date_from,
            DATE_FORMAT(t.due_date, '%Y-%m-%d') as due_date
            FROM tasks t
            JOIN task_statuses ts ON t.status_id = ts.id
            WHERE t.id = ?",
            (id.into_inner(),)
        )
        .map_err(|e| ApiError::DbError(e))?;
    
    match task {
        Some((id, title, description, status_id, status, date_from, due_date)) => {
            Ok(HttpResponse::Ok().json(serde_json::json!({
                "success": true,
                "task": {
                    "id": id,
                    "title": title,
                    "description": description,
                    "status_id": status_id,
                    "status": status,
                    "date_from": date_from,
                    "due_date": due_date
                }
            })))
        }
        None => Ok(HttpResponse::NotFound().json(serde_json::json!({
            "success": false,
            "message": "Tarea no encontrada"
        })))
    }
}

async fn list_tasks(pool: web::Data<Pool>) -> Result<HttpResponse, Error> {
    let mut conn = pool.get_conn()
        .map_err(|e| ApiError::DbError(e))?;
    
    let tasks: Vec<Task> = conn
        .exec_map(
            "SELECT t.id, t.title, t.description, t.status_id, ts.name as status_name,
                    DATE_FORMAT(t.date_from, '%Y-%m-%d') as date_from, 
                    DATE_FORMAT(t.due_date, '%Y-%m-%d') as due_date 
            FROM tasks t
            LEFT JOIN task_statuses ts ON t.status_id = ts.id
            ORDER BY t.id",
            (),
            |(id, title, description, status_id, status_name, date_from, due_date): (Option<i32>, String, Option<String>, i32, Option<String>, String, String)| {
                Task {
                    id,
                    title,
                    description,
                    status_id,
                    status: status_name,
                    date_from,
                    due_date,
                }
            },
        )
        .map_err(|e| ApiError::DbError(e))?;
    
    Ok(HttpResponse::Ok().json(serde_json::json!({
        "success": true,
        "tasks": tasks
    })))
}

async fn update_task(id: web::Path<i32>, task: web::Json<TaskCreateRequest>, pool: web::Data<Pool>) -> Result<HttpResponse, Error> {
    log::debug!("Received task update request for id {}: {:?}", id, task);
    
    // Validate title
    if task.title.trim().is_empty() {
        return Ok(HttpResponse::BadRequest().json(serde_json::json!({
            "success": false,
            "message": "El título es requerido"
        })));
    }
    
    if task.title.len() < 3 {
        return Ok(HttpResponse::BadRequest().json(serde_json::json!({
            "success": false,
            "message": "El título debe tener al menos 3 caracteres"
        })));
    }
    
    // Validate dates
    if !is_valid_date(&task.date_from) {
        return Ok(HttpResponse::BadRequest().json(serde_json::json!({
            "success": false,
            "message": "Formato de fecha inicial inválido. Use YYYY-MM-DD HH:MM:SS"
        })));
    }
    
    if !is_valid_date(&task.due_date) {
        return Ok(HttpResponse::BadRequest().json(serde_json::json!({
            "success": false,
            "message": "Formato de fecha final inválido. Use YYYY-MM-DD HH:MM:SS"
        })));
    }
    
    let mut conn = pool.get_conn()
        .map_err(|e| ApiError::DbError(e))?;
    
    // Validate status exists
    let status_exists: Option<i32> = conn.exec_first(
        "SELECT id FROM task_statuses WHERE id = ?",
        (task.status_id,)
    ).map_err(|e| ApiError::DbError(e))?;
    
    if status_exists.is_none() {
        return Ok(HttpResponse::BadRequest().json(serde_json::json!({
            "success": false,
            "message": "Estado de tarea inválido"
        })));
    }
    
    // Validate task exists
    let task_exists: Option<i32> = conn.exec_first(
        "SELECT id FROM tasks WHERE id = ?",
        (id.into_inner(),)
    ).map_err(|e| ApiError::DbError(e))?;
    
    if task_exists.is_none() {
        return Ok(HttpResponse::NotFound().json(serde_json::json!({
            "success": false,
            "message": "Tarea no encontrada"
        })));
    }
    
    // Update task
    let result = conn.exec_drop(
        "UPDATE tasks SET title = ?, description = ?, status_id = ?, date_from = ?, due_date = ? WHERE id = ?",
        (
            task.title.clone(),
            task.description.clone().unwrap_or_default(),
            task.status_id, 
            task.date_from.clone(),
            task.due_date.clone(),
            task_exists.unwrap()
        )
    ).map_err(|e| ApiError::DbError(e));
    
    match result {
        Ok(_) => Ok(HttpResponse::Ok().json(serde_json::json!({
            "success": true,
            "message": "Tarea actualizada exitosamente"
        }))),
        Err(e) => Ok(HttpResponse::InternalServerError().json(serde_json::json!({
            "success": false,
            "message": format!("Error al actualizar la tarea: {}", e)
        })))
    }
}

async fn delete_task(id: web::Path<i32>, pool: web::Data<Pool>) -> Result<HttpResponse, Error> {
    let mut conn = pool.get_conn()
        .map_err(|e| ApiError::DbError(e))?;
    
    // Validate task exists
    let task_exists: Option<i32> = conn.exec_first(
        "SELECT id FROM tasks WHERE id = ?",
        (id.into_inner(),)
    ).map_err(|e| ApiError::DbError(e))?;
    
    if task_exists.is_none() {
        return Ok(HttpResponse::NotFound().json(serde_json::json!({
            "success": false,
            "message": "Tarea no encontrada"
        })));
    }
    
    // Delete task
    let result = conn.exec_drop(
        "DELETE FROM tasks WHERE id = ?",
        (task_exists.unwrap(),)
    ).map_err(|e| ApiError::DbError(e));
    
    match result {
        Ok(_) => Ok(HttpResponse::Ok().json(serde_json::json!({
            "success": true,
            "message": "Tarea eliminada exitosamente"
        }))),
        Err(e) => Ok(HttpResponse::InternalServerError().json(serde_json::json!({
            "success": false,
            "message": format!("Error al eliminar la tarea: {}", e)
        })))
    }
    }

async fn get_user_task_assignments(pool: web::Data<Pool>) -> Result<HttpResponse, Error> {
    let mut conn = pool.get_conn()
        .map_err(|e| ApiError::DbError(e))?;

    // First, get all users that have task assignments
    let users: Vec<(i32, String, String)> = conn
        .query_map(
            "SELECT DISTINCT u.id, u.name, u.email 
            FROM users u 
            INNER JOIN task_assignments ta ON u.id = ta.user_id 
            ORDER BY u.id",
            |(id, name, email)| (id, name, email),
        )
        .map_err(|e| ApiError::DbError(e))?;

    let mut user_assignments = Vec::new();

    // For each user, get their tasks with all assignments
    for (user_id, name, email) in users {
        let tasks: Vec<TaskWithAssignments> = conn
            .exec_map(
                "SELECT t.id, t.title, COALESCE(t.description, '') as description, \
                t.status_id, ts.name as status, \
                DATE_FORMAT(t.date_from, '%Y-%m-%d') as date_from, \
                DATE_FORMAT(t.due_date, '%Y-%m-%d') as due_date, \
                GROUP_CONCAT(DISTINCT u2.id) as user_ids, \
                GROUP_CONCAT(DISTINCT u2.name) as user_names, \
                GROUP_CONCAT(DISTINCT u2.email) as user_emails \
                FROM tasks t \
                INNER JOIN task_assignments ta1 ON t.id = ta1.task_id AND ta1.user_id = ? \
                LEFT JOIN task_assignments ta2 ON t.id = ta2.task_id \
                LEFT JOIN users u2 ON ta2.user_id = u2.id \
                LEFT JOIN task_statuses ts ON t.status_id = ts.id \
                GROUP BY t.id",
                (user_id,),
                |(id, title, description, status_id, status, date_from, due_date, user_ids, user_names, user_emails)| {
                    let assigned_users = if let (Some(ids), Some(names), Some(emails)) = (user_ids, user_names, user_emails) {
                        let ids: String = ids;
                        let names: String = names;
                        let emails: String = emails;
                        
                        let ids: Vec<&str> = ids.split(',').collect();
                        let names: Vec<&str> = names.split(',').collect();
                        let emails: Vec<&str> = emails.split(',').collect();
                        
                        ids.iter()
                            .zip(names.iter())
                            .zip(emails.iter())
                            .map(|((id, name), email)| AssignedUser {
                                id: id.parse().unwrap_or(0),
                                name: name.to_string(),
                                email: email.to_string(),
                            })
                            .collect()
                    } else {
                        Vec::new()
                    };

                    TaskWithAssignments {
                        id,
                        title,
                        description,
                        status_id,
                        status,
                        date_from,
                        due_date,
                        assigned_users,
                    }
                },
            )
            .map_err(|e| ApiError::DbError(e))?;

        user_assignments.push(UserTaskAssignment {
            user_id,
            name,
            email,
            tasks,
        });
    }

    Ok(HttpResponse::Ok().json(serde_json::json!({
        "success": true,
        "assignments": user_assignments
    })))
}

#[derive(Debug, Serialize, Deserialize)]
struct TaskResponse {
    id: i32,
    title: String,
    description: String,
    status: String,
    date_from: String,
    due_date: String
}

#[derive(Debug, Serialize, Deserialize, FromRow)]
struct UserList {
    id: i32,
    name: String,
    email: String
}

#[derive(Debug, Serialize, Deserialize)]
struct User {
    name: String,
    email: String,
    password: String
}

#[derive(Debug, Serialize, Deserialize)]
struct LoginRequest {
    email: String,
    password: String,
    player_id: Option<String>,
}
#[derive(Debug, Serialize, Deserialize)]
struct RegisterResponse {
    success: bool,
    message: String,
}

fn is_valid_email(email: &str) -> bool {
    email.contains('@') && email.contains('.')
}

#[derive(Debug, Serialize, Deserialize)]
struct UserData {
    id: i32,
    name: String,
    email: String,
}

#[derive(Debug, Serialize, Deserialize)]
struct UserResponse {
    success: bool,
    message: String,
    user: UserData
}

#[derive(Debug, Serialize, Deserialize)]
struct LoginResponse {
    success: bool,
    message: String,
}

async fn login(credentials: web::Json<LoginRequest>, pool: web::Data<Pool>) -> Result<HttpResponse, Error> {
    log::debug!("Received login request for email: {}", credentials.email);
    let mut conn = pool.get_conn()
        .map_err(|e| ApiError::DbError(e))?;

    // Start transaction
    let mut tx = conn.start_transaction(TxOpts::default())
        .map_err(|e| ApiError::DbError(e))?;

    // Query for user with matching email
    let result = tx.exec_first::<(i32, String, String, String), _, _>(
        "SELECT id, name, email, password FROM users WHERE email = ?",
        (credentials.email.clone(),),
    ).map_err(|e| ApiError::DbError(e))?;

    match result {
        Some((user_id, db_name, db_email, db_password)) => {
            // Verify password using bcrypt
            match verify(&credentials.password, &db_password).map_err(|e| ApiError::BcryptError(e))? {
                true => {
                    // If player_id is provided, register it
                    if let Some(player_id) = &credentials.player_id {
                        // Check if player_id already exists
                        let existing_device: Option<i32> = tx.exec_first(
                            "SELECT id FROM devices WHERE player_id = ?",
                            (player_id,)
                        ).map_err(|e| ApiError::DbError(e))?;

                        if existing_device.is_none() {
                            // Register new device
                            tx.exec_drop(
                                "INSERT INTO devices (user_id, player_id) VALUES (?, ?)",
                                (user_id, player_id)
                            ).map_err(|e| ApiError::DbError(e))?;
                        }
                    }

                    // Commit transaction
                    tx.commit().map_err(|e| ApiError::DbError(e))?;

                    Ok(HttpResponse::Ok().json(UserResponse {
                        success: true,
                        message: "Inicio de sesión exitoso".to_string(),
                        user: UserData {
                            id: user_id,
                            name: db_name,
                            email: db_email,
                        }
                    }))
                },
                false => Ok(HttpResponse::Unauthorized().json(LoginResponse {
                    success: false,
                    message: "Combinación de correo o contraseña inválida. Por favor intente de nuevo.".to_string(),
                })),
            }
        }
        None => Ok(HttpResponse::Unauthorized().json(LoginResponse {
            success: false,
            message: "No se encontró cuenta con este correo. Por favor regístrese primero.".to_string(),
        })),
    }
}


async fn register(user: web::Json<User>, pool: web::Data<Pool>) -> Result<HttpResponse, Error> {
    log::debug!("Received registration request: {:?}", user);
    
    // Validate name
    if user.name.trim().is_empty() {
        log::debug!("Validation failed: name is empty");
        return Ok(HttpResponse::BadRequest().json(RegisterResponse {
            success: false,
            message: "El nombre es requerido".to_string(),
        }));
    }
    
    if user.name.len() < 2 {
        log::debug!("Validation failed: name too short ({})", user.name.len());
        return Ok(HttpResponse::BadRequest().json(RegisterResponse {
            success: false,
            message: "El nombre debe tener al menos 2 caracteres".to_string(),
        }));
    }

    if !is_valid_email(&user.email) {
        log::debug!("Validation failed: invalid email format ({})", user.email);
        return Ok(HttpResponse::BadRequest().json(RegisterResponse {
            success: false,
            message: "Formato de correo inválido: debe contener @ y .".to_string(),
        }));
    }
        if user.password.len() < 6 {
            log::debug!("Validation failed: password too short ({})", user.password.len());
            return Ok(HttpResponse::BadRequest().json(RegisterResponse {
                success: false,
                message: "La contraseña debe tener al menos 6 caracteres por seguridad".to_string(),
            }));
        }

    let mut conn = pool.get_conn()
        .map_err(|e| ApiError::DbError(e))?;

    // Check if user already exists
    let existing_user: Option<String> = conn
        .exec_first(
            "SELECT email FROM users WHERE email = ?",
            (user.email.clone(),)
        )
        .map_err(|e| ApiError::DbError(e))?;

    if existing_user.is_some() {
        return Ok(HttpResponse::Conflict().json(RegisterResponse {
            success: false,
            message: "El usuario ya existe".to_string(),
        }));
    }

    // Hash password
    let hashed_password = bcrypt::hash(&user.password, DEFAULT_COST)
        .map_err(|e| ApiError::BcryptError(e))?;

    // Insert new user
    conn.exec_drop(
        "INSERT INTO users (name, email, password) VALUES (?, ?, ?)",
        (user.name.clone(), user.email.clone(), hashed_password),
    )
    .map_err(|e| ApiError::DbError(e))?;

    Ok(HttpResponse::Ok().json(RegisterResponse {
        success: true,
        message: "Usuario registrado exitosamente".to_string(),
    }))
}

async fn update_user(id: web::Path<i32>, user_data: web::Json<UserUpdateRequest>, pool: web::Data<Pool>) -> Result<HttpResponse, Error> {
// Validate name
if user_data.name.trim().is_empty() {
return Ok(HttpResponse::BadRequest().json(serde_json::json!({
    "success": false,
    "message": "El nombre es requerido"
})));
}

if user_data.name.len() < 2 {
return Ok(HttpResponse::BadRequest().json(serde_json::json!({
    "success": false,
    "message": "El nombre debe tener al menos 2 caracteres"
})));
}

if !is_valid_email(&user_data.email) {
return Ok(HttpResponse::BadRequest().json(serde_json::json!({
    "success": false,
    "message": "Formato de correo inválido: debe contener @ y ."
})));
}

let mut conn = pool.get_conn()
.map_err(|e| ApiError::DbError(e))?;

// Check if user exists
let user_exists: Option<i32> = conn.exec_first(
"SELECT id FROM users WHERE id = ?",
(id.into_inner(),)
).map_err(|e| ApiError::DbError(e))?;

if user_exists.is_none() {
return Ok(HttpResponse::NotFound().json(serde_json::json!({
    "success": false,
    "message": "Usuario no encontrado"
})));
}

// Check if email is already taken by another user
let existing_email: Option<(i32, String)> = conn.exec_first(
"SELECT id, email FROM users WHERE email = ? AND id != ?",
(&user_data.email, user_exists.unwrap())
).map_err(|e| ApiError::DbError(e))?;

if let Some((_, email)) = existing_email {
return Ok(HttpResponse::Conflict().json(serde_json::json!({
    "success": false,
    "message": format!("El correo {} ya está en uso por otro usuario", email)
})));
}

// Update user
conn.exec_drop(
"UPDATE users SET name = ?, email = ? WHERE id = ?",
(&user_data.name, &user_data.email, user_exists.unwrap())
).map_err(|e| ApiError::DbError(e))?;

Ok(HttpResponse::Ok().json(serde_json::json!({
"success": true,
"message": "Usuario actualizado exitosamente"
})))
}

async fn get_task_assignments(pool: web::Data<Pool>) -> Result<HttpResponse, Error> {
    let mut conn = pool.get_conn()
        .map_err(|e| ApiError::DbError(e))?;

    let assignments: Vec<TaskWithAssignments> = conn
        .query_map(
            "SELECT t.id, t.title, COALESCE(t.description, '') as description, 
                    t.status_id, ts.name as status, 
                    DATE_FORMAT(t.date_from, '%Y-%m-%d') as date_from, 
                    DATE_FORMAT(t.due_date, '%Y-%m-%d') as due_date,
                    GROUP_CONCAT(DISTINCT u.id) as user_ids,
                    GROUP_CONCAT(DISTINCT u.name) as user_names,
                    GROUP_CONCAT(DISTINCT u.email) as user_emails
            FROM tasks t
            INNER JOIN task_assignments ta ON t.id = ta.task_id
            INNER JOIN users u ON ta.user_id = u.id
            LEFT JOIN task_statuses ts ON t.status_id = ts.id
            GROUP BY t.id",
            |(id, title, description, status_id, status, date_from, due_date, user_ids, user_names, user_emails)| {
                let assigned_users = if let (Some(ids), Some(names), Some(emails)) = (user_ids, user_names, user_emails) {
                    let ids: String = ids;
                    let names: String = names;
                    let emails: String = emails;
                    
                    let ids: Vec<&str> = ids.split(',').collect();
                    let names: Vec<&str> = names.split(',').collect();
                    let emails: Vec<&str> = emails.split(',').collect();
                    
                    ids.iter()
                        .zip(names.iter())
                        .zip(emails.iter())
                        .map(|((id, name), email)| AssignedUser {
                            id: id.parse().unwrap_or(0),
                            name: name.to_string(),
                            email: email.to_string(),
                        })
                        .collect()
                } else {
                    Vec::new()
                };

                TaskWithAssignments {
                    id,
                    title,
                    description,
                    status_id,
                    status,
                    date_from,
                    due_date,
                    assigned_users,
                }
            },
        )
        .map_err(|e| ApiError::DbError(e))?;

    Ok(HttpResponse::Ok().json(serde_json::json!({
        "success": true,
        "assignments": assignments
    })))
}

async fn assign_users_to_task(
    pool: web::Data<Pool>,
    assignment: web::Json<TaskAssignmentRequest>,
) -> Result<HttpResponse, Error> {
    let mut conn = pool.get_conn()
        .map_err(|e| ApiError::DbError(e))?;
    
    let mut tx = conn.start_transaction(TxOpts::default())
        .map_err(|e| ApiError::DbError(e))?;

    // Verify task exists
    let task_exists: Option<i32> = tx
        .exec_first(
            "SELECT 1 FROM tasks WHERE id = ?",
            (assignment.task_id,)
        )
        .map_err(|e| ApiError::DbError(e))?;

    if task_exists.is_none() {
        return Ok(HttpResponse::NotFound().json(serde_json::json!({
            "success": false,
            "message": "Task not found"
        })));
    }

    // Verify all users exist
    for user_id in &assignment.user_ids {
        let user_exists: Option<i32> = tx
            .exec_first(
                "SELECT 1 FROM users WHERE id = ?",
                (user_id,)
            )
            .map_err(|e| ApiError::DbError(e))?;

        if user_exists.is_none() {
            return Ok(HttpResponse::NotFound().json(serde_json::json!({
                "success": false,
                "message": format!("User with id {} not found", user_id)
            })));
        }
    }
    
    if !assignment.user_ids.is_empty() {
        // Get current assignments
        let current_assignments: Vec<i32> = tx
            .exec_map(
                "SELECT user_id FROM task_assignments WHERE task_id = ?",
                (assignment.task_id,),
                |user_id: i32| user_id
            )
            .map_err(|e| ApiError::DbError(e))?;

        // Remove assignments for users not in the new list
        for current_user_id in current_assignments {
            if !assignment.user_ids.contains(&current_user_id) {
                tx.exec_drop(
                    "DELETE FROM task_assignments WHERE task_id = ? AND user_id = ?",
                    (assignment.task_id, current_user_id)
                ).map_err(|e| ApiError::DbError(e))?;
            }
        }

        // Insert new assignments
        for user_id in &assignment.user_ids {
            tx.exec_drop(
                "INSERT IGNORE INTO task_assignments (task_id, user_id) VALUES (?, ?)",
                (assignment.task_id, user_id)
            ).map_err(|e| ApiError::DbError(e))?;
        }
    } else {
        // If no users provided, remove all assignments
        tx.exec_drop(
            "DELETE FROM task_assignments WHERE task_id = ?",
            (assignment.task_id,)
        ).map_err(|e| ApiError::DbError(e))?;
    }

    tx.commit()
        .map_err(|e| ApiError::DbError(e))?;

    Ok(HttpResponse::Ok().json(serde_json::json!({
        "success": true,
        "message": "Task assignments updated successfully"
    })))
}


async fn get_task_statuses(pool: web::Data<Pool>) -> Result<HttpResponse, Error> {
    let mut conn = pool.get_conn()
        .map_err(|e| ApiError::DbError(e))?;
    
    let statuses: Vec<TaskStatus> = conn
        .query_map(
            "SELECT id, name FROM task_statuses ORDER BY id",
            |(id, name)| TaskStatus { id, name }
        )
        .map_err(|e| ApiError::DbError(e))?;
    
    Ok(HttpResponse::Ok().json(serde_json::json!({
        "success": true,
        "statuses": statuses
    })))
}

async fn list_users(pool: web::Data<Pool>) -> Result<HttpResponse, Error> {
    let mut conn = pool.get_conn()
        .map_err(|e| ApiError::DbError(e))?;
    
    let users: Vec<UserList> = conn
        .query_map(
            "SELECT id, name, email FROM users ORDER BY id",
            |(id, name, email)| UserList { id, name, email }
        )
        .map_err(|e| ApiError::DbError(e))?;
    
    Ok(HttpResponse::Ok().json(serde_json::json!({
        "success": true,
        "users": users
    })))
}

async fn upload_images(mut payload: Multipart) -> Result<HttpResponse, Error> {
    let mut paths = Vec::new();
    let uploads_dir = Path::new("./uploads");

    // Create uploads directory if it doesn't exist
    if !uploads_dir.exists() {
        std::fs::create_dir_all(uploads_dir)
            .map_err(|e| ApiError::FileError(format!("Failed to create uploads directory: {}", e)))?;
    }

    // Process each field in the multipart form data
    while let Some(mut field) = payload.try_next().await.map_err(|e| ApiError::FileError(e.to_string()))? {
        let content_type = field.content_disposition();
        
        let filename = content_type
            .get_filename()
            .ok_or_else(|| ApiError::FileError("No filename provided".to_string()))?;

        // Generate unique filename
        let uuid = Uuid::new_v4();
        let extension = Path::new(filename)
            .extension()
            .and_then(|ext| ext.to_str())
            .unwrap_or("bin");
        let new_filename = format!("{}.{}", uuid, extension);
        let filepath = uploads_dir.join(&new_filename);

        // Create file
        let mut file = std::fs::File::create(&filepath)
            .map_err(|e| ApiError::FileError(format!("Failed to create file: {}", e)))?;

        // Write file contents
        while let Some(chunk) = field.try_next().await.map_err(|e| ApiError::FileError(e.to_string()))? {
            file.write_all(&chunk)
                .map_err(|e| ApiError::FileError(format!("Failed to write file: {}", e)))?;
        }

        paths.push(format!("/uploads/{}", new_filename));
    }

    if paths.is_empty() {
        Ok(HttpResponse::BadRequest().json(serde_json::json!({
            "success": false,
            "message": "No files were uploaded"
        })))
    } else {
        Ok(HttpResponse::Ok().json(serde_json::json!({
            "success": true,
            "message": "Files uploaded successfully",
            "paths": paths
        })))
    }
}

fn get_connection_string() -> String {
    let db_user = env::var("DB_USER").expect("DB_USER must be set");
    let db_password = env::var("DB_PASSWORD").expect("DB_PASSWORD must be set");
    let db_host = env::var("DB_HOST").expect("DB_HOST must be set");
    let db_port = env::var("DB_PORT").expect("DB_PORT must be set");
    let db_name = env::var("DB_NAME").expect("DB_NAME must be set");
    
    format!(
        "mysql://{}:{}@{}:{}/{}",
        db_user, db_password, db_host, db_port, db_name
    )
}

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    // Initialize logging
    env_logger::init_from_env(env_logger::Env::new().default_filter_or("info"));
    
    // Get bind address from environment variable with fallback
    let bind_address = env::var("BIND_ADDRESS").unwrap_or_else(|_| "127.0.0.1:8080".to_string());
    
    // Log startup information (keep localhost in logs as it's just informational)
    log::info!("Starting HTTP server at http://localhost:8080");
    log::info!("Available endpoints:");
    log::info!("  POST /api/register - Register new user");
    log::info!("  POST /api/login - User login");
    log::info!("  GET /api/taskAssignments - Get all task assignments");
    log::info!("  POST /api/taskAssignments/assign - Assign users to task");
    log::info!("  PUT /api/taskAssignments/remove - Remove users from task");
    log::info!("  PUT /api/user/{{id}} - Update user");
    log::info!("  GET /api/tasks - List all tasks");
    log::info!("  POST /api/tasks - Create new task");
    log::info!("  GET /api/tasks/{{id}} - Get task details");
    log::info!("  PUT /api/tasks/{{id}} - Update task");
    log::info!("  DELETE /api/tasks/{{id}} - Delete task");
    log::info!("  GET /api/taskStatuses - Get all task statuses");

    HttpServer::new(move || {
        // Create MySQL pool
        let pool = mysql::Pool::new(get_connection_string().as_str()).unwrap();

        App::new()
            .app_data(web::Data::new(pool))
            .wrap(Logger::default())
            .service(web::resource("/").to(|| async {
                HttpResponse::Ok().json(serde_json::json!({
                    "name": "Task Management API",
                    "version": "1.0",
                    "endpoints": {
                        "auth": {
                            "register": "POST /api/register",
                            "login": "POST /api/login"
                        },
                        "users": {
                            "update": "PUT /api/user/{id}"
                        },
                        "tasks": {
                            "list": "GET /api/tasks",
                            "get": "GET /api/tasks/{id}",
                            "create": "POST /api/tasks",
                            "update": "PUT /api/tasks/{id}",
                            "delete": "DELETE /api/tasks/{id}"
                        },
                        "taskStatuses": {
                            "list": "GET /api/taskStatuses"
                        },
                        "taskAssignments": {
                            "list": "GET /api/taskAssignments",
                            "assign": "POST /api/taskAssignments/assign",
                            "remove": "PUT /api/taskAssignments/remove"
                        }
                    }
                }))
            }))
            .service(
                web::scope("/api")
                    .service(web::resource("/register").route(web::post().to(register)))
                    .service(web::resource("/login").route(web::post().to(login)))
                    .service(web::resource("/user/taskAssignments").route(web::get().to(get_user_task_assignments)))
                    .service(web::resource("/user/{id}").route(web::put().to(update_user)))
                    .service(
                        web::scope("/taskAssignments")
                            .route("", web::get().to(get_task_assignments))
                            .route("/assign", web::post().to(assign_users_to_task))
                    )
                    .service(
                        web::scope("/tasks")
                            .route("", web::post().to(create_task))
                            .route("", web::get().to(list_tasks))
                            .route("/{id}", web::get().to(get_task))
                            .route("/{id}", web::put().to(update_task))
                            .route("/{id}", web::delete().to(delete_task))
                    )
                    .service(web::resource("/taskStatuses").route(web::get().to(get_task_statuses)))
                    .service(web::resource("/users").route(web::get().to(list_users)))
                    .service(web::resource("/images").route(web::post().to(upload_images)))
            )
    })
    .bind(bind_address)?
    .run()
    .await
    }
