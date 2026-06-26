package com.example.airecruitment.controller;

import com.example.airecruitment.dto.ResumeRecord;
import com.example.airecruitment.service.ResumeApplicationService;
import org.springframework.http.MediaType;
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResumeRecord upload(@RequestPart("file") MultipartFile file) {
        return resumeApplicationService.upload(file);
    }
}
