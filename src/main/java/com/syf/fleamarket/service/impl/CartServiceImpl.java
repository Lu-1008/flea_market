package com.syf.fleamarket.service.impl;

import com.syf.fleamarket.dao.CartMapper;
import com.syf.fleamarket.dao.ItemMapper;
import com.syf.fleamarket.dto.CartDTO;
import com.syf.fleamarket.dto.OrderDTO;
import com.syf.fleamarket.entity.Cart;
import com.syf.fleamarket.entity.CartItem;
import com.syf.fleamarket.entity.Item;
import com.syf.fleamarket.entity.User;
import com.syf.fleamarket.entity.enums.OrderStatus;
import com.syf.fleamarket.entity.enums.UserRole;
import com.syf.fleamarket.service.CartService;
import com.syf.fleamarket.service.OrderService;
import com.syf.fleamarket.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    @Autowired
    private CartMapper cartMapper;
    
    @Autowired
    private ItemMapper itemMapper;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private UserService userService;

    @Override
    public CartDTO getCartByUserId(Integer userId) {
        try {
            // 查询用户购物车
            Cart cart = cartMapper.getCartByUserId(userId);
            
            // 如果购物车不存在，检查用户是否存在并创建新购物车
            if (cart == null) {
                // 检查用户是否存在
                User user = userService.findById(userId);
                if (user == null) {
                    log.error("用户不存在，无法创建购物车: userId={}", userId);
                    throw new IllegalArgumentException("用户不存在");
                }
                
                // 为用户创建购物车（不考虑用户角色）
                cart = new Cart();
                cart.setUserId(userId);
                cart.setCreatedAt(LocalDateTime.now());
                cartMapper.createCart(cart);
                
                // 返回空购物车
                CartDTO cartDTO = new CartDTO();
                cartDTO.setCartId(cart.getCartId());
                cartDTO.setUserId(userId);
                cartDTO.setCreatedAt(cart.getCreatedAt());
                cartDTO.setTotalAmount(BigDecimal.ZERO);
                cartDTO.setCartItems(Collections.emptyList());
                return cartDTO;
            }
            
            // 查询购物车中的商品
            List<Map<String, Object>> cartItemsMap = cartMapper.getCartItems(cart.getCartId());
            List<CartDTO.CartItemDTO> cartItems = new ArrayList<>();
            AtomicReference<BigDecimal> totalAmount = new AtomicReference<>(BigDecimal.ZERO);
            
            // 转换购物车商品数据
            if (cartItemsMap != null && !cartItemsMap.isEmpty()) {
                cartItems = cartItemsMap.stream().map(item -> {
                    CartDTO.CartItemDTO cartItemDTO = new CartDTO.CartItemDTO();
                    try {
                        cartItemDTO.setCartItemId((Integer) item.get("cartItemId"));
                        cartItemDTO.setCartId((Integer) item.get("cartId"));
                        cartItemDTO.setItemId((Integer) item.get("itemId"));
                        cartItemDTO.setItemTitle((String) item.get("itemTitle"));
                        cartItemDTO.setItemImage((String) item.get("itemImageUrl"));
                        cartItemDTO.setPrice((BigDecimal) item.get("price"));
                        cartItemDTO.setQuantity((Integer) item.get("quantity"));
                        cartItemDTO.setSellerId((Integer) item.get("sellerId"));
                        cartItemDTO.setSellerName((String) item.get("sellerName"));
                        
                        // 处理添加时间
                        Object addedAtObj = item.get("addedAt");
                        if (addedAtObj instanceof java.sql.Timestamp) {
                            cartItemDTO.setAddedAt(((java.sql.Timestamp) addedAtObj).toLocalDateTime());
                        } else {
                            cartItemDTO.setAddedAt(LocalDateTime.now());
                        }
                        
                        // 累加总金额
                        BigDecimal itemTotal = cartItemDTO.getPrice().multiply(new BigDecimal(cartItemDTO.getQuantity()));
                        totalAmount.set(totalAmount.get().add(itemTotal));
                    } catch (Exception e) {
                        log.error("转换购物车商品时出错: {}", e.getMessage());
                    }
                    return cartItemDTO;
                }).collect(Collectors.toList());
            }
            
            // 构建购物车DTO
            CartDTO cartDTO = new CartDTO();
            cartDTO.setCartId(cart.getCartId());
            cartDTO.setUserId(userId);
            cartDTO.setCreatedAt(cart.getCreatedAt());
            cartDTO.setTotalAmount(totalAmount.get());
            cartDTO.setCartItems(cartItems);
            
            return cartDTO;
        } catch (Exception e) {
            log.error("获取购物车失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional
    public CartDTO addItemToCart(Integer userId, Integer itemId, Integer quantity) {
        try {
            // 检查用户是否存在
            User user = userService.findById(userId);
            if (user == null) {
                log.error("用户不存在，无法添加商品到购物车: userId={}", userId);
                throw new IllegalArgumentException("用户不存在");
            }
            
            // 检查商品是否存在
            Item item = itemMapper.findById(itemId);
            if (item == null) {
                throw new IllegalArgumentException("商品不存在");
            }
            
            // 检查商品状态
            if (!"ACTIVE".equals(item.getStatus())) {
                throw new IllegalArgumentException("商品不可购买，当前状态: " + item.getStatus());
            }
            
            // 检查不能添加自己的商品到购物车
            if (userId.equals(item.getUserId())) {
                throw new IllegalArgumentException("不能添加自己的商品到购物车");
            }
            
            // 获取用户购物车，如果不存在则创建
            Cart cart = cartMapper.getCartByUserId(userId);
            if (cart == null) {
                cart = new Cart();
                cart.setUserId(userId);
                cart.setCreatedAt(LocalDateTime.now());
                cartMapper.createCart(cart);
            }
            
            // 检查购物车中是否已有该商品
            CartItem existingItem = cartMapper.getCartItemByItemId(cart.getCartId(), itemId);
            if (existingItem != null) {
                // 如果已存在，更新数量
                int newQuantity = existingItem.getQuantity() + quantity;
                cartMapper.updateCartItemQuantity(existingItem.getCartItemId(), newQuantity);
            } else {
                // 如果不存在，添加新商品
                CartItem cartItem = new CartItem();
                cartItem.setCartId(cart.getCartId());
                cartItem.setItemId(itemId);
                cartItem.setQuantity(quantity);
                cartItem.setAddedAt(LocalDateTime.now());
                cartMapper.addItemToCart(cartItem);
            }
            
            // 返回更新后的购物车
            return getCartByUserId(userId);
        } catch (Exception e) {
            log.error("添加商品到购物车失败: {}", e.getMessage());
            throw new RuntimeException("添加商品到购物车失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public CartDTO updateCartItemQuantity(Integer cartItemId, Integer quantity) {
        try {
            // 获取购物车项
            CartItem cartItem = cartMapper.getCartItemById(cartItemId);
            if (cartItem == null) {
                throw new IllegalArgumentException("购物车商品不存在");
            }
            
            // 更新数量
            cartMapper.updateCartItemQuantity(cartItemId, quantity);
            
            // 获取购物车
            Cart cart = cartMapper.getCartById(cartItem.getCartId()); // 修正这里：通过cartItem的cartId获取购物车
            
            // 返回更新后的购物车
            return getCartByUserId(cart.getUserId());
        } catch (Exception e) {
            log.error("更新购物车商品数量失败: {}", e.getMessage());
            throw new RuntimeException("更新购物车商品数量失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public CartDTO removeCartItem(Integer cartItemId) {
        try {
            // 获取购物车项
            CartItem cartItem = cartMapper.getCartItemById(cartItemId);
            if (cartItem == null) {
                throw new IllegalArgumentException("购物车商品不存在");
            }
            
            // 获取cart和userId
            Integer cartId = cartItem.getCartId();
            
            // 先通过cartId查询cart
            Cart cart = null;
            
            // 获取所有购物车项以找出拥有这个cartId的cart
            List<Cart> carts = cartMapper.getAllCarts();
            for (Cart c : carts) {
                if (c.getCartId().equals(cartId)) {
                    cart = c;
                    break;
                }
            }
            
            if (cart == null) {
                throw new IllegalArgumentException("购物车不存在");
            }
            
            Integer userId = cart.getUserId();
            
            // 删除购物车项
            cartMapper.removeCartItem(cartItemId);
            
            // 返回更新后的购物车
            return getCartByUserId(userId);
        } catch (Exception e) {
            log.error("从购物车移除商品失败: {}", e.getMessage());
            throw new RuntimeException("从购物车移除商品失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean clearCart(Integer userId) {
        try {
            // 获取用户购物车
            Cart cart = cartMapper.getCartByUserId(userId);
            if (cart == null) {
                return true; // 购物车不存在，视为已清空
            }
            
            // 清空购物车
            return cartMapper.clearCart(cart.getCartId()) > 0;
        } catch (Exception e) {
            log.error("清空购物车失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public List<OrderDTO> checkout(Integer userId, List<Integer> cartItemIds) {
        try {
            // 获取用户购物车
            Cart cart = cartMapper.getCartByUserId(userId);
            if (cart == null) {
                throw new IllegalArgumentException("购物车不存在");
            }
            
            // 获取要结算的购物车商品
            List<Map<String, Object>> cartItems;
            if (cartItemIds != null && !cartItemIds.isEmpty()) {
                cartItems = cartMapper.getCartItemsByIds(cartItemIds);
            } else {
                cartItems = cartMapper.getCartItems(cart.getCartId());
            }
            
            if (cartItems == null || cartItems.isEmpty()) {
                throw new IllegalArgumentException("购物车为空或选择的商品不存在");
            }
            
            // 按卖家分组商品，每个卖家创建一个订单
            Map<Integer, List<Map<String, Object>>> sellerItemsMap = new HashMap<>();
            for (Map<String, Object> item : cartItems) {
                Integer sellerId = (Integer) item.get("sellerId");
                if (!sellerItemsMap.containsKey(sellerId)) {
                    sellerItemsMap.put(sellerId, new ArrayList<>());
                }
                sellerItemsMap.get(sellerId).add(item);
            }
            
            // 创建订单列表
            List<OrderDTO> orders = new ArrayList<>();
            
            // 为每个卖家创建订单
            for (Map.Entry<Integer, List<Map<String, Object>>> entry : sellerItemsMap.entrySet()) {
                Integer sellerId = entry.getKey();
                List<Map<String, Object>> sellerItems = entry.getValue();
                
                // 创建订单DTO
                OrderDTO orderDTO = new OrderDTO();
                orderDTO.setBuyerId(userId);
                orderDTO.setSellerId(sellerId);
                orderDTO.setStatus(OrderStatus.PENDING);
                
                // 创建订单商品
                List<OrderDTO.OrderItemDTO> orderItems = new ArrayList<>();
                BigDecimal totalAmount = BigDecimal.ZERO;
                
                for (Map<String, Object> item : sellerItems) {
                    OrderDTO.OrderItemDTO orderItemDTO = new OrderDTO.OrderItemDTO();
                    orderItemDTO.setItemId((Integer) item.get("itemId"));
                    orderItemDTO.setPrice((BigDecimal) item.get("price"));
                    orderItemDTO.setQuantity((Integer) item.get("quantity"));
                    orderItems.add(orderItemDTO);
                    
                    // 累加订单总金额
                    BigDecimal itemTotal = orderItemDTO.getPrice().multiply(new BigDecimal(orderItemDTO.getQuantity()));
                    totalAmount = totalAmount.add(itemTotal);
                    
                    // 从购物车中移除已结算的商品
                    cartMapper.removeCartItem((Integer) item.get("cartItemId"));
                }
                
                orderDTO.setOrderItems(orderItems);
                orderDTO.setTotalAmount(totalAmount);
                
                // 创建订单
                OrderDTO createdOrder = orderService.createOrder(orderDTO);
                orders.add(createdOrder);
            }
            
            return orders;
        } catch (Exception e) {
            log.error("购物车结算失败: {}", e.getMessage());
            throw new RuntimeException("购物车结算失败: " + e.getMessage());
        }
    }
} 