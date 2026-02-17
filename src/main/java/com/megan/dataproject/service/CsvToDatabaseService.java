package com.megan.dataproject.service;

import com.megan.dataproject.model.JobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvToDatabaseService {

    private final JdbcTemplate jdbcTemplate;
    private final JobService jobService;

    @Async
    public void uploadCsvToDatabase(String jobId, String csvPath) throws IOException {


        String sql = "INSERT INTO students(student_id, first_name, last_name, dob, class, score) VALUES (?,?,?,?,?,?)";

        long startTime = System.currentTimeMillis();
        log.info("Job {} - Starting CSV to database upload: {}", jobId, csvPath);

        try (BufferedReader br = Files.newBufferedReader(Paths.get(csvPath))) {

            jobService.updateStatus(jobId, JobStatus.PROCESSING, csvPath);

            String line;
            List<Object[]> batch = new ArrayList<>();
            boolean isHeader = true;
            long rowCount = 0;

            while ((line = br.readLine()) != null) {
                if (isHeader) { isHeader = false; continue;}

                String[] data = line.split(",");

                // Student database score = student CSV score + 5
                int csvScore = Integer.parseInt(data[5].trim());
                int finalScore = csvScore + 5;

                Object[] values = new Object[] {
                        Long.parseLong(data[0].trim()), // studentId
                        data[1].trim(), //firstName
                        data[2].trim(), //lastName
                        LocalDate.parse(data[3].trim()), // DOB
                        data[4].trim(), //studentClass
                        finalScore
                };

                batch.add(values);
                rowCount++;

                // Push to DB every 10000 records for better performance
                if (batch.size() >= 10000) {
                    jdbcTemplate.batchUpdate(sql, batch);
                    batch.clear();
                    log.info("Job {} - CSV to DB: {} rows inserted", jobId, rowCount);
                    jobService.updateProgress(jobId, rowCount, 0);
                }
            }

            // Flush remaining records that didn't reach batch size
            if (!batch.isEmpty()) {
                jdbcTemplate.batchUpdate(sql, batch);
            }

            jobService.updateStatus(jobId, JobStatus.COMPLETED, null);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Job {} - CSV to DB COMPLETED in {}ms: {} rows inserted", jobId, duration, rowCount);
        }
        catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Job {} - CSV to DB FAILED in {}ms: {}", jobId, duration, e.getMessage());
            jobService.updateStatus(jobId, JobStatus.FAILED, e.getMessage());
        }
    }
}
