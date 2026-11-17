package com.gxtourism.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_user")
public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String role;
    /** 0 普通用户 / 1 VIP 会员 */
    private Integer vipLevel;
    /** 会员到期时间 */
    private LocalDateTime vipExpire;
    /** 商家入驻审核：PENDING 待审 / APPROVED 通过 / REJECTED 驳回；其余角色恒为 APPROVED */
    private String auditStatus;
    /** 审核备注 */
    private String auditRemark;
    /** 商家主体 / 品牌名 */
    private String merchantName;
    /** 联系电话 */
    private String phone;
    /** 联系邮箱 */
    private String email;
    private LocalDateTime createdAt;
}
