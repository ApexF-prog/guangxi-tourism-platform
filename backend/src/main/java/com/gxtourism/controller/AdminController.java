package com.gxtourism.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gxtourism.common.BusinessException;
import com.gxtourism.common.Result;
import com.gxtourism.entity.*;
import com.gxtourism.mapper.*;
import com.gxtourism.util.Md5Util;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台管理员后台：景点、跟团游线路、店铺、用户、订单的增删改查。
 * 仅 role = ADMIN 的账号可访问，WebConfig 已挂载登录拦截器。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;
    private final ShopMapper shopMapper;
    private final ProductMapper productMapper;
    private final ScenicSpotMapper spotMapper;
    private final TourMapper tourMapper;
    private final TourBookingMapper tourBookingMapper;
    private final OrderInfoMapper orderMapper;

    // ================= 概览 =================

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(HttpServletRequest req) {
        requireAdmin(req);
        List<OrderInfo> paid = orderMapper.selectList(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getStatus, OrderInfo.PAID));
        BigDecimal turnover = paid.stream().map(o -> o.getTotal() == null ? BigDecimal.ZERO : o.getTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> m = new HashMap<>();
        m.put("spots", spotMapper.selectCount(null));
        m.put("tours", tourMapper.selectCount(null));
        m.put("shops", shopMapper.selectCount(null));
        m.put("products", productMapper.selectCount(null));
        m.put("users", userMapper.selectCount(null));
        m.put("orders", orderMapper.selectCount(null));
        m.put("paidOrders", paid.size());
        m.put("turnover", turnover);
        return Result.ok(m);
    }

    // ================= 景点 =================

    @GetMapping("/spots")
    public Result<List<ScenicSpot>> spots(@RequestParam(required = false) String keyword, HttpServletRequest req) {
        requireAdmin(req);
        return Result.ok(spotMapper.selectList(new LambdaQueryWrapper<ScenicSpot>()
                .like(keyword != null && !keyword.isBlank(), ScenicSpot::getName, keyword)
                .orderByDesc(ScenicSpot::getHot).orderByAsc(ScenicSpot::getId)));
    }

    @PostMapping("/spots")
    public Result<ScenicSpot> addSpot(@RequestBody ScenicSpot s, HttpServletRequest req) {
        requireAdmin(req);
        if (s.getName() == null || s.getName().isBlank()) throw new BusinessException("景点名称不能为空");
        if (spotMapper.selectCount(new LambdaQueryWrapper<ScenicSpot>()
                .eq(ScenicSpot::getName, s.getName())) > 0) throw new BusinessException("已存在同名景点");
        fillSpot(s);
        spotMapper.insert(s);
        return Result.ok(s);
    }

    @PutMapping("/spots/{id}")
    public Result<ScenicSpot> updateSpot(@PathVariable Long id, @RequestBody ScenicSpot body, HttpServletRequest req) {
        requireAdmin(req);
        ScenicSpot s = spotMapper.selectById(id);
        if (s == null) throw new BusinessException("景点不存在");
        if (body.getName() != null && !body.getName().equals(s.getName())
                && spotMapper.selectCount(new LambdaQueryWrapper<ScenicSpot>()
                        .eq(ScenicSpot::getName, body.getName()).ne(ScenicSpot::getId, id)) > 0) {
            throw new BusinessException("已存在同名景点");
        }
        copyIfNotNull(body.getName(), s::setName);
        copyIfNotNull(body.getCity(), s::setCity);
        copyIfNotNull(body.getLevel(), s::setLevel);
        copyIfNotNull(body.getCoverImg(), s::setCoverImg);
        if (body.getPrice() != null) s.setPrice(body.getPrice());
        if (body.getRating() != null) s.setRating(body.getRating());
        copyIfNotNull(body.getTags(), s::setTags);
        copyIfNotNull(body.getAddress(), s::setAddress);
        copyIfNotNull(body.getDescription(), s::setDescription);
        if (body.getHot() != null) s.setHot(body.getHot());
        spotMapper.updateById(s);
        return Result.ok(s);
    }

    @DeleteMapping("/spots/{id}")
    public Result<Void> deleteSpot(@PathVariable Long id, HttpServletRequest req) {
        requireAdmin(req);
        if (shopMapper.selectCount(new LambdaQueryWrapper<Shop>().eq(Shop::getSpotId, id)) > 0
                || productMapper.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getSpotId, id)) > 0) {
            throw new BusinessException("该景点下还有店铺或商品，请先转移或删除后再删景点");
        }
        spotMapper.deleteById(id);
        return Result.ok();
    }

    // ================= 跟团游线路 =================

    @GetMapping("/tours")
    public Result<List<Tour>> tours(@RequestParam(required = false) String keyword, HttpServletRequest req) {
        requireAdmin(req);
        return Result.ok(tourMapper.selectList(new LambdaQueryWrapper<Tour>()
                .like(keyword != null && !keyword.isBlank(), Tour::getTitle, keyword)
                .orderByAsc(Tour::getCity).orderByAsc(Tour::getId)));
    }

    @PostMapping("/tours")
    public Result<Tour> addTour(@RequestBody Tour t, HttpServletRequest req) {
        requireAdmin(req);
        if (t.getTitle() == null || t.getTitle().isBlank()) throw new BusinessException("线路名称不能为空");
        fillTour(t);
        tourMapper.insert(t);
        return Result.ok(t);
    }

    @PutMapping("/tours/{id}")
    public Result<Tour> updateTour(@PathVariable Long id, @RequestBody Tour body, HttpServletRequest req) {
        requireAdmin(req);
        Tour t = tourMapper.selectById(id);
        if (t == null) throw new BusinessException("线路不存在");
        copyIfNotNull(body.getTitle(), t::setTitle);
        copyIfNotNull(body.getCity(), t::setCity);
        copyIfNotNull(body.getCoverImg(), t::setCoverImg);
        copyIfNotNull(body.getDeparture(), t::setDeparture);
        copyIfNotNull(body.getHighlights(), t::setHighlights);
        copyIfNotNull(body.getItinerary(), t::setItinerary);
        if (body.getDays() != null) t.setDays(body.getDays());
        if (body.getPrice() != null) t.setPrice(body.getPrice());
        if (body.getStock() != null) t.setStock(body.getStock());
        if (body.getStatus() != null) t.setStatus(body.getStatus());
        tourMapper.updateById(t);
        return Result.ok(t);
    }

    @DeleteMapping("/tours/{id}")
    public Result<Void> deleteTour(@PathVariable Long id, HttpServletRequest req) {
        requireAdmin(req);
        if (tourBookingMapper.selectCount(new LambdaQueryWrapper<TourBooking>()
                .eq(TourBooking::getTourId, id)) > 0) {
            throw new BusinessException("该线路已有游客报名，不能删除，可改为下架");
        }
        tourMapper.deleteById(id);
        return Result.ok();
    }

    // ================= 店铺 =================

    @GetMapping("/shops")
    public Result<List<Shop>> shops(HttpServletRequest req) {
        requireAdmin(req);
        return Result.ok(shopMapper.selectList(new LambdaQueryWrapper<Shop>()
                .orderByAsc(Shop::getMerchantId).orderByAsc(Shop::getId)));
    }

    @PostMapping("/shops")
    public Result<Shop> addShop(@RequestBody Shop s, HttpServletRequest req) {
        requireAdmin(req);
        if (s.getName() == null || s.getName().isBlank()) throw new BusinessException("店铺名称不能为空");
        if (s.getMerchantId() == null) throw new BusinessException("请选择所属商家");
        if (s.getSpotId() == null) throw new BusinessException("请选择挂靠景点");
        if (shopMapper.selectCount(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getName, s.getName()).eq(Shop::getMerchantId, s.getMerchantId())) > 0) {
            throw new BusinessException("该商家已有同名店铺");
        }
        fillShop(s);
        shopMapper.insert(s);
        return Result.ok(s);
    }

    @PutMapping("/shops/{id}")
    public Result<Shop> updateShop(@PathVariable Long id, @RequestBody Shop body, HttpServletRequest req) {
        requireAdmin(req);
        Shop s = shopMapper.selectById(id);
        if (s == null) throw new BusinessException("店铺不存在");
        copyIfNotNull(body.getName(), s::setName);
        copyIfNotNull(body.getType(), s::setType);
        copyIfNotNull(body.getAddress(), s::setAddress);
        copyIfNotNull(body.getPhone(), s::setPhone);
        copyIfNotNull(body.getCoverImg(), s::setCoverImg);
        copyIfNotNull(body.getDescription(), s::setDescription);
        if (body.getMerchantId() != null) s.setMerchantId(body.getMerchantId());
        if (body.getSpotId() != null) s.setSpotId(body.getSpotId());
        if (body.getRating() != null) s.setRating(body.getRating());
        if (body.getStatus() != null) s.setStatus(body.getStatus());
        shopMapper.updateById(s);
        // 店铺换老板时，商品归属同步跟随，避免商家后台看不到自己的商品
        LambdaUpdateWrapper<Product> uw = new LambdaUpdateWrapper<>();
        uw.eq(Product::getShopId, id).set(Product::getMerchantId, s.getMerchantId());
        productMapper.update(null, uw);
        return Result.ok(s);
    }

    @DeleteMapping("/shops/{id}")
    public Result<Void> deleteShop(@PathVariable Long id, HttpServletRequest req) {
        requireAdmin(req);
        if (productMapper.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getShopId, id)) > 0) {
            throw new BusinessException("该店铺下还有商品，请先删除商品");
        }
        shopMapper.deleteById(id);
        return Result.ok();
    }

    // ================= 用户 =================

    @GetMapping("/users")
    public Result<List<User>> users(HttpServletRequest req) {
        requireAdmin(req);
        List<User> list = userMapper.selectList(new LambdaQueryWrapper<User>()
                .orderByAsc(User::getId));
        list.forEach(u -> u.setPassword(null));
        return Result.ok(list);
    }

    @PostMapping("/users")
    public Result<User> addUser(@RequestBody User body, HttpServletRequest req) {
        requireAdmin(req);
        if (body.getUsername() == null || body.getUsername().isBlank()) throw new BusinessException("账号不能为空");
        if (body.getPassword() == null || body.getPassword().isBlank()) throw new BusinessException("密码不能为空");
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, body.getUsername())) > 0) throw new BusinessException("该账号已存在");
        User u = new User();
        u.setUsername(body.getUsername());
        u.setPassword(Md5Util.md5(body.getPassword()));
        u.setNickname(body.getNickname() == null || body.getNickname().isBlank()
                ? body.getUsername() : body.getNickname());
        u.setAvatar(body.getAvatar() == null || body.getAvatar().isBlank()
                ? "/images/avatar-default.svg" : body.getAvatar());
        u.setRole(validRole(body.getRole()));
        u.setVipLevel(0);
        userMapper.insert(u);
        u.setPassword(null);
        return Result.ok(u);
    }

    @PutMapping("/users/{id}")
    public Result<User> updateUser(@PathVariable Long id, @RequestBody User body, HttpServletRequest req) {
        Long me = requireAdmin(req);
        User u = userMapper.selectById(id);
        if (u == null) throw new BusinessException("用户不存在");
        if ("ADMIN".equals(u.getRole()) && !id.equals(me)) throw new BusinessException("不能修改其他管理员");
        if (id.equals(me) && body.getRole() != null && !"ADMIN".equals(body.getRole())) {
            throw new BusinessException("不能取消自己的管理员权限");
        }
        copyIfNotNull(body.getNickname(), u::setNickname);
        copyIfNotNull(body.getAvatar(), u::setAvatar);
        if (body.getRole() != null) u.setRole(validRole(body.getRole()));
        if (body.getVipLevel() != null) u.setVipLevel(body.getVipLevel());
        if (body.getPassword() != null && !body.getPassword().isBlank()) {
            u.setPassword(Md5Util.md5(body.getPassword()));
        }
        userMapper.updateById(u);
        u.setPassword(null);
        return Result.ok(u);
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id, HttpServletRequest req) {
        Long me = requireAdmin(req);
        if (id.equals(me)) throw new BusinessException("不能删除当前登录的管理员");
        User u = userMapper.selectById(id);
        if (u == null) throw new BusinessException("用户不存在");
        if ("ADMIN".equals(u.getRole())) throw new BusinessException("不能删除管理员账号");
        if (shopMapper.selectCount(new LambdaQueryWrapper<Shop>().eq(Shop::getMerchantId, id)) > 0) {
            throw new BusinessException("该用户名下还有店铺，请先删除店铺");
        }
        userMapper.deleteById(id);
        return Result.ok();
    }

    // ================= 订单 =================

    @GetMapping("/orders")
    public Result<IPage<OrderInfo>> orders(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String status,
                                           HttpServletRequest req) {
        requireAdmin(req);
        LambdaQueryWrapper<OrderInfo> qw = new LambdaQueryWrapper<OrderInfo>()
                .eq(status != null && !status.isBlank(), OrderInfo::getStatus, status)
                .orderByDesc(OrderInfo::getCreatedAt);
        return Result.ok(orderMapper.selectPage(new Page<>(page, size), qw));
    }

    @PutMapping("/orders/{id}/status")
    public Result<OrderInfo> updateOrderStatus(@PathVariable Long id,
                                               @RequestParam String status,
                                               HttpServletRequest req) {
        requireAdmin(req);
        if (!List.of(OrderInfo.PENDING, OrderInfo.PAID, OrderInfo.CANCELED).contains(status)) {
            throw new BusinessException("订单状态不正确");
        }
        OrderInfo o = orderMapper.selectById(id);
        if (o == null) throw new BusinessException("订单不存在");
        o.setStatus(status);
        orderMapper.updateById(o);
        return Result.ok(o);
    }

    @DeleteMapping("/orders/{id}")
    public Result<Void> deleteOrder(@PathVariable Long id, HttpServletRequest req) {
        requireAdmin(req);
        orderMapper.deleteById(id);
        return Result.ok();
    }

    // ================= 私有工具 =================

    private Long requireAdmin(HttpServletRequest req) {
        Long id = (Long) req.getAttribute("uid");
        User u = id == null ? null : userMapper.selectById(id);
        if (u == null || !"ADMIN".equals(u.getRole())) {
            throw new BusinessException("仅平台管理员可操作");
        }
        return id;
    }

    private String validRole(String role) {
        if ("ADMIN".equals(role) || "MERCHANT".equals(role)) return role;
        return "CUSTOMER";
    }

    private void copyIfNotNull(String value, java.util.function.Consumer<String> setter) {
        if (value != null) setter.accept(value);
    }

    private void fillSpot(ScenicSpot s) {
        if (s.getCity() == null || s.getCity().isBlank()) s.setCity("桂林");
        if (s.getLevel() == null) s.setLevel("4A");
        if (s.getCoverImg() == null || s.getCoverImg().isBlank()) s.setCoverImg("/images/spot-guilin.jpg");
        if (s.getPrice() == null) s.setPrice(BigDecimal.ZERO);
        if (s.getRating() == null) s.setRating(new BigDecimal("4.5"));
        if (s.getTags() == null) s.setTags("");
        if (s.getAddress() == null) s.setAddress("");
        if (s.getDescription() == null) s.setDescription("");
        if (s.getHot() == null) s.setHot(0);
    }

    private void fillTour(Tour t) {
        if (t.getCity() == null || t.getCity().isBlank()) t.setCity("桂林");
        if (t.getCoverImg() == null || t.getCoverImg().isBlank()) t.setCoverImg("/images/spot-guilin.jpg");
        if (t.getDays() == null) t.setDays(1);
        if (t.getDeparture() == null || t.getDeparture().isBlank()) t.setDeparture("南宁");
        if (t.getHighlights() == null) t.setHighlights("");
        if (t.getItinerary() == null) t.setItinerary("");
        if (t.getPrice() == null) t.setPrice(BigDecimal.ZERO);
        if (t.getStock() == null) t.setStock(30);
        if (t.getStatus() == null) t.setStatus(1);
    }

    private void fillShop(Shop s) {
        if (s.getType() == null || s.getType().isBlank()) s.setType(Shop.FOOD);
        if (s.getAddress() == null) s.setAddress("");
        if (s.getPhone() == null) s.setPhone("");
        if (s.getCoverImg() == null || s.getCoverImg().isBlank()) s.setCoverImg("/images/food-zhutongfan.jpg");
        if (s.getDescription() == null) s.setDescription("");
        if (s.getRating() == null) s.setRating(new BigDecimal("4.5"));
        if (s.getStatus() == null) s.setStatus(1);
    }
}
