package com.megan.dataproject.service;

import com.megan.dataproject.model.StudentClass;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class ExcelGeneratorService {

    private final FileStorageService storageService;

    public String generateStudentsExcel(int count) throws IOException {
        String fileName = "StudentData_" + System.currentTimeMillis() + ".xlsx";
        String fullPath = storageService.getPath(fileName);

        //Keep only 100 rows in memory

        try(SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("Students");

            // Header

            Row header = sheet.createRow(0);
            String[] cols = {"studentId, firstName","lastName", "DOB","class","score"};
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
               row.createCell(5).setCellValue(55 + (random.nextDouble() * 20));// 55 to 75

            }

            try (FileOutputStream out = new FileOutputStream(fullPath)) {
                workbook.write(out);
            }

            workbook.dispose();

        }

        return fullPath;

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
