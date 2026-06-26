package com.example.airecruitment.service;

import com.example.airecruitment.dto.OriginalResumeFile;
import com.example.airecruitment.dto.ResumeRecord;
import com.example.airecruitment.dto.ResumeSummary;
import com.example.airecruitment.parser.ParsedDocument;
import com.example.airecruitment.parser.ResumeParserService;
import com.example.airecruitment.repository.ResumeRepository;
import java.util.List;
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
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传的简历文件不能为空");
        }
        ParsedDocument document = parserService.parse(file);
        var profile = extractionService.extractResume(document.text());
        String embeddingText = profile.summary() == null || profile.summary().isBlank() ? document.text() : profile.summary();
        double[] embedding = embeddingService.embedResume(embeddingText);
        try {
            return resumeRepository.save(
                    profile,
                    document.text(),
                    embedding,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    file.getBytes()
            );
        } catch (Exception ex) {
            throw new IllegalArgumentException("保存简历文件失败：" + ex.getMessage(), ex);
        }
    }

    public List<ResumeSummary> listResumes() {
        return resumeRepository.findSummaries();
    }

    public OriginalResumeFile findOriginalFile(long resumeId) {
        return resumeRepository.findOriginalFileById(resumeId);
    }
}
