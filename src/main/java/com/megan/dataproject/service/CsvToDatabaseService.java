package com.megan.dataproject.service;

import lombok.RequiredArgsConstructor;
import org.apache.xmlbeans.impl.xb.ltgfmt.TestCase;
import org.springframework.jdbc.core.JdbcTemplate;
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

    public void uploadCsvToDatabase(String csvPath) throws IOException {

        String sql = "INSERT INTO students(first_name, last_name, dob, class, score) VALUES (?,?,?,?,?)";

        try (BufferedReader br = new Files.newBufferedReader(Paths.get(csvPath))){
            String line;
            List<Object[]> batch = new ArrayList<>();
            boolean isHeader = true;

            while ((line = br.readLine()) != null) {
                if (isHeader) { isHeader = false; continue;}

                String[] data = line.split(",");

                // Student database score = student Excel score + 5
                double csvScore = Double.parseDouble(data[5]);
                double finalScore = csvScore + 5;

                Object[] values = new Object[] {
                        data[1], //firstName
                        data[2], //lastName
                        LocalDate.parse(data[3]), // DOB
                        data[4], //studentClass
                        finalScore
                };

                batch.add(values);

                //Push to DB every 1000 records

                if (batch.size() >= 1000) {
                    jdbcTemplate.batchUpdate(sql, batch);
                    batch.clear();
                }

            }
        }
    }
}
