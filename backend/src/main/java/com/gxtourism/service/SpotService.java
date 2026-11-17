package com.gxtourism.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gxtourism.entity.ScenicSpot;

import java.util.List;
import java.util.Map;

public interface SpotService {
    /** 分页 + 关键字 + 城市 + 等级筛选，默认按热度倒序 */
    IPage<ScenicSpot> page(long page, long size, String keyword, String city, String level);

    /** 景点详情（含在售门票列表） */
    Map<String, Object> detail(Long id);

    /** 热门推荐 topN */
    List<ScenicSpot> hot(int limit, String city);

    /** 全部城市（筛选项） */
    List<String> cities();
}
