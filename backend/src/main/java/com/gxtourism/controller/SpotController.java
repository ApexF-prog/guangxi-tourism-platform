package com.gxtourism.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gxtourism.common.Result;
import com.gxtourism.entity.ScenicSpot;
import com.gxtourism.service.SpotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/spot")
@RequiredArgsConstructor
public class SpotController {

    private final SpotService spotService;

    /** 分页列表：?page=&size=&keyword=&city=&level= */
    @GetMapping("/page")
    public Result<IPage<ScenicSpot>> page(@RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "9") long size,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String city,
                                          @RequestParam(required = false) String level) {
        return Result.ok(spotService.page(page, size, keyword, city, level));
    }

    /** 景点详情（含在售门票） */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(spotService.detail(id));
    }

    /** 热门推荐 */
    @GetMapping("/hot")
    public Result<List<ScenicSpot>> hot(@RequestParam(defaultValue = "6") int limit,
                                        @RequestParam(required = false) String city) {
        return Result.ok(spotService.hot(limit, city));
    }

    /** 城市筛选项 */
    @GetMapping("/cities")
    public Result<List<String>> cities() {
        return Result.ok(spotService.cities());
    }
}
