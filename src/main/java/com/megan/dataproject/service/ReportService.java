package com.megan.dataproject.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.megan.dataproject.model.Student;
import com.megan.dataproject.model.StudentClass;
import com.megan.dataproject.payload.ExportResponse;
import com.megan.dataproject.repository.StudentRepository;
import com.megan.dataproject.repository.StudentSpecification;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final StudentRepository studentRepository;

    // Get paginated students
    public Page<Student> getStudents(Long studentId, StudentClass studentClass, Pageable pageable) {
        Specification<Student> spec = StudentSpecification.buildSpecification(studentId, studentClass);
        return studentRepository.findAll(spec, pageable);
    }

    // Export to CSV
    public ExportResponse exportToCsv(Long studentId, StudentClass studentClass) throws IOException {
        List<Student> students = getFilteredStudents(studentId, studentClass);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            writer.print("studentId,firstName,lastName,DOB,class,score\n");

            for (Student s : students) {
                writer.printf("%d,%s,%s,%s,%s,%d\n",
                        s.getStudentId(),
                        s.getFirstName(),
                        s.getLastName(),
                        s.getDOB(),
                        s.getStudentClass().name(),
                        s.getScore()
                );
            }
        }

        String base64Data = Base64.getEncoder().encodeToString(out.toByteArray());
        return new ExportResponse("students_report.csv", "text/csv", base64Data);
    }

    // Export to Excel
    public ExportResponse exportToExcel(Long studentId, StudentClass studentClass) throws IOException {
        List<Student> students = getFilteredStudents(studentId, studentClass);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("Students");

            // Header
            Row header = sheet.createRow(0);
            String[] cols = {"studentId", "firstName", "lastName", "DOB", "class", "score"};
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }

            // Data rows
            int rowNum = 1;
            for (Student s : students) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(s.getStudentId());
                row.createCell(1).setCellValue(s.getFirstName());
                row.createCell(2).setCellValue(s.getLastName());
                row.createCell(3).setCellValue(s.getDOB().toString());
                row.createCell(4).setCellValue(s.getStudentClass().name());
                row.createCell(5).setCellValue(s.getScore());
            }

            workbook.write(out);
            workbook.dispose();
        }

        String base64Data = Base64.getEncoder().encodeToString(out.toByteArray());
        return new ExportResponse(
                "students_report.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                base64Data
        );
    }

    // Export to PDF using OpenPDF
    public ExportResponse exportToPdf(Long studentId, StudentClass studentClass) {
        List<Student> students = getFilteredStudents(studentId, studentClass);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Paragraph title = new Paragraph("Student Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Table with 6 columns
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setWidths(new float[]{1f, 1.5f, 1.5f, 1.5f, 1f, 1f});

            // Header row
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            String[] headers = {"ID", "First Name", "Last Name", "DOB", "Class", "Score"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(new Color(66, 133, 244));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(8);
                table.addCell(cell);
            }

            // Data rows
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            for (Student s : students) {
                table.addCell(new Phrase(String.valueOf(s.getStudentId()), dataFont));
                table.addCell(new Phrase(s.getFirstName(), dataFont));
                table.addCell(new Phrase(s.getLastName(), dataFont));
                table.addCell(new Phrase(s.getDOB().toString(), dataFont));
                table.addCell(new Phrase(s.getStudentClass().name(), dataFont));
                table.addCell(new Phrase(String.valueOf(s.getScore()), dataFont));
            }

            document.add(table);

            // Footer
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);
            Paragraph footer = new Paragraph("Total Records: " + students.size(), footerFont);
            footer.setSpacingBefore(20);
            document.add(footer);

        } catch (DocumentException e) {
            throw new RuntimeException("Error generating PDF: " + e.getMessage(), e);
        } finally {
            document.close();
        }

        String base64Data = Base64.getEncoder().encodeToString(out.toByteArray());
        return new ExportResponse("students_report.pdf", "application/pdf", base64Data);
    }

    private List<Student> getFilteredStudents(Long studentId, StudentClass studentClass) {
        Specification<Student> spec = StudentSpecification.buildSpecification(studentId, studentClass);
        return studentRepository.findAll(spec);
    }
}
