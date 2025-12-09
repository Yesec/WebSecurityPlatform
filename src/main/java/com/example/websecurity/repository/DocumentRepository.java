package com.example.websecurity.repository;

import com.example.websecurity.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByUserId(Long userId);

    List<Document> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT d FROM Document d WHERE d.user.id = :userId OR d.isPublic = true ORDER BY d.createdAt DESC")
    List<Document> findUserAndPublicDocuments(@Param("userId") Long userId);

    @Query("SELECT d FROM Document d WHERE d.isPublic = true ORDER BY d.createdAt DESC")
    List<Document> findPublicDocuments();

    List<Document> findByTitleContainingIgnoreCase(String title);

  Page<Document> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE (d.isPublic = true OR d.user.id = :userId) AND (:keyword IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Document> findAccessibleDocuments(@Param("keyword") String keyword, @Param("userId") Long userId, Pageable pageable);
}