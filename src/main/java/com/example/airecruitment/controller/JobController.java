package com.example.airecruitment.controller;

import com.example.airecruitment.dto.CreateJobRequest;
import com.example.airecruitment.dto.JobRecord;
import com.example.airecruitment.dto.JobSummary;
import com.example.airecruitment.service.JobApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
    private final JobApplicationService jobApplicationService;

    public JobController(JobApplicationService jobApplicationService) {
        this.jobApplicationService = jobApplicationService;
    }

    @GetMapping
    public List<JobSummary> list() {
        return jobApplicationService.listJobs();
    }

    @PostMapping
    public JobRecord create(@Valid @RequestBody CreateJobRequest request) {
        return jobApplicationService.create(request);
    }
}
