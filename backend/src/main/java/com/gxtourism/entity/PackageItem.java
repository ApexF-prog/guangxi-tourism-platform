package com.gxtourism.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("package_item")
public class PackageItem {
    private Long id;
    private Long packageId;
    private Long productId;
    private Integer quantity;
}
