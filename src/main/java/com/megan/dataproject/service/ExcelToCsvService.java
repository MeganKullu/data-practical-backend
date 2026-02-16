package com.megan.dataproject.service;
import com.github.pjfanning.xlsx.StreamingReader;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;


@Service
@RequiredArgsConstructor
public class ExcelToCsvService {

    private final FileStorageService storageService;

    public String convertExceltoCsv(File inputFile) throws IOException {
        String outputFileName = "ProcessedData_" + System.currentTimeMillis() + ".csv";
        String outputPath = storageService.getPath(outputFileName);

        // 1.  Open Excel as a Stream (Low Memory)

        try (InputStream is = new FileInputStream(inputFile);
             Workbook workbook = StreamingReader.builder()
                     .rowCacheSize(100) // number of rows to keep in memory
                     .bufferSize(4096) //buffer size to use when reading InputStream
                     .open(is);

             BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath))) {

            Sheet sheet = workbook.getSheetAt(0);
            boolean isHeader = true;

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
                double originalScore = Double.parseDouble(getCellValue(row, 5));
                double updatedScore = originalScore + 10;

                // 4. Write to CSV
                String csvRow = String.format("%s,%s,%s,%s,%s, %.2f",
                        id, fName, lName, dob, studentClass, updatedScore);

                writer.write(csvRow);
                writer.newLine();
            }
        }
        return outputPath;
    }

    private String getCellValue(Row row, int index) {
        Cell cell = row.getCell(index);
        return (cell == null) ? "" : cell.getStringCellValue();
    }

}
