package com.example.airecruitment.controller;

import com.example.airecruitment.dto.MatchResult;
import com.example.airecruitment.service.MatchApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
public class MatchController {
    private final MatchApplicationService matchApplicationService;

    public MatchController(MatchApplicationService matchApplicationService) {
        this.matchApplicationService = matchApplicationService;
    }

    @PostMapping
    public MatchResult match(@RequestParam long resumeId, @RequestParam long jobId) {
        return matchApplicationService.match(resumeId, jobId);
    }

    @GetMapping("/jobs/{jobId}/recommendations")
    public List<MatchResult> recommend(@PathVariable long jobId) {
        return matchApplicationService.recommend(jobId);
    }
}
