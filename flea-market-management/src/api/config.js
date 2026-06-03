/**
 * API配置文件
 */

// API基础URL
export const BASE_URL = 'http://localhost:8080';

// API路径前缀
export const API_PREFIX = '/api';

// 请求超时时间
export const TIMEOUT = 10000;

// 接口版本
export const API_VERSION = 'v1';

// 环境配置
export const ENV = {
  development: {
    baseUrl: 'http://localhost:8080',
    apiPrefix: '/api',
  },
  // test: {
  //   baseUrl:,
  //   apiPrefix:,
  // },
  // production: {
  //   baseUrl:,
  //   apiPrefix:,
  // }
};

// 当前环境
export const CURRENT_ENV = 'development';

// 获取当前环境配置
export function getEnvConfig() {
  return ENV[CURRENT_ENV];
} 