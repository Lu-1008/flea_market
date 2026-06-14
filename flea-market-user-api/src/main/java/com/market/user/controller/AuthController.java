package com.market.user.controller;

import com.market.common.ApiResponse;
import com.market.user.JwtUtil;
import com.market.user.dto.request.ChangePasswordRequest;
import com.market.user.dto.request.LoginRequest;
import com.market.user.dto.response.LoginResponse;
import com.market.user.entity.User;
import com.market.user.entity.UserRole;
import com.market.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        // 打印登录请求信息（不包含密码）
        log.info("收到管理员登录请求: username={}", loginRequest.getUsername());
        
        // 验证用户
        User user = userService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());
        if (user == null) {
            log.warn("登录失败: 用户名或密码错误");
            return ApiResponse.error(401, "用户名或密码错误");
        }

        // 记录最小必要信息
        log.info("管理员登录成功: username={}, role={}", user.getUsername(), user.getRole());

        // 检查用户角色
        if (user.getRole() == UserRole.USER) {
            log.warn("登录失败: 普通用户 {} 尝试访问管理后台", user.getUsername());
            return ApiResponse.error(403, "普通用户无权访问管理后台");
        }

        // 生成JWT令牌（不记录令牌内容）
        String token = jwtUtil.generateToken(user);

        // 构建响应
        LoginResponse response = LoginResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .token(token)
                .userImgUrl(user.getUserImgUrl())
                .build();

        return ApiResponse.success("登录成功", response);
    }
    
    /**
     * 用户端登录接口 - 允许普通用户登录
     */
    @PostMapping("/user-login")
    public ApiResponse<LoginResponse> userLogin(@RequestBody LoginRequest loginRequest) {
        // 打印登录请求信息（不包含密码）
        log.info("收到用户端登录请求: username={}", loginRequest.getUsername());
        
        // 验证用户
        User user = userService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());
        if (user == null) {
            log.warn("用户端登录失败: 用户名或密码错误");
            return ApiResponse.error(401, "用户名或密码错误");
        }

        // 记录最小必要信息
        log.info("用户登录成功: username={}, role={}", user.getUsername(), user.getRole());

        // 生成JWT令牌（不记录令牌内容）
        String token = jwtUtil.generateToken(user);

        // 构建响应
        LoginResponse response = LoginResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .token(token)
                .userImgUrl(user.getUserImgUrl())
                .build();
        
        return ApiResponse.success("登录成功", response);
    }
    
    /**
     * 修改密码
     * @param request 修改密码请求
     * @return 修改结果
     */
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        // 打印接收到的请求参数（不包含密码）
        log.info("接收到修改密码请求: userId={}", request.getUserId());
        
        // 参数验证
        if (request.getUserId() == null) {
            log.warn("用户ID为空，尝试使用默认ID 1");
            // 如果前端没有传递用户ID，默认使用ID为1（假设是admin用户）
            request.setUserId(1);
        }
        
        if (request.getOldPassword() == null) {
            log.warn("旧密码为空");
            return ApiResponse.error(400, "旧密码不能为空");
        }
        
        if (request.getNewPassword() == null) {
            log.warn("新密码为空");
            return ApiResponse.error(400, "新密码不能为空");
        }
        
        // 密码长度验证
        if (request.getNewPassword().length() < 6) {
            log.warn("新密码长度不足6位");
            return ApiResponse.error(400, "新密码长度不能小于6位");
        }
        
        // 调用服务修改密码
        boolean success = userService.changePassword(
                request.getUserId(), 
                request.getOldPassword(),
                request.getNewPassword());
        
        if (success) {
            log.info("密码修改成功: userId={}", request.getUserId());
            return ApiResponse.success("密码修改成功", null);
        } else {
            log.warn("密码修改失败: userId={}", request.getUserId());
            return ApiResponse.error(400, "原密码错误或用户不存在");
        }
    }

    /**
     * 用户登出接口
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // 实际上JWT认证方式不需要后端处理登出，只需前端清除token
        // 这里只做记录和返回成功状态
        log.info("用户登出请求成功处理");
        return ApiResponse.success("登出成功", null);
    }
} 