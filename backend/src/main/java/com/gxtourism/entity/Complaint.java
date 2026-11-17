package com.gxtourism.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("complaint")
public class Complaint {
    public static final String PENDING = "PENDING";
    public static final String PROCESSING = "PROCESSING";
    public static final String RESOLVED = "RESOLVED";
    public static final String REJECTED = "REJECTED";

    private Long id;
    private Long userId;
    private String userName;
    private String orderNo;
    private String targetType;
    private String targetName;
    private String category;
    private String content;
    private String status;
    private String reply;
    private Long handlerId;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;
}
