package com.megan.dataproject.service;

import com.megan.dataproject.model.JobStatus;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class CsvToDatabaseService {

    private final JdbcTemplate jdbcTemplate;
    private final JobService jobService;

    @Async
    public void uploadCsvToDatabase(String jobId, String csvPath) throws IOException {


        String sql = "INSERT INTO students(first_name, last_name, dob, class, score) VALUES (?,?,?,?,?)";

        try (BufferedReader br = Files.newBufferedReader(Paths.get(csvPath))) {

            jobService.updateStatus(jobId, JobStatus.PROCESSING, csvPath );

            String line;
            List<Object[]> batch = new ArrayList<>();
            boolean isHeader = true;
            long rowCount = 0;

            while ((line = br.readLine()) != null) {
                if (isHeader) { isHeader = false; continue;}

                String[] data = line.split(",");

                // Student database score = student CSV score + 5
                double csvScore = Double.parseDouble(data[5].trim());
                double finalScore = csvScore + 5;

                Object[] values = new Object[] {
                        data[1].trim(), //firstName
                        data[2].trim(), //lastName
                        LocalDate.parse(data[3].trim()), // DOB
                        data[4].trim(), //studentClass
                        finalScore
                };

                batch.add(values);
                rowCount++;

                //Push to DB every 5000 records for better performance
                if (batch.size() >= 5000) {
                    jdbcTemplate.batchUpdate(sql, batch);
                    batch.clear();

                    // Update progress every 10000 records
                    if (rowCount % 10000 == 0) {
                        jobService.updateProgress(jobId, rowCount, 0);
                    }
                }
            }

            // Flush remaining records that didn't reach batch size
            if (!batch.isEmpty()) {
                jdbcTemplate.batchUpdate(sql, batch);
            }

            jobService.updateStatus(jobId, JobStatus.COMPLETED, null);
        }
        catch (Exception e) {
            jobService.updateStatus(jobId, JobStatus.FAILED, e.getMessage());
        }
    }
}
