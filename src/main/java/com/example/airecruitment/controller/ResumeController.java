package com.example.airecruitment.controller;

import com.example.airecruitment.dto.OriginalResumeFile;
import com.example.airecruitment.dto.ResumeRecord;
import com.example.airecruitment.dto.ResumeSummary;
import com.example.airecruitment.service.ResumeApplicationService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {
    private final ResumeApplicationService resumeApplicationService;

    public ResumeController(ResumeApplicationService resumeApplicationService) {
        this.resumeApplicationService = resumeApplicationService;
    }

    @GetMapping
    public List<ResumeSummary> list() {
        return resumeApplicationService.listResumes();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResumeRecord upload(@RequestPart("file") MultipartFile file) {
        return resumeApplicationService.upload(file);
    }

    @GetMapping("/{resumeId}/file")
    public ResponseEntity<byte[]> openOriginalFile(@PathVariable long resumeId) {
        OriginalResumeFile file = resumeApplicationService.findOriginalFile(resumeId);
        String contentType = file.contentType() == null || file.contentType().isBlank() ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.contentType();
        String filename = file.filename() == null || file.filename().isBlank() ? "resume-" + resumeId : file.filename();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(contentType))
                .body(file.content());
    }
}
