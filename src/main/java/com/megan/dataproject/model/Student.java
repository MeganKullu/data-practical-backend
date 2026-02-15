package com.megan.dataproject.model;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studentId;

    private String firstName;

    private String lastName;

    private LocalDate DOB;

    @Enumerated(EnumType.STRING)
    @Column(name = "class")
    private StudentClass studentClass;

    private Double score;

}
