package com.megan.dataproject.service;

import com.megan.dataproject.model.JobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsvToDatabaseServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private JobService jobService;

    @InjectMocks
    private CsvToDatabaseService csvToDatabaseService;

    @TempDir
    Path tempDir;

    private String csvFilePath;

    @BeforeEach
    void setUp() throws Exception {
        csvFilePath = tempDir.resolve("test_input.csv").toString();
    }

    private void createTestCsvFile(int recordCount) throws Exception {
        StringBuilder csv = new StringBuilder();
        csv.append("studentId,firstName,lastName,DOB,class,score\n");

        for (int i = 1; i <= recordCount; i++) {
            csv.append(String.format("%d,John%d,Doe%d,2005-01-%02d,Class%d,70.00%n",
                    i, i, i, (i % 28) + 1, (i % 5) + 1));
        }

        Files.writeString(Path.of(csvFilePath), csv.toString());
    }

    @Test
    @DisplayName("Should insert records into database with batch updates")
    void shouldInsertRecordsWithBatchUpdates() throws Exception {
        // Given
        String jobId = "test-job-123";
        createTestCsvFile(10);

        // When
        csvToDatabaseService.uploadCsvToDatabase(jobId, csvFilePath);

        // Then - verify batchUpdate was called
        verify(jdbcTemplate, atLeastOnce()).batchUpdate(anyString(), anyList());
    }

    @Test
    @DisplayName("Should add 5 to each score before inserting")
    void shouldAdd5ToScoreBeforeInserting() throws Exception {
        // Given
        String jobId = "test-job-123";
        createTestCsvFile(5);

        ArgumentCaptor<List<Object[]>> batchCaptor = ArgumentCaptor.forClass(List.class);

        // When
        csvToDatabaseService.uploadCsvToDatabase(jobId, csvFilePath);

        // Then
        verify(jdbcTemplate).batchUpdate(anyString(), batchCaptor.capture());

        List<Object[]> batch = batchCaptor.getValue();
        // Original score was 70.0, should now be 75.0
        for (Object[] row : batch) {
            double score = (Double) row[4];
            assertThat(score).isEqualTo(75.0);
        }
    }

    @Test
    @DisplayName("Should parse CSV data correctly")
    void shouldParseCsvDataCorrectly() throws Exception {
        // Given
        String jobId = "test-job-123";
        createTestCsvFile(3);

        ArgumentCaptor<List<Object[]>> batchCaptor = ArgumentCaptor.forClass(List.class);

        // When
        csvToDatabaseService.uploadCsvToDatabase(jobId, csvFilePath);

        // Then
        verify(jdbcTemplate).batchUpdate(anyString(), batchCaptor.capture());

        List<Object[]> batch = batchCaptor.getValue();
        assertThat(batch).hasSize(3);

        // Verify first row
        Object[] firstRow = batch.get(0);
        assertThat(firstRow[0]).isEqualTo("John1");  // firstName
        assertThat(firstRow[1]).isEqualTo("Doe1");   // lastName
        assertThat(firstRow[2]).isEqualTo(LocalDate.of(2005, 1, 2));  // DOB
        assertThat(firstRow[3]).isEqualTo("Class2"); // class
        assertThat(firstRow[4]).isEqualTo(75.0);     // score (+5)
    }

    @Test
    @DisplayName("Should flush remaining batch after loop")
    void shouldFlushRemainingBatchAfterLoop() throws Exception {
        // Given
        String jobId = "test-job-123";
        // Create 103 records (less than batch size of 5000)
        createTestCsvFile(103);

        // When
        csvToDatabaseService.uploadCsvToDatabase(jobId, csvFilePath);

        // Then - verify batchUpdate was called with remaining records
        ArgumentCaptor<List<Object[]>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate).batchUpdate(anyString(), batchCaptor.capture());

        assertThat(batchCaptor.getValue()).hasSize(103);
    }

    @Test
    @DisplayName("Should update job status to PROCESSING then COMPLETED")
    void shouldUpdateJobStatusCorrectly() throws Exception {
        // Given
        String jobId = "test-job-123";
        createTestCsvFile(5);

        // When
        csvToDatabaseService.uploadCsvToDatabase(jobId, csvFilePath);

        // Then
        verify(jobService).updateStatus(eq(jobId), eq(JobStatus.PROCESSING), eq(csvFilePath));
        verify(jobService).updateStatus(eq(jobId), eq(JobStatus.COMPLETED), isNull());
    }

    @Test
    @DisplayName("Should update job status to FAILED on invalid file")
    void shouldUpdateJobStatusToFailedOnInvalidFile() throws Exception {
        // Given
        String jobId = "test-job-123";
        String invalidPath = "/nonexistent/file.csv";

        // When
        csvToDatabaseService.uploadCsvToDatabase(jobId, invalidPath);

        // Then
        verify(jobService).updateStatus(eq(jobId), eq(JobStatus.FAILED), any());
    }

    @Test
    @DisplayName("Should skip header row")
    void shouldSkipHeaderRow() throws Exception {
        // Given
        String jobId = "test-job-123";
        createTestCsvFile(2);

        ArgumentCaptor<List<Object[]>> batchCaptor = ArgumentCaptor.forClass(List.class);

        // When
        csvToDatabaseService.uploadCsvToDatabase(jobId, csvFilePath);

        // Then
        verify(jdbcTemplate).batchUpdate(anyString(), batchCaptor.capture());

        // Should only have 2 data rows, not 3 (header excluded)
        assertThat(batchCaptor.getValue()).hasSize(2);
    }
}
