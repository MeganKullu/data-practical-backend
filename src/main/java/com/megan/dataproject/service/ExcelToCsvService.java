package com.megan.dataproject.service;

import com.github.pjfanning.xlsx.StreamingReader;
import com.megan.dataproject.model.JobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;


@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelToCsvService {

    private final FileStorageService storageService;
    private final JobService jobService;

    @Async
    public void convertExceltoCsv(File inputFile, String jobId) {
        String outputFileName = "ProcessedData_" + System.currentTimeMillis() + ".csv";
        String outputPath = storageService.getPath(outputFileName);

        long startTime = System.currentTimeMillis();
        log.info("Job {} - Starting Excel to CSV conversion", jobId);

        try {
            jobService.updateStatus(jobId, JobStatus.PROCESSING, null);

            // 1.  Open Excel as a Stream (Low Memory)
            try (InputStream is = new FileInputStream(inputFile);
                 Workbook workbook = StreamingReader.builder()
                         .rowCacheSize(100) // number of rows to keep in memory
                         .bufferSize(4096) //buffer size to use when reading InputStream
                         .open(is);

                 BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath))) {

                Sheet sheet = workbook.getSheetAt(0);
                boolean isHeader = true;
                long rowCount = 0;

                for (Row row : sheet) {
                    if (isHeader) {
                        //Write CSV Header
                        writer.write("studentId,firstName,lastName,DOB,class,score");
                        writer.newLine();
                        isHeader = false;
                        continue;
                    }

                    // 2. Extract Data
                    String id = getCellValue(row, 0);
                    String fName = getCellValue(row, 1);
                    String lName = getCellValue(row,2);
                    String dob = getCellValue(row, 3);
                    String studentClass = getCellValue(row,4);

                    // 3. APPLY LOGIC: score + 10
                    int originalScore = (int) Double.parseDouble(getCellValue(row, 5));
                    int updatedScore = originalScore + 10;

                    // 4. Write to CSV
                    String csvRow = String.format("%s,%s,%s,%s,%s,%d",
                            id, fName, lName, dob, studentClass, updatedScore);

                    writer.write(csvRow);
                    writer.newLine();

                    rowCount++;
                    // Update progress every 10000 records (total unknown for streaming)
                    if (rowCount % 10000 == 0) {
                        log.info("Job {} - Excel to CSV: {} rows processed", jobId, rowCount);
                        jobService.updateProgress(jobId, rowCount, 0);
                    }
                }
            }

            jobService.updateStatus(jobId, JobStatus.COMPLETED, outputPath);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Job {} - Excel to CSV COMPLETED in {}ms: {}", jobId, duration, outputPath);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Job {} - Excel to CSV FAILED in {}ms: {}", jobId, duration, e.getMessage());
            jobService.updateStatus(jobId, JobStatus.FAILED, e.getMessage());
        }
    }

    private String getCellValue(Row row, int index) {
        Cell cell = row.getCell(index);
        return (cell == null) ? "" : cell.getStringCellValue();
    }

}
