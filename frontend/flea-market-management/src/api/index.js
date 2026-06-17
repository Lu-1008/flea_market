/**
 * API接口统一出口
 */
import * as auth from './auth';
import * as dashboard from './dashboard';
import * as user from './user';

// 默认导出所有接口
export default {
  auth,
  dashboard,
  user
}; 