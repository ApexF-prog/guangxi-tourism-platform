package com.gxtourism.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_log")
public class AdminLog {
    private Long id;
    private Long adminId;
    private String adminName;
    private String action;
    private String detail;
    private LocalDateTime createdAt;
}
