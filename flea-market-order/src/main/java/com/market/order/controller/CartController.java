package com.market.order.controller;

import com.market.common.Result;
import com.market.order.dto.CartDTO;
import com.market.order.dto.OrderDTO;
import com.market.order.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 获取用户购物车

     */
    @GetMapping("/{userId}")
    public Result<CartDTO> getCart(@PathVariable Integer userId) {
        if (userId == null) {
            return Result.fail(400, "用户ID不能为空");
        }
        
        try {
            CartDTO cartDTO = cartService.getCartByUserId(userId);
            return Result.success(cartDTO);
        } catch (Exception e) {
            return Result.fail(500, "获取购物车失败: " + e.getMessage());
        }
    }

    /**
     * 添加商品到购物车

     */
    @PostMapping("/add")
    public Result<CartDTO> addItemToCart(
            @RequestParam Integer userId,
            @RequestParam Integer itemId,
            @RequestParam(defaultValue = "1") Integer quantity) {
        
        if (userId == null) {
            return Result.fail(400, "用户ID不能为空");
        }
        
        if (itemId == null) {
            return Result.fail(400, "商品ID不能为空");
        }
        
        if (quantity <= 0) {
            return Result.fail(400, "数量必须大于0");
        }
        
        try {
            CartDTO cartDTO = cartService.addItemToCart(userId, itemId, quantity);
            return Result.success(cartDTO, "添加成功");
        } catch (Exception e) {
            return Result.fail(500, e.getMessage());
        }
    }

    /**
     * 更新购物车商品数量

     */
    @PutMapping("/item/{cartItemId}")
    public Result<CartDTO> updateCartItemQuantity(
            @PathVariable Integer cartItemId,
            @RequestParam Integer quantity) {
        
        if (cartItemId == null) {
            return Result.fail(400, "购物车项ID不能为空");
        }
        
        if (quantity <= 0) {
            return Result.fail(400, "数量必须大于0");
        }
        
        try {
            CartDTO cartDTO = cartService.updateCartItemQuantity(cartItemId, quantity);
            return Result.success(cartDTO, "更新成功");
        } catch (Exception e) {
            return Result.fail(500, e.getMessage());
        }
    }

    /**
     * 从购物车移除商品

     */
    @DeleteMapping("/item/{cartItemId}")
    public Result<CartDTO> removeCartItem(@PathVariable Integer cartItemId) {
        if (cartItemId == null) {
            return Result.fail(400, "购物车项ID不能为空");
        }
        
        try {
            CartDTO cartDTO = cartService.removeCartItem(cartItemId);
            return Result.success(cartDTO, "移除成功");
        } catch (Exception e) {
            return Result.fail(500, e.getMessage());
        }
    }

    /**
     * 清空购物车

     */
    @DeleteMapping("/clear/{userId}")
    public Result<Boolean> clearCart(@PathVariable Integer userId) {
        if (userId == null) {
            return Result.fail(400, "用户ID不能为空");
        }
        
        try {
            boolean success = cartService.clearCart(userId);
            return Result.success(success, "清空成功");
        } catch (Exception e) {
            return Result.fail(500, "清空购物车失败: " + e.getMessage());
        }
    }

    /**
     * 购物车结算

     */
    @PostMapping("/checkout")
    public Result<List<OrderDTO>> checkout(
            @RequestParam Integer userId,
            @RequestBody(required = false) List<Integer> cartItemIds) {
        
        if (userId == null) {
            return Result.fail(400, "用户ID不能为空");
        }
        
        try {
            List<OrderDTO> orders = cartService.checkout(userId, cartItemIds);
            return Result.success(orders, "结算成功，共创建" + orders.size() + "个订单");
        } catch (Exception e) {
            return Result.fail(500, "结算失败: " + e.getMessage());
        }
    }
} 