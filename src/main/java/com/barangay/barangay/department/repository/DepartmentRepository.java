package com.barangay.barangay.department.repository;

import com.barangay.barangay.admin_management.model.User;
import com.barangay.barangay.department.model.Department;
import com.barangay.barangay.enumerated.Departments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department,Long> {
    Optional<Department> findByName(String name);
    Optional<Department> findByNameIgnoreCase(String name);

    long count();


    Optional<Department> findByName(Departments name);
}
