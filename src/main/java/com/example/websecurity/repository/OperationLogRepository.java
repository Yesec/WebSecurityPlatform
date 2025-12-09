package com.example.websecurity.repository;

import com.example.websecurity.entity.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    List<OperationLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<OperationLog> findByOperationTypeOrderByCreatedAtDesc(String operationType);

    @Query("SELECT ol FROM OperationLog ol WHERE ol.user.id = :userId AND ol.createdAt BETWEEN :startTime AND :endTime ORDER BY ol.createdAt DESC")
    List<OperationLog> findByUserIdAndCreatedAtBetween(@Param("userId") Long userId,
                                                      @Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime);

    @Query("SELECT ol FROM OperationLog ol WHERE ol.createdAt > :since ORDER BY ol.createdAt DESC")
    List<OperationLog> findRecentLogs(@Param("since") LocalDateTime since);
}