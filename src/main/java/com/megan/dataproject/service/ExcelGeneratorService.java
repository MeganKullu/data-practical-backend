package com.megan.dataproject.service;

import com.megan.dataproject.model.JobStatus;
import com.megan.dataproject.model.StudentClass;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelGeneratorService {

    private final FileStorageService storageService;
    private final JobService jobService;

    @Async
    public CompletableFuture<String> generateStudentsExcel(String jobId, int count) throws IOException {
        String fileName = "StudentData_" + System.currentTimeMillis() + ".xlsx";
        String fullPath = storageService.getPath(fileName);

        //Keep only 100 rows in memory
        long startTime = System.currentTimeMillis();
        log.info("Job {} - Starting Excel generation: {} records", jobId, count);

        try(SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            jobService.updateStatus(jobId, JobStatus.PROCESSING, null);
            Sheet sheet = workbook.createSheet("Students");

            // Header
            Row header = sheet.createRow(0);
            String[] cols = {"studentId", "firstName", "lastName", "DOB", "class", "score"};
            for (int i =0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }

            Random random = new Random();

            for (int i = 1; i <= count; i++) {
               Row row = sheet.createRow(i);
               row.createCell(0).setCellValue(i);
               row.createCell(1).setCellValue(randomAlpha(random,3,8));
               row.createCell(2).setCellValue(randomAlpha(random,3,8));
               row.createCell(3).setCellValue(randomDate(random).toString());
               row.createCell(4).setCellValue(StudentClass.getRandom().name());
               row.createCell(5).setCellValue(55 + random.nextInt(21)); // 55 to 75

               // Update progress every 10000 records
               if (i % 10000 == 0) {
                   int percent = (int) ((i * 100L) / count);
                   log.info("Job {} - Excel generation: {}/{} ({}%)", jobId, i, count, percent);
                   jobService.updateProgress(jobId, i, count);
               }
            }

            try (FileOutputStream out = new FileOutputStream(fullPath)) {
                workbook.write(out);
            }

            workbook.dispose();
            jobService.updateStatus(jobId, JobStatus.COMPLETED, fullPath);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Job {} - Excel generation COMPLETED in {}ms: {}", jobId, duration, fullPath);
        }
        catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Job {} - Excel generation FAILED in {}ms: {}", jobId, duration, e.getMessage());
            jobService.updateStatus(jobId, JobStatus.FAILED, e.getMessage());
        }

        return CompletableFuture.completedFuture(fullPath);

    }

    private String randomAlpha(Random r, int min, int max) {
        int length = r.nextInt(max - min + 1) + min;
        return r.ints(97, 123) // 'a' to 'z'
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private LocalDate randomDate(Random r) {
        long minDay = LocalDate.of(2000, 1, 1).toEpochDay();
        long maxDay = LocalDate.of(2010, 12, 31).toEpochDay();
        long randomDay = minDay + r.nextLong(maxDay - minDay);
        return LocalDate.ofEpochDay(randomDay);
    }
}
