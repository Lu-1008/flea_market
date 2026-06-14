package com.market.user.controller;

import com.market.common.ApiResponse;
import com.market.user.JwtUtil;
import com.market.user.entity.User;
import com.market.user.entity.UserRole;
import com.market.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    public ApiResponse<User> getCurrentUserInfo(HttpServletRequest request) {
        try {
            // 从请求头获取token
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            } else {
                log.warn("获取用户信息失败 - 无效的token格式");
                return ApiResponse.error(401, "请先登录");
            }
            
            // 验证token并获取用户ID
            Integer userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                log.warn("获取用户信息失败 - 无法从token中解析用户ID");
                return ApiResponse.error(401, "登录已过期，请重新登录");
            }
            
            // 查询用户信息
            User user = userService.findById(userId);
            if (user == null) {
                log.warn("获取用户信息失败 - 用户不存在: ID={}", userId);
                return ApiResponse.error(404, "用户不存在");
            }
            
            // 安全起见，不返回密码
            user.setPassword(null);
            log.debug("获取当前用户信息成功 - ID: {}, 用户名: {}", user.getUserId(), user.getUsername());
            return ApiResponse.success("获取用户信息成功", user);
        } catch (Exception e) {
            log.error("获取当前用户信息失败", e);
            return ApiResponse.error(500, "获取用户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户列表（分页）
     */
    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getUserList(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        log.debug("获取用户列表请求 - 参数: username={}, role={}, page={}, size={}", 
                username, role, page, size);
        
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("username", username);
            params.put("role", role);
            params.put("page", page);
            params.put("size", size);
            
            Map<String, Object> result = userService.getUserList(params);
            log.debug("获取用户列表成功 - 总数: {}, 列表大小: {}", 
                    result.get("total"), ((List)result.get("list")).size());
            return ApiResponse.success("获取用户列表成功", result);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return ApiResponse.error(500, "获取用户列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户详情
     */
    @GetMapping("/{userId}")
    public ApiResponse<User> getUserById(@PathVariable Integer userId) {
        try {
            log.debug("获取用户详情 - ID: {}", userId);
            
            User user = userService.findById(userId);
            if (user == null) {
                log.warn("用户不存在 - ID: {}", userId);
                return ApiResponse.error(404, "用户不存在");
            }
            
            // 安全起见，不返回密码
            user.setPassword(null);
            return ApiResponse.success("获取用户信息成功", user);
        } catch (Exception e) {
            log.error("获取用户详情失败", e);
            return ApiResponse.error(500, "获取用户详情失败: " + e.getMessage());
        }
    }

    /**
     * 用户端注册接口
     */
    @PostMapping("/register")
    public ApiResponse<User> register(@RequestBody User user) {
        try {
            log.debug("用户端注册请求 - 用户名: {}, 邮箱: {}", user.getUsername(), user.getEmail());
            
            // 验证必要字段
            if (user.getUsername() == null || user.getUsername().isEmpty()) {
                log.warn("用户注册失败 - 用户名为空");
                return ApiResponse.error(400, "用户名不能为空");
            }
            
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                log.warn("用户注册失败 - 密码为空");
                return ApiResponse.error(400, "密码不能为空");
            }
            
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                log.warn("用户注册失败 - 邮箱为空");
                return ApiResponse.error(400, "邮箱不能为空");
            }
            
            // 强制设置为普通用户角色
            user.setRole(UserRole.USER);
            
            // 设置默认头像
            if (user.getUserImgUrl() == null || user.getUserImgUrl().isEmpty()) {
                user.setUserImgUrl("user-default.png");
            }
            
            User createdUser = userService.createUser(user);
            // 安全起见，不返回密码
            createdUser.setPassword(null);
            log.info("用户注册成功 - ID: {}, 用户名: {}", createdUser.getUserId(), createdUser.getUsername());
            return ApiResponse.success("注册成功", createdUser);
        } catch (RuntimeException e) {
            log.error("用户注册失败 - {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("用户注册失败", e);
            return ApiResponse.error(500, "注册失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建用户
     */
    @PostMapping("/create")
    public ApiResponse<User> createUser(@RequestBody User user) {
        try {
            log.debug("创建用户请求 - 用户名: {}, 邮箱: {}", user.getUsername(), user.getEmail());
            
            // 验证必要字段
            if (user.getUsername() == null || user.getUsername().isEmpty()) {
                log.warn("创建用户失败 - 用户名为空");
                return ApiResponse.error(400, "用户名不能为空");
            }
            
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                log.warn("创建用户失败 - 密码为空");
                return ApiResponse.error(400, "密码不能为空");
            }
            
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                log.warn("创建用户失败 - 邮箱为空");
                return ApiResponse.error(400, "邮箱不能为空");
            }
            
            // 处理前端可能提交的字符串角色
            if (user.getRole() == null) {
                // 默认设置为普通用户
                user.setRole(UserRole.USER);
            } else if (!(user.getRole() instanceof UserRole)) {
                // 如果不是枚举类型但有值，尝试转换
                try {
                    String roleStr = user.getRole().toString();
                    user.setRole(UserRole.valueOf(roleStr));
                } catch (Exception e) {
                    log.warn("无效的用户角色: {}, 将设置为默认角色USER", user.getRole());
                    user.setRole(UserRole.USER);
                }
            }
            
            // 设置默认头像
            if (user.getUserImgUrl() == null || user.getUserImgUrl().isEmpty()) {
                user.setUserImgUrl("user-default.png");
            }
            
            User createdUser = userService.createUser(user);
            // 安全起见，不返回密码
            createdUser.setPassword(null);
            log.info("创建用户成功 - ID: {}, 用户名: {}", createdUser.getUserId(), createdUser.getUsername());
            return ApiResponse.success("创建用户成功", createdUser);
        } catch (RuntimeException e) {
            log.error("创建用户失败 - {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("创建用户失败", e);
            return ApiResponse.error(500, "创建用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新用户
     */
    @PutMapping("/update")
    public ApiResponse<User> updateUser(@RequestBody User user) {
        try {
            log.debug("更新用户请求 - ID: {}, 用户名: {}", user.getUserId(), user.getUsername());
            
            // 验证必要字段
            if (user.getUserId() == null) {
                log.warn("更新用户失败 - 用户ID为空");
                return ApiResponse.error(400, "用户ID不能为空");
            }
            
            if (user.getUsername() == null || user.getUsername().isEmpty()) {
                log.warn("更新用户失败 - 用户名为空");
                return ApiResponse.error(400, "用户名不能为空");
            }
            
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                log.warn("更新用户失败 - 邮箱为空");
                return ApiResponse.error(400, "邮箱不能为空");
            }
            
            // 处理前端可能提交的字符串角色
            if (user.getRole() != null && !(user.getRole() instanceof UserRole)) {
                // 如果不是枚举类型但有值，尝试转换
                try {
                    String roleStr = user.getRole().toString();
                    user.setRole(UserRole.valueOf(roleStr));
                } catch (Exception e) {
                    log.warn("无效的用户角色: {}, 将由服务层处理", user.getRole());
                    // 保留原值，服务层会处理
                    user.setRole(null);
                }
            }
            
            // 获取现有用户信息
            User existingUser = userService.findById(user.getUserId());
            if (existingUser == null) {
                log.warn("更新用户失败 - 用户不存在: {}", user.getUserId());
                return ApiResponse.error(404, "用户不存在");
            }
            
            User updatedUser = userService.updateUser(user);
            if (updatedUser == null) {
                return ApiResponse.error(500, "更新用户失败");
            }
            
            // 安全起见，不返回密码
            updatedUser.setPassword(null);
            log.info("更新用户成功 - ID: {}, 用户名: {}", updatedUser.getUserId(), updatedUser.getUsername());
            return ApiResponse.success("更新用户成功", updatedUser);
        } catch (RuntimeException e) {
            log.error("更新用户失败 - {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("更新用户失败", e);
            return ApiResponse.error(500, "更新用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{userId}")
    public ApiResponse<Boolean> deleteUser(@PathVariable Integer userId) {
        try {
            log.debug("删除用户请求 - ID: {}", userId);
            
            if (userId == 1) {
                log.warn("尝试删除超级管理员 (ID=1), 操作被拒绝");
                return ApiResponse.error(403, "不能删除超级管理员");
            }
            
            User user = userService.findById(userId);
            if (user == null) {
                log.warn("删除用户失败 - 用户不存在: {}", userId);
                return ApiResponse.error(404, "用户不存在");
            }
            
            boolean success = userService.deleteUser(userId);
            log.info("删除用户 - ID: {}, 结果: {}", userId, success ? "成功" : "失败");
            
            if (success) {
                return ApiResponse.success("删除用户成功", true);
            } else {
                return ApiResponse.error(500, "删除用户失败");
            }
        } catch (Exception e) {
            log.error("删除用户失败", e);
            return ApiResponse.error(500, "删除用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有可用的用户角色
     */
    @GetMapping("/roles")
    public ApiResponse<List<String>> getAllRoles() {
        try {
            log.debug("获取所有用户角色");
            List<String> roles = userService.getAllRoles();
            return ApiResponse.success("获取用户角色成功", roles);
        } catch (Exception e) {
            log.error("获取用户角色失败", e);
            return ApiResponse.error(500, "获取用户角色失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量删除用户
     */
    @DeleteMapping("/batch")
    public ApiResponse<Map<String, Object>> batchDeleteUsers(@RequestBody List<Integer> userIds) {
        try {
            log.debug("批量删除用户请求 - 用户IDs: {}", userIds);
            
            if (userIds == null || userIds.isEmpty()) {
                log.warn("批量删除用户失败 - 用户ID列表为空");
                return ApiResponse.error(400, "用户ID列表不能为空");
            }
            
            Map<String, Object> result = new HashMap<>();
            List<Integer> successList = new java.util.ArrayList<>();
            List<String> errorList = new java.util.ArrayList<>();
            
            for (Integer userId : userIds) {
                try {
                    boolean success = userService.deleteUser(userId);
                    if (success) {
                        successList.add(userId);
                    } else {
                        errorList.add("用户ID " + userId + " 不存在");
                    }
                } catch (Exception e) {
                    errorList.add("用户ID " + userId + ": 删除失败 - " + e.getMessage());
                }
            }
            
            result.put("success", successList);
            result.put("error", errorList);
            
            log.info("批量删除用户完成 - 成功: {}, 失败: {}", successList.size(), errorList.size());
            
            return ApiResponse.success("批量删除完成", result);
        } catch (Exception e) {
            log.error("批量删除用户失败", e);
            return ApiResponse.error(500, "批量删除用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 重置用户密码为123456
     */
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody Map<String, Object> resetData) {
        try {
            log.debug("接收到重置密码请求: {}", resetData);
            
            // 验证请求参数
            Integer userId = null;
            String newPassword = null;
            
            if (resetData.containsKey("userId")) {
                userId = Integer.valueOf(resetData.get("userId").toString());
            }
            
            if (resetData.containsKey("newPassword")) {
                newPassword = resetData.get("newPassword").toString();
            }
            
            if (userId == null) {
                log.warn("重置密码失败 - 用户ID为空");
                return ApiResponse.error(400, "用户ID不能为空");
            }
            
            if (newPassword == null || newPassword.isEmpty()) {
                log.warn("重置密码失败 - 新密码为空");
                return ApiResponse.error(400, "新密码不能为空");
            }
            
            // 获取用户信息
            User user = userService.findById(userId);
            if (user == null) {
                log.warn("重置密码失败 - 用户不存在: {}", userId);
                return ApiResponse.error(404, "用户不存在");
            }
            
            // 强制修改密码（不需要旧密码）
            boolean success = userService.changePassword(userId, null, newPassword);
            
            if (success) {
                log.info("密码重置成功 - userId: {}, username: {}", userId, user.getUsername());
                return ApiResponse.success("密码重置成功", null);
            } else {
                log.error("密码重置失败 - userId: {}", userId);
                return ApiResponse.error(500, "密码重置失败");
            }
        } catch (Exception e) {
            log.error("密码重置失败", e);
            return ApiResponse.error(500, "密码重置失败: " + e.getMessage());
        }
    }
} 