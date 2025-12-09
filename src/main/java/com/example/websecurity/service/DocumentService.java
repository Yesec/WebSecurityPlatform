package com.example.websecurity.service;

import com.example.websecurity.entity.Document;
import com.example.websecurity.entity.OperationLog;
import com.example.websecurity.entity.User;
import com.example.websecurity.repository.DocumentRepository;
import com.example.websecurity.repository.OperationLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private OperationLogRepository operationLogRepository;

    public Document createDocument(String title, String content, Boolean isPublic, User user, String ipAddress, String userAgent) {
        Document document = new Document();
        document.setTitle(title);
        document.setContent(content);
        document.setIsPublic(isPublic != null ? isPublic : false);
        document.setUser(user);

        Document savedDocument = documentRepository.save(document);

        operationLogRepository.save(new OperationLog(user, "文档创建",
            String.format("创建文档: %s (公开: %s)", title, savedDocument.getIsPublic()),
            ipAddress, userAgent));

        return savedDocument;
    }

    public Optional<Document> findDocumentById(Long id) {
        return documentRepository.findById(id);
    }

    public Optional<Document> findAccessibleDocument(Long id, User currentUser) {
        Optional<Document> docOpt = documentRepository.findById(id);
        if (docOpt.isPresent()) {
            Document document = docOpt.get();
            if (document.getIsPublic() ||
                (currentUser != null && (document.getUser().getId().equals(currentUser.getId()) ||
                 currentUser.getRole().equals("ADMIN")))) {
                return docOpt;
            }
        }
        return Optional.empty();
    }

    public List<Document> getUserDocuments(Long userId) {
        return documentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Document> getUserAndPublicDocuments(Long userId) {
        return documentRepository.findUserAndPublicDocuments(userId);
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    public List<Document> getPublicDocuments() {
        return documentRepository.findPublicDocuments();
    }

    public Page<Document> searchDocuments(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return documentRepository.findByTitleContainingIgnoreCase(keyword.trim(), pageable);
        }
        return documentRepository.findAll(pageable);
    }

    public Page<Document> searchDocuments(String keyword, User currentUser, Pageable pageable) {
        if (currentUser != null && "ADMIN".equals(currentUser.getRole())) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                return documentRepository.findByTitleContainingIgnoreCase(keyword.trim(), pageable);
            }
            return documentRepository.findAll(pageable);
        } else {
            Long userId = (currentUser != null) ? currentUser.getId() : -1L;
            return documentRepository.findAccessibleDocuments(
                (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null,
                userId,
                pageable
            );
        }
    }

    public Document updateDocument(Long id, String title, String content, Boolean isPublic, User currentUser, String ipAddress, String userAgent) {
        Optional<Document> docOpt = documentRepository.findById(id);
        if (!docOpt.isPresent()) {
            throw new RuntimeException("文档不存在");
        }

        Document document = docOpt.get();

        if (!document.getUser().getId().equals(currentUser.getId()) &&
            !currentUser.getRole().equals("ADMIN")) {
            throw new RuntimeException("无权限修改此文档");
        }

        String oldTitle = document.getTitle();
        document.setTitle(title);
        document.setContent(content);
        if (isPublic != null) {
            document.setIsPublic(isPublic);
        }

        Document updatedDocument = documentRepository.save(document);

        operationLogRepository.save(new OperationLog(currentUser, "文档更新",
            String.format("更新文档: %s -> %s", oldTitle, title),
            ipAddress, userAgent));

        return updatedDocument;
    }

    public void save(Document document) {
        documentRepository.save(document);
    }

    public void deleteDocument(Long id, User currentUser, String ipAddress, String userAgent) {
        Optional<Document> docOpt = documentRepository.findById(id);
        if (!docOpt.isPresent()) {
            throw new RuntimeException("文档不存在");
        }

        Document document = docOpt.get();

        if (!document.getUser().getId().equals(currentUser.getId()) &&
            !currentUser.getRole().equals("ADMIN")) {
            throw new RuntimeException("无权限删除此文档");
        }

        String title = document.getTitle();
        documentRepository.delete(document);

        operationLogRepository.save(new OperationLog(currentUser, "文档删除",
            String.format("删除文档: %s", title),
            ipAddress, userAgent));
    }

    public List<Document> searchDocumentsByTitle(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return documentRepository.findByTitleContainingIgnoreCase(keyword.trim());
        }
        return documentRepository.findAll();
    }

    // Enhanced methods for categories and tags
    public List<String> getAllCategories() {
        return documentRepository.findAll().stream()
                .map(Document::getCategory)
                .filter(Objects::nonNull)
                .filter(category -> !category.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());
    }

    public List<String> getAllTags() {
        return documentRepository.findAll().stream()
                .map(Document::getTags)
                .filter(Objects::nonNull)
                .flatMap(tags -> java.util.Arrays.stream(tags.split(","))
                        .map(String::trim)
                        .filter(tag -> !tag.isEmpty()))
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());
    }

    public List<Document> searchByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return documentRepository.findAll().stream()
                .filter(doc -> category.equals(doc.getCategory()))
                .sorted((d1, d2) -> d2.getCreatedAt().compareTo(d1.getCreatedAt()))
                .collect(java.util.stream.Collectors.toList());
    }

    public List<Document> searchByTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return documentRepository.findAll().stream()
                .filter(doc -> doc.getTags() != null &&
                        java.util.Arrays.stream(doc.getTags().split(","))
                                .map(String::trim)
                                .anyMatch(t -> t.equals(tag)))
                .sorted((d1, d2) -> d2.getCreatedAt().compareTo(d1.getCreatedAt()))
                .collect(java.util.stream.Collectors.toList());
    }

    // Statistics methods
    public DocumentStats getDocumentStats(Long userId) {
        List<Document> userDocs = getUserDocuments(userId);
        long publicDocs = userDocs.stream().mapToLong(doc -> doc.getIsPublic() ? 1 : 0).sum();
        long privateDocs = userDocs.size() - publicDocs;
        long totalViews = userDocs.stream().mapToLong(doc -> doc.getViewCount() != null ? doc.getViewCount() : 0).sum();
        long totalDownloads = userDocs.stream().mapToLong(doc -> doc.getDownloadCount() != null ? doc.getDownloadCount() : 0).sum();

        return new DocumentStats(userDocs.size(), publicDocs, privateDocs, totalViews, totalDownloads);
    }

    public void incrementViewCount(Long documentId) {
        java.util.Optional<Document> docOpt = documentRepository.findById(documentId);
        if (docOpt.isPresent()) {
            Document document = docOpt.get();
            if (document.getViewCount() == null) {
                document.setViewCount(0L);
            }
            document.setViewCount(document.getViewCount() + 1);
            documentRepository.save(document);
        }
    }

    public void incrementDownloadCount(Long documentId) {
        java.util.Optional<Document> docOpt = documentRepository.findById(documentId);
        if (docOpt.isPresent()) {
            Document document = docOpt.get();
            if (document.getDownloadCount() == null) {
                document.setDownloadCount(0L);
            }
            document.setDownloadCount(document.getDownloadCount() + 1);
            documentRepository.save(document);
        }
    }

    // DocumentStats inner class
    public static class DocumentStats {
        private final long totalDocuments;
        private final long publicDocuments;
        private final long privateDocuments;
        private final long totalViews;
        private final long totalDownloads;

        public DocumentStats(long totalDocuments, long publicDocuments, long privateDocuments,
                           long totalViews, long totalDownloads) {
            this.totalDocuments = totalDocuments;
            this.publicDocuments = publicDocuments;
            this.privateDocuments = privateDocuments;
            this.totalViews = totalViews;
            this.totalDownloads = totalDownloads;
        }

        // Getters
        public long getTotalDocuments() { return totalDocuments; }
        public long getPublicDocuments() { return publicDocuments; }
        public long getPrivateDocuments() { return privateDocuments; }
        public long getTotalViews() { return totalViews; }
        public long getTotalDownloads() { return totalDownloads; }
    }
}
