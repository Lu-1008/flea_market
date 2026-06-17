/**
 * 格式化日期时间
 * @param {string|number|Date} time - 时间
 * @param {string} [format='YYYY-MM-DD HH:mm:ss'] - 格式化模板
 * @returns {string} - 格式化后的时间字符串
 */
export function formatTime(time, format = 'YYYY-MM-DD HH:mm:ss') {
  if (!time) return '';
  
  let date;
  if (typeof time === 'object') {
    date = time;
  } else {
    // 处理时间戳或时间字符串
    if (typeof time === 'string') {
      if (/^\d+$/.test(time)) {
        time = parseInt(time);
      } else {
        // 处理ISO格式的日期字符串
        time = time.replace(/-/g, '/');
      }
    }
    
    // 如果是时间戳且超过2000年（946656000000），可能是毫秒，否则可能是秒
    if (typeof time === 'number' && time < 946656000000) {
      time = time * 1000;
    }
    
    date = new Date(time);
  }
  
  const year = date.getFullYear();
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const hours = date.getHours();
  const minutes = date.getMinutes();
  const seconds = date.getSeconds();
  
  const map = {
    'YYYY': year.toString(),
    'MM': month.toString().padStart(2, '0'),
    'DD': day.toString().padStart(2, '0'),
    'HH': hours.toString().padStart(2, '0'),
    'mm': minutes.toString().padStart(2, '0'),
    'ss': seconds.toString().padStart(2, '0'),
    'M': month.toString(),
    'D': day.toString(),
    'H': hours.toString(),
    'm': minutes.toString(),
    's': seconds.toString()
  };
  
  return format.replace(/(YYYY|MM|DD|HH|mm|ss|M|D|H|m|s)/g, matched => map[matched]);
}

/**
 * 格式化相对时间
 * @param {string|number|Date} time - 时间
 * @returns {string} - 相对时间描述
 */
export function formatRelativeTime(time) {
  if (!time) return '';
  
  const now = new Date();
  const date = new Date(time);
  const diff = Math.floor((now - date) / 1000); // 秒数差
  
  if (diff < 60) {
    return '刚刚';
  } else if (diff < 3600) {
    return Math.floor(diff / 60) + '分钟前';
  } else if (diff < 86400) {
    return Math.floor(diff / 3600) + '小时前';
  } else if (diff < 2592000) {
    return Math.floor(diff / 86400) + '天前';
  } else if (diff < 31536000) {
    return Math.floor(diff / 2592000) + '个月前';
  } else {
    return Math.floor(diff / 31536000) + '年前';
  }
}

/**
 * 格式化金额
 * @param {number|string} amount - 金额
 * @param {number} [decimals=2] - 保留小数位数
 * @param {boolean} [withSymbol=true] - 是否带货币符号
 * @returns {string} - 格式化后的金额
 */
export function formatAmount(amount, decimals = 2, withSymbol = true) {
  if (amount === undefined || amount === null) return '';
  
  const num = parseFloat(amount);
  if (isNaN(num)) return '';
  
  const formatted = num.toFixed(decimals);
  return withSymbol ? '¥' + formatted : formatted;
} 