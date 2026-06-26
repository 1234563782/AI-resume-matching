package com.example.airecruitment.service;

import com.example.airecruitment.dto.MatchResult;
import com.example.airecruitment.dto.RecommendationResult;
import com.example.airecruitment.dto.ResumeSummary;
import com.example.airecruitment.match.MatchEngineService;
import com.example.airecruitment.repository.JobRepository;
import com.example.airecruitment.repository.MatchRepository;
import com.example.airecruitment.repository.ResumeRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    public List<RecommendationResult> recommend(long jobId, List<Long> resumeIds) {
        var job = jobRepository.findById(jobId);
        var resumes = resumeIds == null || resumeIds.isEmpty() ? resumeRepository.findAll() : resumeRepository.findByIds(resumeIds);
        if (resumeIds != null && !resumeIds.isEmpty() && resumes.size() != resumeIds.size()) {
            throw new IllegalArgumentException("部分简历 ID 不存在，请刷新简历列表后重试");
        }
        List<MatchResult> matches = resumes.stream()
                .map(resume -> matchEngineService.match(resume, job))
                .sorted(Comparator.comparingDouble(MatchResult::totalScore).reversed())
                .toList();
        matches.forEach(matchRepository::save);
        Map<Long, ResumeSummary> summaries = resumeRepository.findSummariesByIds(matches.stream().map(MatchResult::resumeId).toList()).stream()
                .collect(Collectors.toMap(ResumeSummary::id, Function.identity()));
        return matches.stream()
                .map(match -> RecommendationResult.from(match, summaries.get(match.resumeId())))
                .toList();
    }
}
