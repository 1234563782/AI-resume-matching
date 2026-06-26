package com.example.airecruitment.service;

import com.example.airecruitment.dto.CreateJobRequest;
import com.example.airecruitment.dto.JobRecord;
import com.example.airecruitment.repository.JobRepository;
import org.springframework.stereotype.Service;

@Service
public class JobApplicationService {
    private final ProfileExtractionService extractionService;
    private final EmbeddingService embeddingService;
    private final JobRepository jobRepository;

    public JobApplicationService(
            ProfileExtractionService extractionService,
            EmbeddingService embeddingService,
            JobRepository jobRepository
    ) {
        this.extractionService = extractionService;
        this.embeddingService = embeddingService;
        this.jobRepository = jobRepository;
    }

    public JobRecord create(CreateJobRequest request) {
        var profile = extractionService.extractJob(request.title(), request.jdText());
        String embeddingText = profile.summary() == null || profile.summary().isBlank() ? request.jdText() : profile.summary();
        double[] embedding = embeddingService.embedJob(embeddingText);
        return jobRepository.save(profile, request.title(), request.jdText(), embedding);
    }
}
