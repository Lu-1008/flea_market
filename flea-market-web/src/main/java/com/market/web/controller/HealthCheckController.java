package com.market.web.controller;

import com.market.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器
 */
@RestController
@RequestMapping("/api/health")
public class HealthCheckController {

    /**
     * 健康检查接口
     * @return 服务状态信息
     */
    @GetMapping
    public ApiResponse<String> healthCheck() {
        return ApiResponse.success("服务运行正常", "OK");
    }
} 