package com.example.websecurity.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.example.websecurity.entity.User;
import com.example.websecurity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private UserService userService;

    @GetMapping("/public/users")
    public ResponseEntity<?> getUsers() {
        List<User> users = userService.getAllUsers();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", users);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/public/parse")
    public ResponseEntity<?> parseJson(@RequestBody String jsonData) {
        try {
            System.out.println("=== FASTJSON VULNERABILITY DEBUG ===");
            System.out.println("Input JSON: " + jsonData);
            Object parsed = JSON.parseObject(jsonData, Feature.SupportNonPublicField);
            
            System.out.println("Parsed object: " + parsed);
            System.out.println("Parsed object type: " + parsed.getClass().getName());
            
            if (parsed instanceof Map) {
                Map<String, Object> parsedMap = (Map<String, Object>) parsed;
                System.out.println("Parsed as Map with keys: " + parsedMap.keySet());
                
                for (Map.Entry<String, Object> entry : parsedMap.entrySet()) {
                    System.out.println("Processing key: " + entry.getKey() + ", value: " + entry.getValue());
                    if (entry.getValue() != null) {
                        System.out.println("Value type: " + entry.getValue().getClass().getName());
                    }
                }
            }
            
            System.out.println("=== END DEBUG ===");
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("parsed", parsed);
            response.put("type", parsed.getClass().getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("FASTJSON ERROR: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/public/parseObject")
    public ResponseEntity<?> parseJsonObject(@RequestBody String jsonData) {
        try {
            JSONObject parsed = JSON.parseObject(jsonData, Feature.SupportNonPublicField);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("parsed", parsed);
            response.put("keys", parsed.keySet());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/public/userInfo")
    public ResponseEntity<?> getUserInfo(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            if (username == null || username.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "用户名不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            Optional<User> userOpt = userService.findUserByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Map<String, Object> userData = new HashMap<>();
                userData.put("id", user.getId());
                userData.put("username", user.getUsername());
                userData.put("email", user.getEmail());
                userData.put("fullName", user.getFullName());
                userData.put("role", user.getRole());
                userData.put("createdAt", user.getCreatedAt());

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("user", userData);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "用户不存在");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/documents/stats")
    public ResponseEntity<?> getDocumentStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalDocuments", 1250);
            stats.put("activeUsers", 342);
            stats.put("todayUploads", 48);
            stats.put("storageUsed", "2.4GB");
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/documents/import")
    public ResponseEntity<?> importDocument(@RequestBody String jsonData) {
        try {
            JSONObject docData = JSON.parseObject(jsonData, Feature.SupportNonPublicField);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("documentId", "doc_" + System.currentTimeMillis());
            response.put("title", docData.getString("title"));
            response.put("parsedType", docData.getClass().getName());
            response.put("message", "文档导入成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "文档导入失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
