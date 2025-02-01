# Task Assignment API

A RESTful API built with Actix Web for managing task assignments and notifications.

## Prerequisites

- Docker
- Docker Compose

## Setup and Running

1. Clone the repository:
```bash
git clone <repository-url>
cd actix_web_api
```

2. Configure environment variables:
- Copy `.env.example` to `.env`
- Update the values in `.env` with your configurations

3. Build and run with Docker Compose:
```bash
docker-compose up --build
```

The API will be available at `http://localhost:8081`

## API Endpoints

### Assign Tasks
- **POST** `/api/taskAssignments/assign`
```json
{
    "task_id": 1,
    "user_ids": [1, 2, 3]
}
```

## Database Schema

The application uses MySQL with the following tables:
- tasks
- users
- devices
- task_assignments

## Development

To run locally without Docker:

1. Install Rust and MySQL
2. Update `.env` with local database credentials
3. Run:
```bash
cargo run
```

## Testing

Run the tests with:
```bash
cargo test
```

