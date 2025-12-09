package com.example.websecurity.controller;

import com.example.websecurity.entity.Document;
import com.example.websecurity.entity.User;
import com.example.websecurity.service.DocumentService;
import com.example.websecurity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String listDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            User user = userService.findUserByUsername(auth.getName())
                    .orElse(null);

            if (user != null) {
                Pageable pageable = PageRequest.of(page, size);
                Page<Document> documents = documentService.searchDocuments(keyword, user, pageable);

                model.addAttribute("documents", documents);
                model.addAttribute("keyword", keyword);
                model.addAttribute("currentPage", page);
                model.addAttribute("totalPages", documents.getTotalPages());
            }
        }

        return "documents/list";
    }

    @GetMapping("/my")
    public String myDocuments(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        User user = userService.findUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        List<Document> documents = documentService.getUserDocuments(user.getId());
        model.addAttribute("documents", documents);
        model.addAttribute("user", user);

        return "documents/my";
    }

    @GetMapping("/public")
    public String publicDocuments(Model model) {
        List<Document> documents = documentService.getPublicDocuments();
        model.addAttribute("documents", documents);

        return "documents/public";
    }

    @GetMapping("/view/{id}")
    public String viewDocument(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = null;

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            currentUser = userService.findUserByUsername(auth.getName()).orElse(null);
        }

        Optional<Document> docOpt = documentService.findAccessibleDocument(id, currentUser);
        if (!docOpt.isPresent()) {
            try {
                return "redirect:/documents?error=" + java.net.URLEncoder.encode("文档不存在或无权限访问", StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                return "redirect:/documents?error=access_denied";
            }
        }

        documentService.incrementViewCount(id);

        Document document = docOpt.get();
        model.addAttribute("document", document);
        model.addAttribute("isOwner", currentUser != null && currentUser.getId().equals(document.getUser().getId()));
        model.addAttribute("isAdmin", currentUser != null && "ADMIN".equals(currentUser.getRole()));

        return "documents/view";
    }

    @GetMapping("/create")
    public String createDocumentForm(Model model) {
        model.addAttribute("document", new Document());
        return "documents/create";
    }

    @PostMapping("/create")
    public String createDocument(@ModelAttribute Document document,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();

            User user = userService.findUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            documentService.createDocument(
                    document.getTitle(),
                    document.getContent(),
                    document.getIsPublic(),
                    user,
                    ipAddress,
                    userAgent
            );

            redirectAttributes.addFlashAttribute("success", "文档创建成功");
            return "redirect:/documents/my";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/documents/create";
        }
    }

    @GetMapping("/edit/{id}")
    public String editDocumentForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        User currentUser = userService.findUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Optional<Document> docOpt = documentService.findDocumentById(id);
        if (!docOpt.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "文档不存在");
            return "redirect:/documents/my";
        }

        Document document = docOpt.get();

        if (!document.getUser().getId().equals(currentUser.getId()) &&
            !"ADMIN".equals(currentUser.getRole())) {
            redirectAttributes.addFlashAttribute("error", "无权限编辑此文档");
            return "redirect:/documents/my";
        }

        model.addAttribute("document", document);
        return "documents/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateDocument(@PathVariable Long id,
                                @ModelAttribute Document document,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();

            User currentUser = userService.findUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            documentService.updateDocument(
                    id,
                    document.getTitle(),
                    document.getContent(),
                    document.getIsPublic(),
                    currentUser,
                    ipAddress,
                    userAgent
            );

            redirectAttributes.addFlashAttribute("success", "文档更新成功");
            return "redirect:/documents/view/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/documents/edit/" + id;
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = null;
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            currentUser = userService.findUserByUsername(auth.getName()).orElse(null);
        }

        Optional<Document> docOpt = documentService.findAccessibleDocument(id, currentUser);
        if (!docOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Document document = docOpt.get();
        
        document.incrementDownloadCount();
        documentService.save(document);

        byte[] data = document.getContent() != null ? document.getContent().getBytes(StandardCharsets.UTF_8) : new byte[0];
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getTitle() + ".md\"")
                .contentType(MediaType.parseMediaType("text/markdown"))
                .contentLength(data.length)
                .body(resource);
    }

    @PostMapping("/delete/{id}")
    public String deleteDocument(@PathVariable Long id,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();

            User currentUser = userService.findUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            documentService.deleteDocument(id, currentUser, ipAddress, userAgent);

            redirectAttributes.addFlashAttribute("success", "文档删除成功");
            return "redirect:/documents/my";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/documents/my";
        }
    }
}