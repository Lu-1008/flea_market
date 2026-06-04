package com.syf.fleamarket.controller;

import com.syf.fleamarket.common.Result;
import com.syf.fleamarket.config.MinioConfig;
import com.syf.fleamarket.service.FileService;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.Base64;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/file")
public class FileController {
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    private final FileService fileService;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Autowired
    public FileController(FileService fileService, MinioClient minioClient, MinioConfig minioConfig) {
        this.fileService = fileService;
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;
        logger.info("FileController初始化，MinIO配置: URL={}, BucketName={}", minioConfig.getUrl(), minioConfig.getBucketName());
    }

    /**
     * 上传用户头像

     * @return 文件访问路径
     */
    @PostMapping("/upload/user")
    public Result<String> uploadUserAvatar(@RequestParam("file") MultipartFile file, 
                                          @RequestParam(value = "userId", required = false) String userId) {
        logger.info("接收到用户头像上传请求: 文件名={}, 大小={}, userId={}", 
                   file.getOriginalFilename(), file.getSize(), userId);
                   
        // 验证userId参数，只处理有效的用户ID
        Integer numericUserId = null;
        
        // 尝试转换为数字类型的用户ID
        if (userId != null) {
            try {
                numericUserId = Integer.parseInt(userId);
                if (numericUserId <= 0) {
                    logger.warn("无效的用户ID: {}", userId);
                    return Result.fail("无效的用户ID，请先保存用户信息");
                }
            } catch (NumberFormatException e) {
                logger.warn("用户ID转换为数字失败: {}", userId);
                return Result.fail("无效的用户ID格式，请先保存用户信息");
            }
        } else {
            return Result.fail("未提供用户ID，请先保存用户信息");
        }
        
        try {
            logger.debug("开始处理用户头像上传, numericUserId={}", numericUserId);
            // 使用有效的数字用户ID上传头像
            String path = fileService.uploadUserAvatar(file, numericUserId);
            
            logger.info("用户头像上传成功: path={}, userId={}", path, userId);
            return Result.success(path, "用户头像上传成功");
        } catch (Exception e) {
            logger.error("上传用户头像失败: userId={}, error={}", userId, e.getMessage(), e);
            return Result.fail("上传用户头像失败：" + e.getMessage());
        }
    }

    /**
     * 上传商品图片

     * @return 文件访问路径
     */
    @PostMapping("/upload/item")
    public Result<String> uploadItemImage(@RequestParam("file") MultipartFile file, 
                                         @RequestParam(value = "itemId", required = false) Integer itemId) {
        logger.info("接收到商品图片上传请求: 文件名={}, 大小={}, itemId={}", 
                   file.getOriginalFilename(), file.getSize(), itemId);
                   
        // 验证商品ID参数
        if (itemId == null || itemId <= 0) {
            String message = "上传商品图片缺少有效的商品ID";
            logger.error(message + ": {}", itemId);
            return Result.fail(message);
        }
        
        try {
            logger.debug("开始处理商品图片上传: itemId={}", itemId);
            String path = fileService.uploadItemImage(file, itemId);
            logger.info("商品图片上传成功: path={}, itemId={}", path, itemId);
            return Result.success(path, "商品图片上传成功");
        } catch (Exception e) {
            logger.error("商品图片上传失败: itemId={}, error={}", itemId, e.getMessage(), e);
            return Result.fail("上传商品图片失败：" + e.getMessage());
        }
    }

