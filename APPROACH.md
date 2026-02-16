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
- **Database inserts**: Batching 5000 records at a time instead of one-by-one

---

## Why Async?

Processing 1M records takes a while. We can't block the HTTP request for 5 minutes - the connection would timeout and the user would think it's broken.

Instead:
1. User clicks "Generate" → backend immediately returns a `jobId`
2. Heavy work happens in a background thread
3. Frontend polls `/status/{jobId}` every 2 seconds to check progress
4. When done, status shows `COMPLETED` with the file path

We're using Spring's `@Async` annotation. Tried `CompletableFuture` first but `@Async` is cleaner - just annotate the method and Spring handles the threading.

Thread pool config (in `AsyncConfig.java`):
- 4 core threads (always running)
- Up to 8 threads when busy
- Queue of 100 waiting tasks

---

## The Score Math

Per the requirements, scores get bumped at each stage:

```
Excel (original)  →  CSV (+10)  →  Database (+5)
     65.0              75.0            80.0
```

Total increase: +15 from Excel to database.

---

## Database

Added indexes on columns we'll filter/search on:
- `class` - for the dropdown filter
- `score` - might be useful
- composite `(class, score)` - for combined queries

Batch size is 5000 for inserts. Tried 1000 first but 5000 was noticeably faster. Going higher didn't help much and used more memory.

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
- `POST /api/students/process` - Excel → CSV (multipart file upload)
- `POST /api/students/upload` - CSV → database (multipart file upload)
- `GET /api/students/status/{jobId}` - check progress

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
    status;         // SUBMITTED → PROCESSING → COMPLETED (or FAILED)
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
│   └── AsyncConfig.java           # thread pool setup
├── controller/
│   └── StudentController.java     # all the endpoints
├── model/
│   ├── Student.java               # JPA entity
│   ├── StudentClass.java          # enum: Class1, Class2, etc.
│   └── JobStatus.java             # enum: SUBMITTED, PROCESSING, etc.
├── payload/
│   ├── ApiResponse.java           # wrapper for all responses
│   └── ExportResponse.java        # for file exports
├── repository/
│   ├── StudentRepository.java     # JPA repo
│   └── StudentSpecification.java  # dynamic query builder
└── service/
    ├── JobService.java            # tracks async jobs
    ├── FileStorageService.java    # handles file paths
    ├── ExcelGeneratorService.java # creates Excel files
    ├── ExcelToCsvService.java     # Excel → CSV conversion
    ├── CsvToDatabaseService.java  # CSV → PostgreSQL
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

```bash
# start with local profile (uses application-local.yaml)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# or build and run jar
./mvnw clean package -DskipTests
java -jar target/dataproject-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

Make sure PostgreSQL is running and the connection string in `application-local.yaml` is correct.
