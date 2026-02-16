package com.megan.dataproject.controller;

import com.megan.dataproject.payload.ApiResponse;
import com.megan.dataproject.service.CsvToDatabaseService;
import com.megan.dataproject.service.ExcelGeneratorService;
import com.megan.dataproject.service.ExcelToCsvService;
import com.megan.dataproject.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class StudentController {

    private final ExcelGeneratorService excelGeneratorService;
    private final ExcelToCsvService excelToCsvService;
    private final CsvToDatabaseService csvToDatabaseService;
    private final JobService jobService;

    // ==========================================
    // 1. POLLING ENDPOINT (Frontend calls this to check job status)
    // ==========================================
    @GetMapping("/status/{jobId}")
    public ResponseEntity<ApiResponse<JobService.JobInfo>> getJobStatus(@PathVariable String jobId) {
        JobService.JobInfo jobInfo = jobService.getJob(jobId);
        if (jobInfo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success("Job status retrieved", jobInfo));
    }

    // A) Generate Excel (Async)
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateExcel(@RequestParam int count) throws IOException {
        String jobId = jobService.createJob();
        excelGeneratorService.generateStudentsExcel(jobId, count);
        return ResponseEntity.ok(ApiResponse.success("Excel generation started", Map.of("jobId", jobId)));
    }


    // B) Process Excel to CSV (Async)
    @PostMapping("/process")
    public ResponseEntity<ApiResponse<Map<String, String>>> processToCsv(@RequestParam("file") MultipartFile file) {
        try {
            File tempFile = File.createTempFile("upload_raw_", ".xlsx");
            file.transferTo(tempFile);

            String jobId = jobService.createJob();
            excelToCsvService.convertExceltoCsv(tempFile, jobId);
            return ResponseEntity.ok(ApiResponse.success("CSV processing started", Map.of("jobId", jobId)));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("File upload failed: " + e.getMessage()));
        }
    }

    // C) Upload CSV to DB (Async)
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadToDb(@RequestParam("file") MultipartFile file) {
        try {
            File tempFile = File.createTempFile("upload_csv_", ".csv");
            file.transferTo(tempFile);

            String jobId = jobService.createJob();
            csvToDatabaseService.uploadCsvToDatabase(jobId, tempFile.getAbsolutePath());
            return ResponseEntity.ok(ApiResponse.success("Database upload started", Map.of("jobId", jobId)));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("File upload failed: " + e.getMessage()));
        }
    }
}
