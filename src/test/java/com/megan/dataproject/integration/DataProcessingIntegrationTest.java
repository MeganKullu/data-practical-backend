package com.megan.dataproject.integration;

import com.megan.dataproject.model.JobStatus;
import com.megan.dataproject.model.Student;
import com.megan.dataproject.model.StudentClass;
import com.megan.dataproject.payload.ExportResponse;
import com.megan.dataproject.repository.StudentRepository;
import com.megan.dataproject.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DataProcessingIntegrationTest {

    @Autowired
    private ExcelGeneratorService excelGeneratorService;

    @Autowired
    private ExcelToCsvService excelToCsvService;

    @Autowired
    private CsvToDatabaseService csvToDatabaseService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private JobService jobService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private StudentRepository studentRepository;

    private static String generatedExcelPath;
    private static String generatedCsvPath;

    @BeforeEach
    void setUp() {
        // Clean up database before each test
    }

    @Test
    @Order(1)
    @DisplayName("Integration: Should generate Excel file with 100 records")
    void shouldGenerateExcelFile() throws Exception {
        // Given
        String jobId = jobService.createJob();
        int recordCount = 100;

        // When
        excelGeneratorService.generateStudentsExcel(jobId, recordCount);

        // Then - wait for async completion
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            JobService.JobInfo jobInfo = jobService.getJob(jobId);
            assertThat(jobInfo.getStatus()).isEqualTo(JobStatus.COMPLETED);
        });

        JobService.JobInfo jobInfo = jobService.getJob(jobId);
        generatedExcelPath = jobInfo.getResult();

        assertThat(generatedExcelPath).isNotNull();
        assertThat(Files.exists(Path.of(generatedExcelPath))).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("Integration: Should convert Excel to CSV with score +10")
    void shouldConvertExcelToCsv() throws Exception {
        // Skip if Excel wasn't generated
        Assumptions.assumeTrue(generatedExcelPath != null, "Excel file must be generated first");

        // Given
        String jobId = jobService.createJob();
        File excelFile = new File(generatedExcelPath);

        // When
        excelToCsvService.convertExceltoCsv(excelFile, jobId);

        // Then - wait for async completion
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            JobService.JobInfo jobInfo = jobService.getJob(jobId);
            assertThat(jobInfo.getStatus()).isEqualTo(JobStatus.COMPLETED);
        });

        JobService.JobInfo jobInfo = jobService.getJob(jobId);
        generatedCsvPath = jobInfo.getResult();

        assertThat(generatedCsvPath).isNotNull();
        assertThat(Files.exists(Path.of(generatedCsvPath))).isTrue();

        // Verify CSV has correct line count
        long lineCount = Files.lines(Path.of(generatedCsvPath)).count();
        assertThat(lineCount).isEqualTo(101); // header + 100 data rows
    }

    @Test
    @Order(3)
    @DisplayName("Integration: Should upload CSV to database with score +5")
    void shouldUploadCsvToDatabase() throws Exception {
        // Skip if CSV wasn't generated
        Assumptions.assumeTrue(generatedCsvPath != null, "CSV file must be generated first");

        // Given
        String jobId = jobService.createJob();
        studentRepository.deleteAll(); // Clean database

        // When
        csvToDatabaseService.uploadCsvToDatabase(jobId, generatedCsvPath);

        // Then - wait for async completion
        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
            JobService.JobInfo jobInfo = jobService.getJob(jobId);
            assertThat(jobInfo.getStatus()).isEqualTo(JobStatus.COMPLETED);
        });

        // Verify records were inserted
        long count = studentRepository.count();
        assertThat(count).isEqualTo(100);
    }

    @Test
    @Order(4)
    @DisplayName("Integration: Should retrieve paginated report")
    void shouldRetrievePaginatedReport() {
        // Given - data should be in DB from previous test
        Assumptions.assumeTrue(studentRepository.count() > 0, "Database must have records");

        // When
        Page<Student> page = reportService.getStudents(null, null, PageRequest.of(0, 20));

        // Then
        assertThat(page.getContent()).hasSize(20);
        assertThat(page.getTotalElements()).isEqualTo(100);
        assertThat(page.getTotalPages()).isEqualTo(5);
    }

    @Test
    @Order(5)
    @DisplayName("Integration: Should filter report by class")
    void shouldFilterReportByClass() {
        // Given
        Assumptions.assumeTrue(studentRepository.count() > 0, "Database must have records");

        // When
        Page<Student> page = reportService.getStudents(null, StudentClass.Class1, PageRequest.of(0, 100));

        // Then
        assertThat(page.getContent()).allMatch(s -> s.getStudentClass() == StudentClass.Class1);
    }

    @Test
    @Order(6)
    @DisplayName("Integration: Should search report by studentId")
    void shouldSearchReportByStudentId() {
        // Given
        Assumptions.assumeTrue(studentRepository.count() > 0, "Database must have records");

        // When
        Page<Student> page = reportService.getStudents(1L, null, PageRequest.of(0, 10));

        // Then
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getStudentId()).isEqualTo(1L);
    }

    @Test
    @Order(7)
    @DisplayName("Integration: Should export report to CSV")
    void shouldExportReportToCsv() throws Exception {
        // Given
        Assumptions.assumeTrue(studentRepository.count() > 0, "Database must have records");

        // When
        ExportResponse response = reportService.exportToCsv(null, null);

        // Then
        assertThat(response.getFileName()).isEqualTo("students_report.csv");

        String csvContent = new String(Base64.getDecoder().decode(response.getData()));
        String[] lines = csvContent.split("\n");
        assertThat(lines.length).isEqualTo(101); // header + 100 records
    }

    @Test
    @Order(8)
    @DisplayName("Integration: Should export report to Excel")
    void shouldExportReportToExcel() throws Exception {
        // Given
        Assumptions.assumeTrue(studentRepository.count() > 0, "Database must have records");

        // When
        ExportResponse response = reportService.exportToExcel(null, null);

        // Then
        assertThat(response.getFileName()).isEqualTo("students_report.xlsx");
        assertThat(response.getData()).isNotEmpty();
    }

    @Test
    @Order(9)
    @DisplayName("Integration: Should export report to PDF")
    void shouldExportReportToPdf() {
        // Given
        Assumptions.assumeTrue(studentRepository.count() > 0, "Database must have records");

        // When
        ExportResponse response = reportService.exportToPdf(null, null);

        // Then
        assertThat(response.getFileName()).isEqualTo("students_report.pdf");

        byte[] pdfBytes = Base64.getDecoder().decode(response.getData());
        assertThat(new String(pdfBytes, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    @Order(10)
    @DisplayName("Integration: Should track job progress")
    void shouldTrackJobProgress() throws Exception {
        // Given
        String jobId = jobService.createJob();

        // When
        excelGeneratorService.generateStudentsExcel(jobId, 50);

        // Then - verify progress tracking works
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            JobService.JobInfo jobInfo = jobService.getJob(jobId);
            assertThat(jobInfo.getStatus()).isEqualTo(JobStatus.COMPLETED);
            assertThat(jobInfo.getProgress()).isEqualTo(100);
        });
    }
}
