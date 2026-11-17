package com.gxtourism.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_browse_log")
public class BrowseLog {
    private Long id;
    private Long userId;
    private String itemType;
    private Long itemId;
    private String city;
    private LocalDateTime createdAt;
}
