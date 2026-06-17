import request from './request'
import { getProductById } from './product'
/**
 * 获取订单详情

 */
export function getOrderDetail(orderId) {
  return request({
    url: `/api/order/${orderId}`,
    method: 'get'
  })
}

/**
 * 获取完整订单详情（包括商品信息）

 */
export async function getFullOrderDetail(orderId) {
  try {
    // 获取订单基本信息
    const orderRes = await getOrderDetail(orderId);
    
    if (orderRes.code !== 200) {
      return orderRes; // 如果获取订单失败，直接返回错误
    }
    
    // 获取订单商品项
    let orderItems = [];
    
    // 从订单详情中获取商品项信息
    if (orderRes.data.orderItems) {
      orderItems = Array.isArray(orderRes.data.orderItems) ? orderRes.data.orderItems : [];
    }
    
    // 补充商品详细信息
    if (orderItems.length > 0) {
      await Promise.all(orderItems.map(async (item) => {
        // 如果商品项中没有足够的商品信息，尝试获取商品详情
        if (item.itemId && (!item.itemTitle || !item.itemImageUrl)) {
          try {
            const productRes = await getProductById(item.itemId);
            if (productRes.code === 200) {
              // 填充商品信息
              item.itemTitle = item.itemTitle || productRes.data.title;
              item.itemImageUrl = item.itemImageUrl || productRes.data.itemImageUrl;
              item.price = item.price || productRes.data.price;
            }
          } catch (err) {
            console.error(`获取商品详情失败(ID: ${item.itemId}):`, err);
          }
        }
      }));
    }
    
    // 构建完整的订单详情
    const fullOrderDetail = {
      ...orderRes.data,
      orderItems: orderItems
    };
    
    return {
      code: 200,
      data: fullOrderDetail,
      message: '获取成功'
    };
  } catch (error) {
    console.error('获取完整订单详情失败:', error);
    return {
      code: 500,
      data: null,
      message: '获取订单详情失败'
    };
  }
}

/**
 * 创建订单（多商品购买）

 */
export function createOrder(data) {
  return request({
    url: '/api/order/create',
    method: 'post',
    data
  })
}

/**
 * 购买单个商品（简化接口）

 */
export function purchaseItem(buyerId, itemId, contactInfo = {}) {
  const params = {
    buyerId,
    itemId,
    contactInfo: JSON.stringify(contactInfo)
  }
  
  return request({
    url: '/api/order/purchase',
    method: 'post',
    params
  })
}

/**
 * 更新订单状态

 */
export function updateOrderStatus(orderId, status) {
  return request({
    url: `/api/order/${orderId}/status/${status}`,
    method: 'put'
  })
}

/**
 * 取消订单

 */
export function cancelOrder(orderId) {
  return updateOrderStatus(orderId, 'CANCELLED')
}

/**
 * 完成订单

 */
export function completeOrder(orderId) {
  return updateOrderStatus(orderId, 'COMPLETED')
}

/**
 * 获取当前用户订单列表

 */
export function getUserOrders(buyerId, params = {}) {
  return request({
    url: '/api/order/list',
    method: 'get',
    params: {
      ...params,
      buyerId: buyerId,
      buyerRole: params.buyerRole === undefined ? true : params.buyerRole
    }
  })
}

/**
 * 获取当前用户完整订单列表(带商品信息)

 */
