package com.megan.dataproject.repository;

import com.megan.dataproject.model.Student;
import com.megan.dataproject.model.StudentClass;
import org.springframework.data.jpa.domain.Specification;

public class StudentSpecification {

    public static Specification<Student> hasStudentId(Long studentId) {
        return (root, query, cb) -> {
            if (studentId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("studentId"), studentId);
        };
    }

    public static Specification<Student> hasStudentClass(StudentClass studentClass) {
        return (root, query, cb) -> {
            if (studentClass == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("studentClass"), studentClass);
        };
    }

    public static Specification<Student> buildSpecification(Long studentId, StudentClass studentClass) {
        return Specification
                .where(hasStudentId(studentId))
                .and(hasStudentClass(studentClass));
    }
}
