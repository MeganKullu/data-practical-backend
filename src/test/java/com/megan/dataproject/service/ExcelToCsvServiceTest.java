package com.megan.dataproject.service;

import com.megan.dataproject.model.JobStatus;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExcelToCsvServiceTest {

    @Mock
    private FileStorageService storageService;

    @Mock
    private JobService jobService;

    @InjectMocks
    private ExcelToCsvService excelToCsvService;

    @TempDir
    Path tempDir;

    private File inputExcelFile;
    private String outputCsvPath;

    @BeforeEach
    void setUp() throws Exception {
        // Create test Excel file
        inputExcelFile = tempDir.resolve("test_input.xlsx").toFile();
        outputCsvPath = tempDir.resolve("test_output.csv").toString();

        createTestExcelFile(inputExcelFile);
    }

    private void createTestExcelFile(File file) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(file)) {

            Sheet sheet = workbook.createSheet("Students");

            // Header
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("studentId");
            header.createCell(1).setCellValue("firstName");
            header.createCell(2).setCellValue("lastName");
            header.createCell(3).setCellValue("DOB");
            header.createCell(4).setCellValue("class");
            header.createCell(5).setCellValue("score");

            // Data rows
            for (int i = 1; i <= 5; i++) {
                Row row = sheet.createRow(i);
                row.createCell(0).setCellValue(String.valueOf(i));
                row.createCell(1).setCellValue("John" + i);
                row.createCell(2).setCellValue("Doe" + i);
                row.createCell(3).setCellValue("2005-01-1" + i);
                row.createCell(4).setCellValue("Class" + i);
                row.createCell(5).setCellValue(String.valueOf(60.0 + i));
            }

            workbook.write(fos);
        }
    }

    @Test
    @DisplayName("Should convert Excel to CSV with correct row count")
    void shouldConvertExcelToCsvWithCorrectRowCount() throws Exception {
        // Given
        String jobId = "test-job-123";
        when(storageService.getPath(any())).thenReturn(outputCsvPath);

        // When
        excelToCsvService.convertExceltoCsv(inputExcelFile, jobId);

        // Then
        List<String> lines = Files.readAllLines(Path.of(outputCsvPath));
        // 1 header + 5 data rows
        assertThat(lines).hasSize(6);
    }

    @Test
    @DisplayName("Should add 10 to each score during conversion")
    void shouldAdd10ToScoreDuringConversion() throws Exception {
        // Given
        String jobId = "test-job-123";
        when(storageService.getPath(any())).thenReturn(outputCsvPath);

        // When
        excelToCsvService.convertExceltoCsv(inputExcelFile, jobId);

        // Then
        List<String> lines = Files.readAllLines(Path.of(outputCsvPath));

        // Check first data row (original score was 61.0, should now be 71.0)
        String[] firstDataRow = lines.get(1).split(",");
        double score = Double.parseDouble(firstDataRow[5].trim());
        assertThat(score).isEqualTo(71.0);
    }

    @Test
    @DisplayName("Should generate CSV with correct headers")
    void shouldGenerateCsvWithCorrectHeaders() throws Exception {
        // Given
        String jobId = "test-job-123";
        when(storageService.getPath(any())).thenReturn(outputCsvPath);

        // When
        excelToCsvService.convertExceltoCsv(inputExcelFile, jobId);

        // Then
        List<String> lines = Files.readAllLines(Path.of(outputCsvPath));
        assertThat(lines.get(0)).isEqualTo("studentId,firstName,lastName,DOB,class,score");
    }

    @Test
    @DisplayName("Should update job status to PROCESSING then COMPLETED")
    void shouldUpdateJobStatusCorrectly() throws Exception {
        // Given
        String jobId = "test-job-123";
        when(storageService.getPath(any())).thenReturn(outputCsvPath);

        // When
        excelToCsvService.convertExceltoCsv(inputExcelFile, jobId);

        // Then
        verify(jobService).updateStatus(eq(jobId), eq(JobStatus.PROCESSING), isNull());
        verify(jobService).updateStatus(eq(jobId), eq(JobStatus.COMPLETED), eq(outputCsvPath));
    }

    @Test
    @DisplayName("Should update job status to FAILED on invalid file")
    void shouldUpdateJobStatusToFailedOnInvalidFile() {
        // Given
        String jobId = "test-job-123";
        File invalidFile = new File("/nonexistent/file.xlsx");
        when(storageService.getPath(any())).thenReturn(outputCsvPath);

        // When
        excelToCsvService.convertExceltoCsv(invalidFile, jobId);

        // Then
        verify(jobService).updateStatus(eq(jobId), eq(JobStatus.FAILED), any());
    }
}
