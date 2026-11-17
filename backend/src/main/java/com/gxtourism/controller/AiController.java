package com.gxtourism.controller;

import com.gxtourism.common.Result;
import com.gxtourism.service.AiService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /** AI 智选：?preference=偏好&city=城市（可不传） */
    @PostMapping("/recommend")
    public Result<Map<String, Object>> recommend(@RequestBody RecommendDTO dto) {
        return Result.ok(aiService.recommend(dto.getPreference(), dto.getCity()));
    }

    @Data
    public static class RecommendDTO {
        private String preference;
        private String city;
    }
}
