import request from './request'

/**
 * 获取用户购物车

 */
export function getCart(userId) {
  return request({
    url: `/api/cart/${userId}`,
    method: 'get'
  })
}

/**
 * 获取购物车商品数量

 */
export async function getCartItemCount(userId) {
  try {
    const res = await getCart(userId)
    if (res.code === 200 && res.data && Array.isArray(res.data.cartItems)) {
      return res.data.cartItems.length
    }
    return 0
  } catch (error) {
    console.error('获取购物车数量失败:', error)
    return 0
  }
}

/**
 * 添加商品到购物车

 */
export function addToCart(userId, itemId, quantity = 1) {
  return request({
    url: '/api/cart/add',
    method: 'post',
    params: {
      userId,
      itemId,
      quantity
    }
  })
}

/**
 * 更新购物车商品数量

 */
export function updateCartItemQuantity(cartItemId, quantity) {
  return request({
    url: `/api/cart/item/${cartItemId}`,
    method: 'put',
    params: {
      quantity
    }
  })
}

/**
 * 从购物车移除商品

 */
export function removeCartItem(cartItemId) {
  return request({
    url: `/api/cart/item/${cartItemId}`,
    method: 'delete'
  })
}

/**
 * 清空购物车

 */
export function clearCart(userId) {
  return request({
    url: `/api/cart/clear/${userId}`,
    method: 'delete'
  })
}

/**
 * 购物车结算

 */
export function checkout(userId, cartItemIds = null) {
  return request({
    url: '/api/cart/checkout',
    method: 'post',
    params: {
      userId
    },
    data: cartItemIds
  })
} 