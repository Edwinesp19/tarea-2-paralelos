# Task Assignment Notification API

This Rust-based API service manages task assignments and sends notifications to users when they are assigned to tasks. Built with Actix-web and integrates with OneSignal for notifications.

## Prerequisites

- Rust (latest stable version)
- MySQL (XAMPP)
- OneSignal account and API credentials

## Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd actix-web-api
```

2. Create a `.env` file in the project root with the following content:
```env
DATABASE_URL=mysql://root:@localhost:3306/rest_db
ONESIGNAL_APP_ID=your_app_id
ONESIGNAL_API_KEY=your_api_key
```

3. Build and run the project:
```bash
cargo build
cargo run
```

## API Documentation

### Assign Users to Task
Assigns one or more users to a specific task and sends notifications to the assigned users.

**Endpoint**: `POST /api/assign-task`

**Request Body**:
```json
{
"task_id": 1,
"user_ids": [1, 2, 3]
}
```

**Success Response**:
```json
{
"success": true,
"message": "Users successfully assigned to task",
"data": {
    "task_id": 1,
    "assigned_users": [1, 2, 3]
}
}
```

**Error Responses**:
- 404: Task not found
- 400: Invalid request data
- 409: Duplicate assignment
- 500: Internal server error

## Database Setup

The API uses the existing `rest_db` database. Make sure the following tables exist:

- `tasks`: Stores task information
- `users`: Stores user information
- `task_assignments`: Links tasks to users

## Environment Variables

- `DATABASE_URL`: MySQL connection string
- `ONESIGNAL_APP_ID`: Your OneSignal App ID
- `ONESIGNAL_API_KEY`: Your OneSignal REST API Key

## Error Handling

The API implements comprehensive error handling for:
- Database connection issues
- Invalid task or user IDs
- Duplicate assignments
- OneSignal notification failures

## Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

