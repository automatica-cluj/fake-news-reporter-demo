package com.automatica.fakenews.repository;

import com.automatica.fakenews.model.FakeNewsReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FakeNewsReportRepository extends JpaRepository<FakeNewsReport, Long> {
    
    List<FakeNewsReport> findByApprovedTrueOrderByApprovedAtDesc();

    List<FakeNewsReport> findByApprovedFalseAndRejectedAtIsNullOrderByReportedAtDesc();

    List<FakeNewsReport> findByRejectedAtIsNotNullOrderByRejectedAtDesc();

    @Query("SELECT r FROM FakeNewsReport r WHERE r.approved = true OR r.rejectedAt IS NOT NULL ORDER BY CASE WHEN r.approved = true THEN r.approvedAt ELSE r.rejectedAt END DESC")
    List<FakeNewsReport> findApprovedAndRejectedReportsOrderByProcessedAtDesc();

    List<FakeNewsReport> findAllByOrderByReportedAtDesc();
}