export async function getFullUserOrders(buyerId, params = {}) {
  try {
    // 获取基本订单列表
    const ordersRes = await getUserOrders(buyerId, params);
    
    if (ordersRes.code !== 200 || !ordersRes.data || !ordersRes.data.list) {
      return ordersRes;
    }
    
    // 处理每个订单，获取商品信息
    const orders = ordersRes.data.list;
    const enhancedOrders = await Promise.all(orders.map(async (order) => {
      // 获取订单详情来补充商品信息
      let orderItems = [];
      
      // 如果订单已经有商品项，先使用现有的
      if (order.orderItems && Array.isArray(order.orderItems)) {
        orderItems = order.orderItems;
      } else {
        // 尝试获取详细订单信息
        try {
          const detailRes = await getOrderDetail(order.orderId);
          if (detailRes.code === 200 && detailRes.data.orderItems) {
            orderItems = Array.isArray(detailRes.data.orderItems) ? detailRes.data.orderItems : [];
          }
        } catch (error) {
          console.error(`获取订单详情失败(订单ID: ${order.orderId}):`, error);
        }
      }
      
      // 补充商品信息
      if (orderItems.length > 0) {
        await Promise.all(orderItems.map(async (item) => {
          if (item.itemId && (!item.itemTitle || !item.itemImageUrl)) {
            try {
              const productRes = await getProductById(item.itemId);
              if (productRes.code === 200) {
                // 填充商品信息
                item.itemTitle = item.itemTitle || productRes.data.title;
                item.itemImageUrl = item.itemImageUrl || productRes.data.itemImageUrl;
                item.price = item.price || productRes.data.price;
                item.quantity = item.quantity || 1;
              }
            } catch (err) {
              console.error(`获取商品详情失败(ID: ${item.itemId}):`, err);
            }
          }
        }));
      }
      
      // 返回增强的订单对象
      return {
        ...order,
        orderItems: orderItems
      };
    }));
    
    // 构建返回结果
    return {
      code: 200,
      data: {
        ...ordersRes.data,
        list: enhancedOrders
      },
      message: '获取成功'
    };
  } catch (error) {
    console.error('获取完整订单列表失败:', error);
    return {
      code: 500,
      data: null,
      message: '获取订单列表失败'
    };
  }
}

/**
 * 获取待处理订单（卖家需要发货的订单）

 */
export function getPendingOrders(params = {}) {
  // 确保参数名称与后端API匹配
  return request({
    url: '/api/order/list',
    method: 'get',
    params: {
      sellerId: params.sellerId, // 使用卖家ID查询
      page: params.pageNum || 1,
      size: params.pageSize || 10,
      status: 'PENDING' // 使用正确的订单状态枚举值
    }
  })
}

/**
 * 获取完整的待处理订单（带商品详情）

 */
export async function getFullPendingOrders(sellerId, params = {}) {
  try {
    // 获取基本待处理订单
    const ordersRes = await getPendingOrders({
      sellerId: sellerId,
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 10
    });
    
    if (ordersRes.code !== 200 || !ordersRes.data || !ordersRes.data.list) {
      return ordersRes;
    }
    
    // 处理每个订单，获取商品信息
    const orders = ordersRes.data.list;
    
    // 添加额外过滤，确保只返回当前用户作为卖家的订单
    const filteredOrders = orders.filter(order => order.sellerId === sellerId);
    
    const enhancedOrders = await Promise.all(filteredOrders.map(async (order) => {
      try {
        // 获取订单详情
        const detailRes = await getFullOrderDetail(order.orderId);
        if (detailRes.code === 200) {
          return {
            ...order,
            ...detailRes.data
          };
        }
        return order;
      } catch (error) {
        console.error(`获取订单详情失败(ID: ${order.orderId}):`, error);
        return order;
      }
    }));
    
    // 构建返回结果
    return {
      code: 200,
      data: {
        ...ordersRes.data,
        list: enhancedOrders,
        total: enhancedOrders.length // 更新总数以匹配过滤后的结果
      },
      message: '获取成功'
    };
  } catch (error) {
    console.error('获取待处理订单列表失败:', error);
    return {
      code: 500,
      data: null,
      message: '获取待处理订单列表失败'
    };
  }
}

/**
 * 卖家确认发货

 */
export function shipOrder(orderId) {
  // 后端只支持PENDING, COMPLETED, CANCELLED三种状态，没有SHIPPED
  // 因此通过更新状态为COMPLETED来实现发货功能
  return updateOrderStatus(orderId, 'COMPLETED')
}

/**
 * 删除订单

 */
export function deleteOrder(orderId) {
  return request({
    url: `/api/order/${orderId}`,
    method: 'delete'
  })
}

/**
 * 获取订单商品列表

 */
export function getOrderItems(orderId) {
  return request({
    url: `/api/order/${orderId}/items`,
    method: 'get'
  })
} 