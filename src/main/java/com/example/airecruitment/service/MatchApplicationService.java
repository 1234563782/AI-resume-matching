package com.example.airecruitment.service;

import com.example.airecruitment.dto.MatchResult;
import com.example.airecruitment.match.MatchEngineService;
import com.example.airecruitment.repository.JobRepository;
import com.example.airecruitment.repository.MatchRepository;
import com.example.airecruitment.repository.ResumeRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MatchApplicationService {
    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final MatchRepository matchRepository;
    private final MatchEngineService matchEngineService;

    public MatchApplicationService(
            ResumeRepository resumeRepository,
            JobRepository jobRepository,
            MatchRepository matchRepository,
            MatchEngineService matchEngineService
    ) {
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
        this.matchRepository = matchRepository;
        this.matchEngineService = matchEngineService;
    }

    public MatchResult match(long resumeId, long jobId) {
        MatchResult result = matchEngineService.match(resumeRepository.findById(resumeId), jobRepository.findById(jobId));
        matchRepository.save(result);
        return result;
    }

    public List<MatchResult> recommend(long jobId) {
        var job = jobRepository.findById(jobId);
        List<MatchResult> results = resumeRepository.findAll().stream()
                .map(resume -> matchEngineService.match(resume, job))
                .sorted(Comparator.comparingDouble(MatchResult::totalScore).reversed())
                .toList();
        results.forEach(matchRepository::save);
        return results;
    }
}
