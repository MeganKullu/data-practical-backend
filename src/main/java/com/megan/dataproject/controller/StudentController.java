package com.megan.dataproject.controller;

import com.megan.dataproject.model.Student;
import com.megan.dataproject.model.StudentClass;
import com.megan.dataproject.payload.ApiResponse;
import com.megan.dataproject.payload.ExportResponse;
import com.megan.dataproject.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StudentController {

    private final ExcelGeneratorService excelGeneratorService;
    private final ExcelToCsvService excelToCsvService;
    private final CsvToDatabaseService csvToDatabaseService;
    private final JobService jobService;
    private final ReportService reportService;


    // 1. POLLING ENDPOINT (Frontend calls this to check job status)
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
    // D) REPORT ENDPOINTS

    // D0) Get total count of students
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getCount() {
        Page<Student> page = reportService.getStudents(null, null, PageRequest.of(0, 1));
        return ResponseEntity.ok(ApiResponse.success("Count retrieved",
            Map.of("total", page.getTotalElements())));
    }

    // D1) Get paginated students with search and filter
    @GetMapping("/report")
    public ResponseEntity<ApiResponse<Page<Student>>> getReport(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) StudentClass studentClass,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "studentId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Student> students = reportService.getStudents(studentId, studentClass, pageable);

        return ResponseEntity.ok(ApiResponse.success("Report data retrieved", students));
    }

    // D2) Export to CSV
    @GetMapping("/report/export/csv")
    public ResponseEntity<ApiResponse<ExportResponse>> exportToCsv(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) StudentClass studentClass) throws IOException {

        ExportResponse export = reportService.exportToCsv(studentId, studentClass);
        return ResponseEntity.ok(ApiResponse.success("CSV export generated", export));
    }

    // D3) Export to Excel
    @GetMapping("/report/export/excel")
    public ResponseEntity<ApiResponse<ExportResponse>> exportToExcel(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) StudentClass studentClass) throws IOException {

        ExportResponse export = reportService.exportToExcel(studentId, studentClass);
        return ResponseEntity.ok(ApiResponse.success("Excel export generated", export));
    }

    // D4) Export to PDF
    @GetMapping("/report/export/pdf")
    public ResponseEntity<ApiResponse<ExportResponse>> exportToPdf(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) StudentClass studentClass) {

        ExportResponse export = reportService.exportToPdf(studentId, studentClass);
        return ResponseEntity.ok(ApiResponse.success("PDF export generated", export));
    }
}
