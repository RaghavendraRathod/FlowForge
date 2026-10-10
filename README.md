# FlowForge

**A Spring Boot-based background job processing system with asynchronous workers, task handlers, retry handling, and PostgreSQL persistence.**

FlowForge is a backend project designed to demonstrate how applications can create jobs through REST APIs, process them asynchronously using background workers, and persist their execution status and results.

## Live Deployment

- **Base URL:** https://flowforge-wocn.onrender.com
- **Health Check:** https://flowforge-wocn.onrender.com/actuator/health
- **List Workflows:** https://flowforge-wocn.onrender.com/api/workflows
- **List Jobs:** https://flowforge-wocn.onrender.com/api/jobs

> Note: The application runs on Render's free instance. The service may take some time to respond after periods of inactivity.

## Features

- REST APIs for creating and retrieving workflows.
- REST APIs for creating and retrieving jobs.
- Asynchronous background job processing.
- Worker-based job claiming and execution.
- Extensible task-handler architecture.
- DEMO task handler for successful task execution.
- ECHO task handler for returning the submitted payload.
- Job status tracking and result persistence.
- Retry handling for failed jobs.
- PostgreSQL database integration.
- Health monitoring through Spring Boot Actuator.
- Docker-based deployment.
- Automated tests using the Maven test lifecycle.

## Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Programming language |
| Spring Boot | Backend application framework |
| Spring Data JPA | Database persistence |
| PostgreSQL | Relational database |
| Maven | Build and dependency management |
| Docker | Containerized deployment |
| Spring Boot Actuator | Application health monitoring |
| JUnit | Automated testing |
| Render | Application hosting |
| Neon | Managed PostgreSQL hosting |

## Architecture

```text
Client
  |
  v
REST API
  |
  v
PostgreSQL Database
  |
  v
Background Worker
  |
  v
Task Executor
  |
  +-------------------+
  |                   |
  v                   v
DEMO Handler      ECHO Handler
  |                   |
  +---------+---------+
            |
            v
     Persist Job Result

```

## API Endpoints

### Workflows

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/workflows` | Create a workflow |
| GET | `/api/workflows` | Retrieve all workflows |
| GET | `/api/workflows/{id}` | Retrieve a workflow by ID |

### Jobs

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/jobs` | Create a job |
| GET | `/api/jobs` | Retrieve all jobs |
| GET | `/api/jobs/{id}` | Retrieve a job by ID |

### Health

| Method | Endpoint | Description |
|---|---|---|
| GET | `/actuator/health` | Check application health |

## Supported Task Types

### DEMO

Executes a demonstration task and returns a success message containing the submitted payload.

Example result:

```text
Executed successfully: Hello from FlowForge
```

### ECHO

Returns the submitted payload as the task result.

Example:

```text
Payload: FlowForge echo test
Result:  FlowForge echo test
```

## Example: Create a Workflow

Send a `POST` request to:

`https://flowforge-wocn.onrender.com/api/workflows`

Request body:

```json
{
  "name": "Demo Workflow",
  "description": "Testing the FlowForge API"
}
```

The API returns the created workflow, including its generated ID and creation timestamp.

## Example: Create a Job

Send a `POST` request to:

`https://flowforge-wocn.onrender.com/api/jobs`

Request body:

```json
{
  "workflowId": "YOUR_WORKFLOW_UUID",
  "taskType": "DEMO",
  "payload": "Hello from FlowForge"
}
```

Replace `YOUR_WORKFLOW_UUID` with the ID of an existing workflow.

Jobs are processed asynchronously. The initial response may show `QUEUED` or `RUNNING`. Query the jobs endpoint again to inspect the final status and result.

## Local Setup

### Prerequisites

- Java 21
- Git
- PostgreSQL
- A terminal such as PowerShell

### 1. Clone the repository

```bash
git clone https://github.com/RaghavendraRathod/FlowForge.git
cd FlowForge
```

### 2. Configure the database

Create a PostgreSQL database for FlowForge.

Configure these environment variables before starting the application:

| Variable | Purpose |
|---|---|
| `DB_URL` | JDBC connection URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `DDL_AUTO` | Hibernate schema-management setting |
| `SHOW_SQL` | Whether SQL statements are logged |

For a local PostgreSQL installation, the JDBC URL generally follows this format:

```text
jdbc:postgresql://localhost:5432/flowforge
```

Set `DB_PASSWORD` to your local database password. Never commit credentials or production secrets to GitHub.

### 3. Run the application

On Windows PowerShell:

```powershell
.\mvnw spring-boot:run
```

The application uses port `8080` by default unless configured otherwise.

### 4. Check application health

Open:

`http://localhost:8080/actuator/health`

## Running Tests

Run the test suite using the Maven wrapper:

```powershell
.\mvnw clean test
```

The project has successfully completed a test run with:

- **37 tests**
- **0 failures**
- **0 errors**
- **0 skipped**

## Deployment

FlowForge is deployed using Docker on Render.

The application uses Neon PostgreSQL for managed database hosting.

Production configuration is supplied through environment variables rather than hardcoded credentials.

## Future Improvements

Potential future enhancements include:

- Interactive API documentation.
- A web dashboard for monitoring jobs and workflows.
- Additional task-handler implementations.
- Improved retry policies and observability.
- Workflow execution history and step-level tracking.
- Authentication and authorization.

## Author

**Raghavendra Rathod**

- GitHub: https://github.com/RaghavendraRathod

---

FlowForge is a learning and portfolio project focused on backend engineering, asynchronous processing, database persistence, and deployment.