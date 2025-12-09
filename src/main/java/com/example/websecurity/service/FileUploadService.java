package com.example.websecurity.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileUploadService {

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain",
        "text/markdown",
        "text/html",
        "application/json",
        "image/jpeg",
        "image/png",
        "image/gif"
    );

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        "pdf", "doc", "docx", "txt", "md", "html", "json", "jpg", "jpeg", "png", "gif"
    );

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.max-file-size:10485760}")
    private long maxFileSize;

    public UploadResult uploadFile(MultipartFile file, String username) throws IOException {
        validateFile(file);

        Path uploadPath = createUploadDirectory(username);

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String newFilename = generateUniqueFilename(extension);

        Path targetPath = uploadPath.resolve(newFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return new UploadResult(
            newFilename,
            originalFilename,
            file.getSize(),
            file.getContentType(),
            targetPath.toString()
        );
    }

    public boolean deleteFile(String filename, String username) {
        try {
            Path uploadPath = createUploadDirectory(username);
            Path filePath = uploadPath.resolve(filename);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }

    public Path getFilePath(String filename, String username) throws IOException {
        Path uploadPath = createUploadDirectory(username);
        return uploadPath.resolve(filename);
    }

    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("文件不能为空");
        }

        if (file.getSize() > maxFileSize) {
            throw new IOException("文件大小超过限制 (最大 " + (maxFileSize / 1024 / 1024) + "MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IOException("不支持的文件类型: " + contentType);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IOException("不支持的文件扩展名: " + extension);
        }
    }

    private Path createUploadDirectory(String username) throws IOException {
        Path userUploadPath = Paths.get(uploadDir, username);
        if (!Files.exists(userUploadPath)) {
            Files.createDirectories(userUploadPath);
        }
        return userUploadPath;
    }

    private String generateUniqueFilename(String extension) {
        String uuid = UUID.randomUUID().toString();
        return uuid + (extension != null ? "." + extension : "");
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : null;
    }

    public static class UploadResult {
        private final String filename;
        private final String originalFilename;
        private final long fileSize;
        private final String contentType;
        private final String filePath;

        public UploadResult(String filename, String originalFilename, long fileSize,
                          String contentType, String filePath) {
            this.filename = filename;
            this.originalFilename = originalFilename;
            this.fileSize = fileSize;
            this.contentType = contentType;
            this.filePath = filePath;
        }

        public String getFilename() { return filename; }
        public String getOriginalFilename() { return originalFilename; }
        public long getFileSize() { return fileSize; }
        public String getContentType() { return contentType; }
        public String getFilePath() { return filePath; }
    }
}