    /**
     * 查看文件（带禁用缓存选项）
     * @param fileKey 文件键
     * @param noCache 是否禁用缓存
     * @param response HTTP响应
     */
    @GetMapping("/view/{fileKey}")
    public void viewFile(@PathVariable String fileKey, 
                        @RequestParam(value = "noCache", required = false, defaultValue = "false") boolean noCache,
                        HttpServletResponse response) {
        logger.info("接收到文件查看请求: fileKey={}, noCache={}", fileKey, noCache);
        
        try {
            // 首先进行URL解码
            String decodedKey = URLDecoder.decode(fileKey, StandardCharsets.UTF_8);
            logger.debug("URL解码后的路径: {}", decodedKey);
            
            // 保持路径原样，不做转换，由FileService处理多种路径格式
            String processedFileKey = decodedKey;
            logger.info("处理后的文件路径: {}", processedFileKey);
            
            // 设置缓存控制头
            if (noCache) {
                // 禁用缓存
                response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");
            } else {
                // 允许缓存，但需要验证
                response.setHeader("Cache-Control", "max-age=86400"); // 缓存一天
            }
            
            logger.debug("开始获取文件对象: {}", processedFileKey);
            InputStream inputStream = fileService.getObject(processedFileKey);
            
            if (inputStream == null) {
                logger.error("所有路径尝试均失败，文件不存在: {}", processedFileKey);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            // 根据文件类型设置ContentType
            String contentType = determineContentType(processedFileKey);
            response.setContentType(contentType);
            logger.debug("设置ContentType: {}", contentType);
            
            // 复制文件流到响应
            logger.debug("开始复制文件流到响应");
            IOUtils.copy(inputStream, response.getOutputStream());
            response.flushBuffer();
            
            // 关闭流
            inputStream.close();
            logger.info("文件查看成功: {}", processedFileKey);
        } catch (Exception e) {
            logger.error("文件查看失败: {}", fileKey, e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * 删除文件
     * @return 删除结果
     */
    @PostMapping("/delete")
    public Result<Boolean> deleteFile(@RequestBody Map<String, String> requestBody) {
        String fileKey = requestBody.get("fileKey");
        if (fileKey == null || fileKey.isEmpty()) {
            return Result.fail("文件路径不能为空");
        }
        
        try {
            boolean result = fileService.deleteFile(fileKey);
            return Result.success(result, "文件删除成功");
        } catch (Exception e) {
            return Result.fail("删除文件失败：" + e.getMessage());
        }
    }

    /**
     * 根据文件名确定ContentType

     * @return ContentType
     */
    private String determineContentType(String fileKey) {
        fileKey = fileKey.toLowerCase();
        if (fileKey.endsWith(".jpg") || fileKey.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG_VALUE;
        } else if (fileKey.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        } else if (fileKey.endsWith(".gif")) {
            return MediaType.IMAGE_GIF_VALUE;
        } else if (fileKey.endsWith(".webp")) {
            return "image/webp";
        } else {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }

    /**
     * 检查MinIO连接状态
     * @return MinIO连接状态
     */
    @GetMapping("/check-minio")
    public Result<?> checkMinioStatus() {
        try {
            // 检查MinIO是否可以连接和桶是否存在
            boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .build()
            );
            
            Map<String, Object> result = new HashMap<>();
            result.put("connected", true);
            result.put("bucketExists", bucketExists);
            result.put("bucketName", minioConfig.getBucketName());
            result.put("endpointUrl", minioConfig.getUrl());
            
            return Result.success(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("connected", false);
            error.put("message", e.getMessage());
            error.put("endpointUrl", minioConfig.getUrl());
            
            return Result.fail(500, "MinIO连接失败: " + e.getMessage());
        }
    }

    /**
     * 强制清除图片缓存（API）
     */
    @GetMapping("/clear-cache/{fileKey}")
    public Result<?> clearImageCache(@PathVariable String fileKey) {
        try {
            // 检查文件是否存在
            InputStream inputStream = fileService.getObject(fileKey);
            if (inputStream == null) {
                return Result.fail("文件不存在: " + fileKey);
            }
            inputStream.close();
            
            // 返回成功，前端需要刷新页面或重新加载图片
            Map<String, Object> data = new HashMap<>();
            data.put("fileKey", fileKey);
            data.put("url", minioConfig.getUrl() + "/" + minioConfig.getBucketName() + "/" + fileKey + "?t=" + System.currentTimeMillis());
            
            return Result.success(data, "清除图片缓存成功，请刷新页面查看最新图片");
        } catch (Exception e) {
            logger.error("清除图片缓存失败: {}", fileKey, e);
            return Result.fail("清除图片缓存失败: " + e.getMessage());
        }
    }
} 
 