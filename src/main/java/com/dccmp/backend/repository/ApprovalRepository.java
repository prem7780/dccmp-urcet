package com.dccmp.backend.repository;

import com.dccmp.backend.entity.*;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {
    List<Approval> findByApplicationIdOrderByDepartmentId(Long applicationId);

    Optional<Approval> findByApplicationIdAndDepartmentId(Long applicationId, Long departmentId);

    List<Approval> findByDepartmentIdOrderByIdDesc(Long departmentId);
}
