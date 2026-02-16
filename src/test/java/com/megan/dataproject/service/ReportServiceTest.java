package com.megan.dataproject.service;

import com.megan.dataproject.model.Student;
import com.megan.dataproject.model.StudentClass;
import com.megan.dataproject.payload.ExportResponse;
import com.megan.dataproject.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private ReportService reportService;

    private List<Student> testStudents;

    @BeforeEach
    void setUp() {
        testStudents = Arrays.asList(
                createStudent(1L, "John", "Doe", StudentClass.Class1, 85.0),
                createStudent(2L, "Jane", "Smith", StudentClass.Class2, 90.0),
                createStudent(3L, "Bob", "Wilson", StudentClass.Class1, 75.0)
        );
    }

    private Student createStudent(Long id, String firstName, String lastName,
                                   StudentClass studentClass, Double score) {
        Student student = new Student();
        student.setStudentId(id);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setDOB(LocalDate.of(2005, 1, 15));
        student.setStudentClass(studentClass);
        student.setScore(score);
        return student;
    }

    @Test
    @DisplayName("Should return paginated students")
    void shouldReturnPaginatedStudents() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Student> expectedPage = new PageImpl<>(testStudents, pageable, testStudents.size());
        when(studentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(expectedPage);

        // When
        Page<Student> result = reportService.getStudents(null, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should export to CSV with correct format")
    void shouldExportToCsvWithCorrectFormat() throws Exception {
        // Given
        when(studentRepository.findAll(any(Specification.class))).thenReturn(testStudents);

        // When
        ExportResponse response = reportService.exportToCsv(null, null);

        // Then
        assertThat(response.getFileName()).isEqualTo("students_report.csv");
        assertThat(response.getContentType()).isEqualTo("text/csv");

        // Decode and verify content
        String csvContent = new String(Base64.getDecoder().decode(response.getData()));
        String[] lines = csvContent.split("\n");

        assertThat(lines[0]).isEqualTo("studentId,firstName,lastName,DOB,class,score");
        assertThat(lines).hasSize(4); // header + 3 data rows
    }

    @Test
    @DisplayName("Should export to Excel with correct format")
    void shouldExportToExcelWithCorrectFormat() throws Exception {
        // Given
        when(studentRepository.findAll(any(Specification.class))).thenReturn(testStudents);

        // When
        ExportResponse response = reportService.exportToExcel(null, null);

        // Then
        assertThat(response.getFileName()).isEqualTo("students_report.xlsx");
        assertThat(response.getContentType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(response.getData()).isNotEmpty();
    }

    @Test
    @DisplayName("Should export to PDF with correct format")
    void shouldExportToPdfWithCorrectFormat() {
        // Given
        when(studentRepository.findAll(any(Specification.class))).thenReturn(testStudents);

        // When
        ExportResponse response = reportService.exportToPdf(null, null);

        // Then
        assertThat(response.getFileName()).isEqualTo("students_report.pdf");
        assertThat(response.getContentType()).isEqualTo("application/pdf");
        assertThat(response.getData()).isNotEmpty();

        // Verify it's a valid PDF (starts with %PDF)
        byte[] pdfBytes = Base64.getDecoder().decode(response.getData());
        String pdfHeader = new String(pdfBytes, 0, 4);
        assertThat(pdfHeader).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("Should filter by studentId")
    void shouldFilterByStudentId() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Student> filtered = List.of(testStudents.get(0));
        Page<Student> expectedPage = new PageImpl<>(filtered, pageable, 1);
        when(studentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(expectedPage);

        // When
        Page<Student> result = reportService.getStudents(1L, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should filter by studentClass")
    void shouldFilterByStudentClass() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Student> filtered = testStudents.stream()
                .filter(s -> s.getStudentClass() == StudentClass.Class1)
                .toList();
        Page<Student> expectedPage = new PageImpl<>(filtered, pageable, 2);
        when(studentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(expectedPage);

        // When
        Page<Student> result = reportService.getStudents(null, StudentClass.Class1, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Should handle empty result")
    void shouldHandleEmptyResult() throws Exception {
        // Given
        when(studentRepository.findAll(any(Specification.class))).thenReturn(List.of());

        // When
        ExportResponse response = reportService.exportToCsv(null, null);

        // Then
        String csvContent = new String(Base64.getDecoder().decode(response.getData()));
        String[] lines = csvContent.split("\n");

        // Only header row
        assertThat(lines).hasSize(1);
        assertThat(lines[0]).isEqualTo("studentId,firstName,lastName,DOB,class,score");
    }
}
