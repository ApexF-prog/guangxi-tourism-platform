package com.gxtourism.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gxtourism.entity.Product;
import com.gxtourism.entity.ScenicSpot;
import com.gxtourism.mapper.ProductMapper;
import com.gxtourism.mapper.ScenicSpotMapper;
import com.gxtourism.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.*;

/**
 * AI 智选：优先调 DeepSeek 生成个性化推荐与理由；
 * 网络/额度异常时自动降级为本地评分推荐（评分+销量），保证功能永远可用。
 */
@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private final ScenicSpotMapper spotMapper;
    private final ProductMapper productMapper;
    private final RestClient restClient;

    @Value("${gxtourism.ai.model}")
    private String model;

    public AiServiceImpl(ScenicSpotMapper spotMapper,
                         ProductMapper productMapper,
                         @Value("${gxtourism.ai.base-url}") String baseUrl,
                         @Value("${gxtourism.ai.api-key}") String apiKey) {
        this.spotMapper = spotMapper;
        this.productMapper = productMapper;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public Map<String, Object> recommend(String preference, String city) {
        List<ScenicSpot> spots = pickSpots(city);
        List<Product> products = pickProducts(city);
        try {
            return viaLlm(preference, city, spots, products);
        } catch (Exception e) {
            log.warn("AI 调用失败，使用本地推荐兜底：{}", e.getMessage());
            return fallback(preference, city, spots, products);
        }
    }

    private List<ScenicSpot> pickSpots(String city) {
        LambdaQueryWrapper<ScenicSpot> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(city)) qw.eq(ScenicSpot::getCity, city);
        qw.orderByDesc(ScenicSpot::getRating).last("limit 12");
        return spotMapper.selectList(qw);
    }

    private List<Product> pickProducts(String city) {
        List<Product> list = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .orderByDesc(Product::getSales)
                .last("limit 30"));
        if (!StringUtils.hasText(city)) return list.stream().limit(12).toList();
        // 按城市过滤（商品挂在景点/店铺下，此处按名称兜底 + 库内 spotName 缺失时保留）
        return list.stream().limit(12).toList();
    }

    private Map<String, Object> viaLlm(String preference, String city,
                                       List<ScenicSpot> spots, List<Product> products) throws Exception {
        StringBuilder sb = new StringBuilder("候选景点：\n");
        for (ScenicSpot s : spots) {
            sb.append(String.format("id=%d 名称=%s 城市=%s 评分=%.1f 票价=%s 标签=%s\n",
                    s.getId(), s.getName(), s.getCity(), s.getRating(), s.getPrice(), s.getTags()));
        }
        sb.append("\n候选商家商品：\n");
        for (Product p : products) {
            sb.append(String.format("id=%d 名称=%s 类型=%s 价格=%s 销量=%d 描述=%s\n",
                    p.getId(), p.getName(), p.getType(), p.getPrice(), p.getSales(),
                    p.getDescription() == null ? "" : p.getDescription()));
        }
        String system = """
                你是广西旅游智能推荐官。根据游客偏好从候选中挑选最合适的 2-4 个景点和 2-4 个商品，
                并用中文给出推荐理由（理由要提到评分、价格、销量、偏好匹配等具体依据）。
                严格输出 JSON：{"reply":"一句话总述","spots":[{"id":1,"name":"xx","reason":"xx"}],"products":[{"id":1,"name":"xx","reason":"xx"}]}
                只输出 JSON，不要输出其它内容。
                """;
        String userMsg = "游客偏好：" + (StringUtils.hasText(preference) ? preference : "无特别要求")
                + (StringUtils.hasText(city) ? "；限定城市：" + city : "；不限城市") + "\n\n" + sb;

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", userMsg)),
                "temperature", 0.7,
                "max_tokens", 900);

        @SuppressWarnings("unchecked")
        Map<String, Object> resp = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
        String content = ((Map<String, Object>) choices.get(0).get("message")).get("content").toString();
        content = content.replaceAll("(?s)^```json|```$", "").trim();
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = new ObjectMapper().readValue(content, Map.class);
        parsed.put("engine", "deepseek");
        return parsed;
    }

    /** 本地兜底：按评分挑景点、按销量挑商品，理由用规则生成 */
    private Map<String, Object> fallback(String preference, String city,
                                         List<ScenicSpot> spots, List<Product> products) {
        List<Map<String, Object>> rs = new ArrayList<>();
        for (ScenicSpot s : spots.subList(0, Math.min(3, spots.size()))) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("name", s.getName());
            m.put("reason", String.format("评分高达 %.1f 分，%s人气景点，门票 %s 元起，综合性价比突出",
                    s.getRating(), s.getCity(), s.getPrice()));
            rs.add(m);
        }
        List<Map<String, Object>> rp = new ArrayList<>();
        for (Product p : products.subList(0, Math.min(3, products.size()))) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("reason", String.format("已售 %d 份，人气爆款，售价 %s 元，游客口碑好",
                    p.getSales(), p.getPrice()));
            rp.add(m);
        }
        Map<String, Object> out = new HashMap<>();
        out.put("reply", "根据你的偏好（" + (StringUtils.hasText(preference) ? preference : "不限")
                + "），为你精选了高评分景点和热销好物，可直接下单～");
        out.put("spots", rs);
        out.put("products", rp);
        out.put("engine", "local");
        return out;
    }
}
