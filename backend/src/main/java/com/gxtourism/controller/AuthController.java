package com.gxtourism.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gxtourism.common.BusinessException;
import com.gxtourism.common.Result;
import com.gxtourism.dto.LoginDTO;
import com.gxtourism.entity.Shop;
import com.gxtourism.entity.User;
import com.gxtourism.mapper.ShopMapper;
import com.gxtourism.mapper.UserMapper;
import com.gxtourism.util.JwtUtil;
import com.gxtourism.util.Md5Util;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;
    private final ShopMapper shopMapper;
    private final JwtUtil jwtUtil;

    /** 演示账号白名单：只暴露这些内置账号，密码统一为 123456（仅本地演示环境） */
    private static final List<String> DEMO_NAMES = List.of(
            "13800000001", "13911112222",
            "merchant001", "merchant002", "merchant003", "merchant004",
            "merchant005", "merchant006", "merchant007",
            "admin");

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null || !user.getPassword().equals(Md5Util.md5(dto.getPassword()))) {
            throw new BusinessException("账号或密码错误");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("token", jwtUtil.sign(user.getId(), user.getUsername()));
        user.setPassword(null);
        data.put("user", user);
        return Result.ok(data);
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody RegisterDTO dto) {
        if (dto.getUsername() == null || dto.getUsername().length() < 6) {
            throw new BusinessException("账号至少 6 位");
        }
        if (dto.getPassword() == null || dto.getPassword().length() < 6) {
            throw new BusinessException("密码至少 6 位");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername())) > 0) {
            throw new BusinessException("该账号已被注册");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(Md5Util.md5(dto.getPassword()));
        user.setNickname(dto.getNickname() == null || dto.getNickname().isBlank()
                ? "游客" + dto.getUsername().substring(0, 3) : dto.getNickname());
        // 本地默认头像，杜绝注册后头像空白
        user.setAvatar("/images/avatar-default.svg");
        // 注册只能选游客 / 商家，管理员账号只能由数据库预置，防止越权
        boolean merchant = "MERCHANT".equals(dto.getRole());
        user.setRole(merchant ? "MERCHANT" : "CUSTOMER");
        // 新入驻商家进入待审核队列，由平台管理员在「商家审核」里放行；
        // 游客一律直接通过，历史商家账号在 migrate_v8.sql 中已置为 APPROVED。
        user.setAuditStatus(merchant ? "PENDING" : "APPROVED");
        user.setAuditRemark(merchant ? "新入驻，等待平台审核" : "");
        if (merchant) {
            user.setMerchantName(user.getNickname());
            user.setPhone("");
            user.setEmail("");
        }
        userMapper.insert(user);

        Map<String, Object> data = new HashMap<>();
        data.put("token", jwtUtil.sign(user.getId(), user.getUsername()));
        user.setPassword(null);
        data.put("user", user);
        if (merchant) {
            data.put("notice", "商家账号已提交入驻申请，需平台管理员审核通过后才能进入商家后台");
        }
        return Result.ok(data);
    }

    /** 演示账号清单：登录页与商家后台「切换身份」共用，账号数据始终来自数据库 */
    @GetMapping("/demo-accounts")
    public Result<List<Map<String, Object>>> demoAccounts() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .in(User::getUsername, DEMO_NAMES)
                .orderByAsc(User::getId));
        List<Map<String, Object>> list = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> m = new HashMap<>();
            m.put("username", u.getUsername());
            m.put("nickname", u.getNickname());
            m.put("role", u.getRole());
            m.put("avatar", u.getAvatar());
            List<String> shopNames = shopMapper.selectList(new LambdaQueryWrapper<Shop>()
                            .eq(Shop::getMerchantId, u.getId())
                            .orderByAsc(Shop::getId))
                    .stream().map(Shop::getName).toList();
            m.put("shops", shopNames);
            list.add(m);
        }
        return Result.ok(list);
    }

    @Data
    public static class RegisterDTO {
        private String username;
        private String password;
        private String nickname;
        private String role;
    }
}
