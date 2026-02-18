# Data Processing Application

## What This Does
1. Generates fake student data into an Excel file
2. Reads that Excel and converts it to CSV (adding 10 to each score)
3. Uploads the CSV to PostgreSQL (adding another 5 to each score)
4. Provides a report page with search, filter, and export

---

## The Main Challenge

Loading 1M rows into memory is not advised. Java will throw OutOfMemoryError.

Therefore, we stream everything:

- **Writing Excel**: Using `SXSSFWorkbook` which only keeps 100 rows in memory at a time, flushing older rows to disk
- **Reading Excel**: Using `excel-streaming-reader` (SAX parser under the hood) - reads row by row, never loads the whole file
- **Database inserts**: Batching 10,000 records at a time instead of one-by-one

---

## Why Async?

Processing 1M records takes a while. We can't block the HTTP request for 5 minutes - the connection would timeout and the user would think it's broken.

Instead:
1. User clicks "Generate" -> backend immediately returns a `jobId`
2. Heavy work happens in a background thread
3. Frontend polls `/status/{jobId}` every 2 seconds to check progress
4. When done, status shows `COMPLETED` with the file path

We're using Spring's `@Async` annotation. 

Thread pool config (in `AsyncConfig.java`):
- 4 core threads (always running)
- Up to 8 threads when busy
- Queue of 100 waiting tasks

---

## The Score Math

Per the requirements, scores get bumped at each stage:

```
Excel (original)  ->  CSV (+10)  ->  Database (+5)
      65                 75              80
```

Total increase: +15 from Excel to database. Scores are integers (55-75 initially).

---

## Database

Using Neon (serverless PostgreSQL).

Added indexes on columns we'll filter/search on:
- `class` - for the dropdown filter
- `score` - might be useful
- composite `(class, score)` - for combined queries

Batch size is 10,000 for inserts. Tried smaller batches first but 10,000 was noticeably faster without using too much memory.

---

## Environment Setup

Set the `DATABASE_URL` environment variable with your Neon connection string:

```bash
export DATABASE_URL=jdbc:postgresql://ep-xxx.us-east-2.aws.neon.tech/dataproject?sslmode=require&user=your_user&password=your_password
```

---

## API Endpoints

All responses follow the same structure:
```json
{
  "success": true,
  "message": "Something happened",
  "data": { ... }
}
```

### Async operations (return jobId immediately):
- `POST /api/students/generate?count=1000000` - make Excel
- `POST /api/students/process` - Excel -> CSV (multipart file upload)
- `POST /api/students/upload` - CSV -> database (multipart file upload)
- `GET /api/students/status/{jobId}` - check progress
- `GET /api/students/download/{jobId}` - download generated file

### Reports:
- `GET /api/students/report?page=0&size=20&studentClass=Class1` - paginated list
- `GET /api/students/report/export/csv` - download CSV
- `GET /api/students/report/export/excel` - download Excel
- `GET /api/students/report/export/pdf` - download PDF

Export endpoints return base64-encoded file data. Frontend decodes and triggers download. Did it this way to keep response structure consistent.

---

## Job Tracking

The `JobService` keeps track of running jobs in a `ConcurrentHashMap`:

```java
JobInfo {
    status;         // SUBMITTED -> PROCESSING -> COMPLETED (or FAILED)
    result;         // file path when done, error message if failed
    progress;       // 0-100
    processedCount; // how many rows done
    totalCount;     // total rows (if known)
}
```

Services update progress every 10,000 records. More frequent updates would slow things down; less frequent and the progress bar looks stuck.

---

## Project Structure

```
src/main/java/com/megan/dataproject/
├── config/
│   ├── AsyncConfig.java           # thread pool setup
│   └── RequestLoggingFilter.java  # request/response logging
├── controller/
│   └── StudentController.java     # all the endpoints
├── model/
│   ├── Student.java               # JPA entity
│   ├── StudentClass.java          # enum: Class1, Class2, etc.
│   └── JobStatus.java             # enum: SUBMITTED, PROCESSING, etc.
├── payload/
│   ├── ApiResponse.java           # wrapper for all responses
│   ├── ExportResponse.java        # for file exports
│   └── PageResponse.java          # pagination wrapper
├── repository/
│   ├── StudentRepository.java     # JPA repo
│   └── StudentSpecification.java  # dynamic query builder
└── service/
    ├── JobService.java            # tracks async jobs
    ├── FileStorageService.java    # handles file paths
    ├── ExcelGeneratorService.java # creates Excel files
    ├── ExcelToCsvService.java     # Excel -> CSV conversion
    ├── CsvToDatabaseService.java  # CSV -> PostgreSQL
    └── ReportService.java         # queries + exports
```

---

## Testing

Unit tests mock dependencies and test logic in isolation. Integration tests use H2 in-memory database and run the full pipeline.

```bash
# all tests
./mvnw test

# just unit tests
./mvnw test -Dtest="*ServiceTest"

# just integration tests
./mvnw test -Dtest="*IntegrationTest"
```

Integration tests are ordered - they generate Excel, convert to CSV, upload to DB, then test the reports. Each test assumes the previous one passed.

---

## Running Locally

### Option 1: Docker Compose (Recommended)

The easiest way to run locally

```bash
# Clone the repo
git clone https://github.com/megankullu/dataproject.git
cd dataproject

# Start everything (PostgreSQL + Backend)
docker-compose up -d

# Check logs
docker-compose logs -f backend

# Stop when done
docker-compose down
```

### Option 2: With Your Own Database  

```bash
# Set your database URL
export DATABASE_URL=jdbc:postgresql://ep-xxx.neon.tech/dataproject?sslmode=require&user=xxx&password=xxx

# Run the application
./mvnw spring-boot:run

# Or build and run jar
./mvnw clean package -DskipTests
java -jar target/dataproject-0.0.1-SNAPSHOT.jar
```
