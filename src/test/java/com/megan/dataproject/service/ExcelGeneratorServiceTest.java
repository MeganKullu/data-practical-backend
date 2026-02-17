package com.megan.dataproject.service;

import com.megan.dataproject.model.JobStatus;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileInputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExcelGeneratorServiceTest {

    @Mock
    private FileStorageService storageService;

    @Mock
    private JobService jobService;

    @InjectMocks
    private ExcelGeneratorService excelGeneratorService;

    @TempDir
    Path tempDir;

    private String testFilePath;

    @BeforeEach
    void setUp() {
        testFilePath = tempDir.resolve("test_output.xlsx").toString();
    }

    @Test
    @DisplayName("Should generate Excel file with correct number of records")
    void shouldGenerateExcelWithCorrectRecordCount() throws Exception {
        // Given
        String jobId = "test-job-123";
        int recordCount = 100;
        when(storageService.getPath(any())).thenReturn(testFilePath);

        // When
        excelGeneratorService.generateStudentsExcel(jobId, recordCount);

        // Then - verify file was created with correct row count
        try (FileInputStream fis = new FileInputStream(testFilePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            // +1 for header row
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(recordCount + 1);
        }
    }

    @Test
    @DisplayName("Should generate Excel file with correct headers")
    void shouldGenerateExcelWithCorrectHeaders() throws Exception {
        // Given
        String jobId = "test-job-123";
        when(storageService.getPath(any())).thenReturn(testFilePath);

        // When
        excelGeneratorService.generateStudentsExcel(jobId, 10);

        // Then
        try (FileInputStream fis = new FileInputStream(testFilePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("studentId");
            assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("firstName");
            assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("lastName");
            assertThat(headerRow.getCell(3).getStringCellValue()).isEqualTo("DOB");
            assertThat(headerRow.getCell(4).getStringCellValue()).isEqualTo("class");
            assertThat(headerRow.getCell(5).getStringCellValue()).isEqualTo("score");
        }
    }

    @Test
    @DisplayName("Should update job status to PROCESSING then COMPLETED")
    void shouldUpdateJobStatusCorrectly() throws Exception {
        // Given
        String jobId = "test-job-123";
        when(storageService.getPath(any())).thenReturn(testFilePath);

        // When
        excelGeneratorService.generateStudentsExcel(jobId, 10);

        // Then
        verify(jobService).updateStatus(eq(jobId), eq(JobStatus.PROCESSING), isNull());
        verify(jobService).updateStatus(eq(jobId), eq(JobStatus.COMPLETED), eq(testFilePath));
    }

    @Test
    @DisplayName("Should generate valid student data with score between 55-75")
    void shouldGenerateValidStudentData() throws Exception {
        // Given
        String jobId = "test-job-123";
        when(storageService.getPath(any())).thenReturn(testFilePath);

        // When
        excelGeneratorService.generateStudentsExcel(jobId, 50);

        // Then
        try (FileInputStream fis = new FileInputStream(testFilePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= 50; i++) {
                Row row = sheet.getRow(i);

                // Verify studentId is incremental
                assertThat((int) row.getCell(0).getNumericCellValue()).isEqualTo(i);

                // Verify firstName length (3-8 chars)
                String firstName = row.getCell(1).getStringCellValue();
                assertThat(firstName.length()).isBetween(3, 8);

                // Verify score is between 55-75
                int score = (int) row.getCell(5).getNumericCellValue();
                assertThat(score).isBetween(55, 75);
            }
        }
    }

    @Test
    @DisplayName("Should update job status to FAILED on exception")
    void shouldUpdateJobStatusToFailedOnException() throws Exception {
        // Given
        String jobId = "test-job-123";
        when(storageService.getPath(any())).thenReturn("/invalid/path/file.xlsx");

        // When
        excelGeneratorService.generateStudentsExcel(jobId, 10);

        // Then
        verify(jobService).updateStatus(eq(jobId), eq(JobStatus.FAILED), any());
    }
}
