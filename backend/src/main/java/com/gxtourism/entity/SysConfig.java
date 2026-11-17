package com.gxtourism.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_config")
public class SysConfig {
    @TableId("config_key")
    private String configKey;
    private String configValue;
    private String description;
    private LocalDateTime updatedAt;
}
