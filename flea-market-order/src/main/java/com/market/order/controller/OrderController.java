package com.market.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.common.Result;
import com.market.order.dto.OrderDTO;
import com.market.order.entity.OrderStatus;
import com.market.product.entity.ItemStatus;
import com.market.product.service.ItemService;
import com.market.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取订单列表
     * @return 订单列表和总数
     */
    @GetMapping("/list")
    public Result getOrderList(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) Integer buyerId,
            @RequestParam(required = false) String buyerName,
            @RequestParam(required = false) String sellerName,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", orderId);
        params.put("buyerId", buyerId);
        params.put("buyerName", buyerName);
        params.put("sellerName", sellerName);
        params.put("status", status);
        params.put("page", page);
        params.put("size", size);
        
        Map<String, Object> result = orderService.getOrderList(params);
        
        return Result.success(result);
    }

    /**
     * 获取订单详情
     * @return 订单详情
     */
    @GetMapping("/{orderId}")
    public Result getOrderById(@PathVariable Integer orderId) {
        OrderDTO orderDTO = orderService.getOrderById(orderId);
        if (orderDTO == null) {
            return Result.fail(404, "订单不存在");
        }
        return Result.success(orderDTO);
    }

    /**
     * 更新订单状态

     * @return 更新结果
     */
    @PutMapping("/{orderId}/status/{status}")
    public Result updateOrderStatus(@PathVariable Integer orderId, @PathVariable String status) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status);
            boolean success = orderService.updateOrderStatus(orderId, orderStatus);
            
            if (!success) {
                return Result.fail(404, "订单不存在");
            }
            
            // 如果订单状态变为已取消，则恢复商品状态为在售
            if (orderStatus == OrderStatus.CANCELLED) {
                // 查询订单商品项
                OrderDTO orderDTO = orderService.getOrderById(orderId);
                if (orderDTO != null && orderDTO.getOrderItems() != null) {
                    for (OrderDTO.OrderItemDTO item : orderDTO.getOrderItems()) {
                        try {
                            // 恢复商品状态为ACTIVE
                            itemService.updateItemStatus(item.getItemId(), ItemStatus.ACTIVE.toString());
                        } catch (Exception e) {
                            // 记录错误但不影响主流程
                            System.err.println("恢复商品状态失败: " + e.getMessage());
                        }
                    }
                }
            }
            
            return Result.success(true, "更新状态成功");
        } catch (IllegalArgumentException e) {
            return Result.fail(400, "无效的订单状态");
        }
    }

    /**
     * 删除订单

     * @return 删除结果
     */
    @DeleteMapping("/{orderId}")
    public Result deleteOrder(@PathVariable Integer orderId) {
        boolean success = orderService.deleteOrder(orderId);
        
        if (!success) {
            return Result.fail(404, "订单不存在");
        }
        
        return Result.success(true, "删除成功");
    }

    /**
     * 批量删除订单

     * @return 删除结果
     */
    @DeleteMapping("/batch")
    public Result batchDeleteOrders(@RequestBody List<Integer> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Result.fail(400, "缺少订单ID列表");
        }
        
        Map<String, List<Integer>> result = orderService.batchDeleteOrders(orderIds);
        return Result.success(result);
    }

    /**
     * 创建订单（购买商品）

     * @return 创建的订单
     */
    @PostMapping("/create")
    public Result<OrderDTO> createOrder(@RequestBody OrderDTO orderDTO) {
        if (orderDTO.getBuyerId() == null) {
            return Result.fail(400, "买家ID不能为空");
        }
        
        if (orderDTO.getSellerId() == null) {
            return Result.fail(400, "卖家ID不能为空");
        }
        
        if (orderDTO.getOrderItems() == null || orderDTO.getOrderItems().isEmpty()) {
            return Result.fail(400, "订单商品不能为空");
        }
        
        try {
            OrderDTO createdOrder = orderService.createOrder(orderDTO);
            return Result.success(createdOrder, "订单创建成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(500, "创建订单失败: " + e.getMessage());
        }
    }
    
    /**
     * 购买单个商品的便捷接口

     * @return 创建的订单
     */
    @PostMapping("/purchase")
    public Result<OrderDTO> purchaseItem(
            @RequestParam Integer buyerId,
            @RequestParam Integer itemId,
            @RequestParam(required = false) String contactInfo) {
        
        if (buyerId == null) {
            return Result.fail(400, "买家ID不能为空");
        }
        
        if (itemId == null) {
            return Result.fail(400, "商品ID不能为空");
        }
        
        try {
            // 查询商品信息
            com.market.product.entity.Item item = itemService.findById(itemId);
            if (item == null) {
                return Result.fail(404, "商品不存在");
            }
            
            // 检查商品状态
            if (!item.getStatus().equals("ACTIVE")) {
                return Result.fail(400, "商品不可购买，当前状态: " + item.getStatus());
            }
            
            // 检查不能购买自己的商品
            if (buyerId.equals(item.getUserId())) {
                return Result.fail(400, "不能购买自己的商品");
            }
            
            // 创建订单DTO
            OrderDTO orderDTO = new OrderDTO();
            orderDTO.setBuyerId(buyerId);
            orderDTO.setSellerId(item.getUserId());
            orderDTO.setTotalAmount(item.getPrice());
            orderDTO.setStatus(OrderStatus.PENDING);
            
            // 处理联系信息
            Map<String, Object> contactMap = new HashMap<>();
            if (contactInfo != null && !contactInfo.isEmpty()) {
                try {
                    contactMap = objectMapper.readValue(contactInfo, Map.class);
                } catch (Exception e) {
                    return Result.fail(400, "联系信息格式不正确");
                }
            }
            orderDTO.setContactInfo(contactMap);
            
            // 创建订单商品
            List<OrderDTO.OrderItemDTO> items = new ArrayList<>();
            OrderDTO.OrderItemDTO orderItemDTO = new OrderDTO.OrderItemDTO();
            orderItemDTO.setItemId(itemId);
            orderItemDTO.setPrice(item.getPrice());
            orderItemDTO.setQuantity(1);
            items.add(orderItemDTO);
            orderDTO.setOrderItems(items);
            
            // 创建订单
            OrderDTO createdOrder = orderService.createOrder(orderDTO);
            return Result.success(createdOrder, "商品购买成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(500, "购买商品失败: " + e.getMessage());
        }
    }
} 