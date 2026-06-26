package com.example.airecruitment.service;

import com.example.airecruitment.dto.ResumeRecord;
import com.example.airecruitment.parser.ParsedDocument;
import com.example.airecruitment.parser.ResumeParserService;
import com.example.airecruitment.repository.ResumeRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeApplicationService {
    private final ResumeParserService parserService;
    private final ProfileExtractionService extractionService;
    private final EmbeddingService embeddingService;
    private final ResumeRepository resumeRepository;

    public ResumeApplicationService(
            ResumeParserService parserService,
            ProfileExtractionService extractionService,
            EmbeddingService embeddingService,
            ResumeRepository resumeRepository
    ) {
        this.parserService = parserService;
        this.extractionService = extractionService;
        this.embeddingService = embeddingService;
        this.resumeRepository = resumeRepository;
    }

    public ResumeRecord upload(MultipartFile file) {
        ParsedDocument document = parserService.parse(file);
        var profile = extractionService.extractResume(document.text());
        String embeddingText = profile.summary() == null || profile.summary().isBlank() ? document.text() : profile.summary();
        double[] embedding = embeddingService.embedResume(embeddingText);
        return resumeRepository.save(profile, document.text(), embedding);
    }
}
