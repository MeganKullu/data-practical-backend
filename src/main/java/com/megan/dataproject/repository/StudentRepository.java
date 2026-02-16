package com.megan.dataproject.repository;

import com.megan.dataproject.model.Student;
import com.megan.dataproject.model.StudentClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    // Search by studentId
    Optional<Student> findByStudentId(Long studentId);

    // Filter by class with pagination
    Page<Student> findByStudentClass(StudentClass studentClass, Pageable pageable);

    // Search by studentId with pagination (for partial matches if needed)
    Page<Student> findByStudentId(Long studentId, Pageable pageable);

    // Filter by class
    Page<Student> findByStudentClassAndStudentId(StudentClass studentClass, Long studentId, Pageable pageable);
}
