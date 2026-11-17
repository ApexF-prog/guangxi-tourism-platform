package com.gxtourism.service;

import java.util.Map;

public interface AiService {

    /**
     * AI 智选：根据游客偏好（预算/口味/玩法/城市）推荐景点与商家商品。
     * 返回 { reply: 总述, spots: [{id, name, reason}], products: [{id, name, reason}] }
     */
    Map<String, Object> recommend(String preference, String city);
}
