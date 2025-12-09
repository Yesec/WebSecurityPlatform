package com.example.websecurity.controller;

import com.example.websecurity.entity.Document;
import com.example.websecurity.entity.OperationLog;
import com.example.websecurity.entity.User;
import com.example.websecurity.service.DocumentService;
import com.example.websecurity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private DocumentService documentService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<User> users = userService.getAllUsers();
        List<Document> documents = documentService.getAllDocuments();
        
        long activeUsers = users.stream().filter(user -> Boolean.TRUE.equals(user.getIsActive())).count();
        long totalDocuments = documents.size();
        long publicDocuments = documents.stream().filter(doc -> Boolean.TRUE.equals(doc.getIsPublic())).count();
        long privateDocuments = totalDocuments - publicDocuments;
        long totalViews = documents.stream().mapToLong(doc -> doc.getViewCount() != null ? doc.getViewCount() : 0).sum();
        
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        List<OperationLog> todayLogs = userService.findRecentLogs(today);
        long todayOperations = todayLogs.size();
        
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        String memoryUsage = String.format("%.1f MB / %.1f MB", usedMemory / 1024.0 / 1024.0, totalMemory / 1024.0 / 1024.0);
        
        model.addAttribute("users", users);
        model.addAttribute("totalUsers", users.size());
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("totalDocuments", totalDocuments);
        model.addAttribute("publicDocuments", publicDocuments);
        model.addAttribute("privateDocuments", privateDocuments);
        model.addAttribute("totalViews", totalViews);
        model.addAttribute("todayOperations", todayOperations);
        model.addAttribute("memoryUsage", memoryUsage);
        model.addAttribute("javaVersion", System.getProperty("java.version"));
        model.addAttribute("springBootVersion", "2.7.0");
        model.addAttribute("uptime", getUptime());
        
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String listUsers(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           @RequestParam(required = false) String search,
                           @RequestParam(required = false) String role,
                           @RequestParam(required = false) String status,
                           Model model) {
        List<User> users = userService.getAllUsers();
        
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase();
            users = users.stream()
                    .filter(user -> user.getUsername().toLowerCase().contains(searchLower) ||
                                   user.getEmail().toLowerCase().contains(searchLower))
                    .collect(Collectors.toList());
        }
        
        if (role != null && !role.trim().isEmpty()) {
            String roleFilter = role.trim();
            users = users.stream()
                    .filter(user -> roleFilter.equals(user.getRole()))
                    .collect(Collectors.toList());
        }
        
        if (status != null && !status.trim().isEmpty()) {
            boolean isActive = "active".equals(status.trim());
            users = users.stream()
                    .filter(user -> Boolean.TRUE.equals(user.getIsActive()) == isActive)
                    .collect(Collectors.toList());
        }

        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, users.size());
        List<User> pageUsers = users.subList(startIndex, endIndex);

        model.addAttribute("users", pageUsers);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", (int) Math.ceil((double) users.size() / size));

        return "admin/users";
    }

    @GetMapping("/users/{id}")
    public String viewUser(@PathVariable Long id, Model model) {
        Optional<User> userOpt = userService.findUserById(id);
        if (!userOpt.isPresent()) {
            return "redirect:/admin/users?error=用户不存在";
        }

        User user = userOpt.get();
        model.addAttribute("user", user);

        return "admin/user-detail";
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        Optional<User> userOpt = userService.findUserById(id);
        if (!userOpt.isPresent()) {
            return "redirect:/admin/users?error=用户不存在";
        }

        model.addAttribute("user", userOpt.get());
        return "admin/user-edit";
    }

    @PostMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id,
                          @ModelAttribute User user,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        try {
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            userService.updateUser(id, user.getEmail(), user.getFullName(),
                                 user.getRole(), user.getIsActive(), ipAddress, userAgent);

            redirectAttributes.addFlashAttribute("success", "用户信息更新成功");
            return "redirect:/admin/users/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/users/" + id + "/edit";
        }
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                            HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String operatorUsername = auth.getName();

            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            userService.deleteUser(id, operatorUsername, ipAddress, userAgent);

            redirectAttributes.addFlashAttribute("success", "用户删除成功");
            return "redirect:/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/users";
        }
    }

    @GetMapping("/documents")
    public String listDocuments(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(required = false) String search,
                               @RequestParam(required = false) String category,
                               @RequestParam(required = false) String visibility,
                               Model model) {
        
        List<Document> documents = documentService.getAllDocuments();
        
        if (search != null && !search.trim().isEmpty()) {
            documents = documentService.searchDocumentsByTitle(search.trim());
        }
        
        if (category != null && !category.trim().isEmpty()) {
            String categoryFilter = category.trim();
            documents = documentService.searchByCategory(categoryFilter);
        }
        
        if (visibility != null && !visibility.trim().isEmpty()) {
            boolean isPublic = "public".equals(visibility.trim());
            documents = documents.stream()
                    .filter(doc -> Boolean.TRUE.equals(doc.getIsPublic()) == isPublic)
                    .collect(Collectors.toList());
        }

        long totalDocuments = documents.size();
        long publicDocuments = documents.stream().filter(doc -> Boolean.TRUE.equals(doc.getIsPublic())).count();
        long privateDocuments = totalDocuments - publicDocuments;
        long totalViews = documents.stream().mapToLong(doc -> doc.getViewCount() != null ? doc.getViewCount() : 0).sum();

        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, documents.size());
        List<Document> pageDocuments = documents.subList(startIndex, endIndex);

        model.addAttribute("documents", pageDocuments);
        model.addAttribute("categories", documentService.getAllCategories());
        model.addAttribute("totalDocuments", totalDocuments);
        model.addAttribute("publicDocuments", publicDocuments);
        model.addAttribute("privateDocuments", privateDocuments);
        model.addAttribute("totalViews", totalViews);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", (int) Math.ceil((double) documents.size() / size));

        return "admin/documents";
    }

    @PostMapping("/documents/{id}/delete")
    public String deleteDocument(@PathVariable Long id,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findUserByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("当前用户不存在"));

            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            documentService.deleteDocument(id, currentUser, ipAddress, userAgent);

            redirectAttributes.addFlashAttribute("success", "文档删除成功");
            return "redirect:/admin/documents";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/documents";
        }
    }

    @GetMapping("/logs")
    public String viewLogs(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String operationType,
                          @RequestParam(required = false) String username,
                          @RequestParam(required = false) String startDate,
                          @RequestParam(required = false) String endDate,
                          Model model) {

        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<OperationLog> logs = userService.findRecentLogs(since);

        if (username != null && !username.trim().isEmpty()) {
            String usernameFilter = username.trim().toLowerCase();
            logs = logs.stream()
                    .filter(log -> log.getUser().getUsername().toLowerCase().contains(usernameFilter))
                    .collect(Collectors.toList());
        }

        if (operationType != null && !operationType.trim().isEmpty()) {
            String opType = operationType.trim();
            logs = logs.stream()
                    .filter(log -> opType.equals(log.getOperationType()))
                    .collect(Collectors.toList());
        }

        if (startDate != null && !startDate.trim().isEmpty()) {
            try {
                LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
                logs = logs.stream()
                        .filter(log -> !log.getCreatedAt().isBefore(start))
                        .collect(Collectors.toList());
            } catch (Exception e) {
            }
        }

        if (endDate != null && !endDate.trim().isEmpty()) {
            try {
                LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");
                logs = logs.stream()
                        .filter(log -> !log.getCreatedAt().isAfter(end))
                        .collect(Collectors.toList());
            } catch (Exception e) {
            }
        }

        long todayOperations = logs.stream()
                .filter(log -> log.getCreatedAt().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
                .count();
        
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long weekOperations = logs.stream()
                .filter(log -> !log.getCreatedAt().isBefore(weekAgo))
                .count();
        
        long activeUsers = logs.stream()
                .filter(log -> !log.getCreatedAt().isBefore(weekAgo))
                .map(OperationLog::getUser)
                .distinct()
                .count();

        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, logs.size());
        List<OperationLog> pageLogs = logs.subList(startIndex, endIndex);

        model.addAttribute("logs", pageLogs);
        model.addAttribute("totalLogs", logs.size());
        model.addAttribute("todayOperations", todayOperations);
        model.addAttribute("weekOperations", weekOperations);
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", (int) Math.ceil((double) logs.size() / size));
        model.addAttribute("operationType", operationType);

        return "admin/logs";
    }

    @GetMapping("/logs/export")
    public void exportLogs(@RequestParam(required = false) String operationType,
                          @RequestParam(required = false) String username,
                          @RequestParam(required = false) String startDate,
                          @RequestParam(required = false) String endDate,
                          HttpServletResponse response) throws IOException {
        
        List<OperationLog> logs = userService.findRecentLogs(LocalDateTime.now().minusDays(30));
        
        if (username != null && !username.trim().isEmpty()) {
            String usernameFilter = username.trim().toLowerCase();
            logs = logs.stream()
                    .filter(log -> log.getUser().getUsername().toLowerCase().contains(usernameFilter))
                    .collect(Collectors.toList());
        }

        if (operationType != null && !operationType.trim().isEmpty()) {
            String opType = operationType.trim();
            logs = logs.stream()
                    .filter(log -> opType.equals(log.getOperationType()))
                    .collect(Collectors.toList());
        }

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=logs_" + 
                         LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");

        java.io.PrintWriter writer = response.getWriter();
        writer.println("时间,用户,操作类型,操作详情,IP地址,用户代理");
        
        for (OperationLog log : logs) {
            writer.printf("%s,%s,%s,\"%s\",%s,\"%s\"%n",
                    log.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    log.getUser().getUsername(),
                    log.getOperationType(),
                    log.getOperationDetail() != null ? log.getOperationDetail().replace("\"", "\"\"") : "",
                    log.getIpAddress() != null ? log.getIpAddress() : "",
                    log.getUserAgent() != null ? log.getUserAgent().replace("\"", "\"\"") : ""
            );
        }
        
        writer.flush();
    }

    @GetMapping("/system")
    public String systemInfo(Model model) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        model.addAttribute("totalMemory", totalMemory / 1024.0 / 1024.0);
        model.addAttribute("usedMemory", usedMemory / 1024.0 / 1024.0);
        model.addAttribute("freeMemory", freeMemory / 1024.0 / 1024.0);
        model.addAttribute("maxMemory", maxMemory / 1024.0 / 1024.0);
        model.addAttribute("memoryUsagePercent", (usedMemory * 100.0 / maxMemory));
        model.addAttribute("uptime", getUptime());
        model.addAttribute("javaVersion", System.getProperty("java.version"));
        model.addAttribute("osName", System.getProperty("os.name"));
        model.addAttribute("osVersion", System.getProperty("os.version"));
        model.addAttribute("availableProcessors", runtime.availableProcessors());
        
        return "admin/system";
    }

    @PostMapping("/system/config")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveConfig(@RequestParam Map<String, String> config) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "配置保存成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "配置保存失败：" + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    // 辅助方法
    private String getUptime() {
        long uptime = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
        long days = uptime / (24 * 60 * 60 * 1000);
        long hours = (uptime % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (uptime % (60 * 60 * 1000)) / (60 * 1000);
        
        return String.format("%d天 %d小时 %d分钟", days, hours, minutes);
    }
}
