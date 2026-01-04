# Batch Data Privacy Sanitizer

A production-grade Spring Boot + Spring Batch application for sanitizing PII from large CSV datasets.

## Features

- **Streaming Batch Processing**: Memory-efficient processing of files with millions of rows
- **Web UI**: Beautiful dark-themed interface with CSV preview, file upload, and job monitoring
- **CSV Preview**: View data before sanitization with live preview
- **Diff Viewer**: Compare before/after to see exactly what changed
- **Configurable Sanitization Rules**: Apply different operations per column
- **Four Sanitization Operations**:
  - `MASK` - Partially hide values while preserving format
  - `HASH` - One-way SHA-256 hashing (deterministic)
  - `NULLIFY` - Complete data removal
  - `RANDOMIZE` - Replace with realistic fake data
- **Storage Abstraction**: Local storage (default) with S3 support ready
- **Restartability**: Resume failed jobs from last checkpoint
- **Comprehensive Audit Logging**: Track all job executions for compliance

## Quick Start

### Prerequisites

- Java 24+
- Maven 3.8+

### Build

```bash
mvn clean package
```

### Run

```bash
java -jar target/batch-data-sanitizer-1.0.0.jar
```

The application starts on `http://localhost:8080`

## Docker Deployment

### Using Docker Compose (Recommended)

```bash
docker-compose up -d
```

### Using Docker Directly

```bash
# Build the image
docker build -t data-sanitizer .

# Run the container
docker run -d -p 8080:8080 \
  -v $(pwd)/data/input:/app/data/input \
  -v $(pwd)/data/output:/app/data/output \
  --name data-sanitizer \
  data-sanitizer
```

### Docker Commands

```bash
# View logs
docker-compose logs -f

# Stop the container
docker-compose down

# Rebuild after code changes
docker-compose up -d --build
```

## Web Interface

- **Home** (`/index.html`) - Upload CSV files and configure sanitization
- **Dashboard** (`/dashboard.html`) - View job statistics and recent jobs
- **History** (`/history.html`) - Browse complete job history

## API Usage

### Start a Sanitization Job

```bash
curl -X POST http://localhost:8080/api/v1/sanitize \
  -F "file=@samples/sample_data.csv" \
  -F 'config={"columns":{"email":"HASH","phone":"MASK","name":"RANDOMIZE","ssn":"NULLIFY"}}'
```

### Check Job Status

```bash
curl http://localhost:8080/api/v1/jobs/{jobExecutionId}
```

### View Recent Audits

```bash
curl http://localhost:8080/api/v1/audits
```

### Get Statistics

```bash
curl http://localhost:8080/api/v1/audits/stats
```

## Configuration

### Application Properties

| Property | Description | Default |
|----------|-------------|---------|
| `sanitizer.batch.chunk-size` | Records per chunk | 1000 |
| `sanitizer.batch.skip-limit` | Max skippable errors | 100 |
| `sanitizer.storage.input-dir` | Input file directory | ./data/input |
| `sanitizer.storage.output-dir` | Output file directory | ./data/output |

## Project Structure

```
batch-data-sanitizer/
├── src/main/java/com/sourav/enterprise/sanitizer/
│   ├── batch/           # Spring Batch components
│   ├── controller/      # REST endpoints
│   ├── domain/          # Entities and models
│   ├── dto/             # Request/Response DTOs
│   ├── exception/       # Custom exceptions
│   ├── repository/      # JPA repositories
│   ├── service/         # Business logic
│   └── strategy/        # Sanitization strategies
├── src/main/resources/
│   ├── static/          # Frontend (HTML, CSS, JS)
│   └── application.yml  # Configuration
├── samples/             # Sample data
└── pom.xml
```

## Database Console

Access H2 console at: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./data/sanitizer-db`
- Username: `sa`
- Password: (empty)

## License

Enterprise Internal Use Only - Sourav Enterprise

## Version

1.0.0
