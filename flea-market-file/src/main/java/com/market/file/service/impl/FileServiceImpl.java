package com.market.file.service.impl;

import com.market.file.config.MinioConfig;
import com.market.file.service.FileService;
import io.minio.*;
import io.minio.errors.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {
    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Autowired
    public FileServiceImpl(MinioClient minioClient, MinioConfig minioConfig) {
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;
        logger.info("FileServiceImpl初始化，MinIO配置: URL={}, BucketName={}", minioConfig.getUrl(), minioConfig.getBucketName());
        
//        // 确保桶存在
//        try {
//            logger.info("检查MinIO桶是否存在: {}", minioConfig.getBucketName());
//            boolean bucketExists = minioClient.bucketExists(
//                    BucketExistsArgs.builder().bucket(minioConfig.getBucketName()).build()
//            );
//            if (!bucketExists) {
//                logger.info("MinIO桶不存在，创建新桶: {}", minioConfig.getBucketName());
//                minioClient.makeBucket(
//                        MakeBucketArgs.builder().bucket(minioConfig.getBucketName()).build()
//                );
//                logger.info("MinIO桶创建成功: {}", minioConfig.getBucketName());
//            } else {
//                logger.info("MinIO桶已存在: {}", minioConfig.getBucketName());
//            }
//        } catch (Exception e) {
//            logger.error("初始化MinIO存储桶失败", e);
//            throw new RuntimeException("初始化 Minio 存储桶失败", e);
//        }
    }

    @Override
    public String uploadUserAvatar(MultipartFile file, Integer userId) {
        logger.info("上传用户头像: userId={}, 文件名={}, 大小={}", userId, file.getOriginalFilename(), file.getSize());
        if (file.isEmpty()) {
            logger.error("上传的用户头像文件为空");
            throw new IllegalArgumentException("上传的文件不能为空");
        }

        if (userId == null || userId <= 0) {
            logger.error("上传用户头像必须指定有效的用户ID");
            throw new IllegalArgumentException("上传用户头像必须指定有效的用户ID");
        }

        // 修改路径，移除多余的flea-market前缀
        String fileKey = "user-" + userId;
        // 记录路径便于调试
        logger.info("用户头像路径: {}", fileKey);
        return uploadFile(file, fileKey);
    }

    @Override
    public String uploadUserAvatar(MultipartFile file, String userId) {
        logger.info("上传用户头像(字符串ID): userId={}, 文件名={}, 大小={}", userId, file.getOriginalFilename(), file.getSize());
        if (file.isEmpty()) {
            logger.error("上传的用户头像文件为空");
            throw new IllegalArgumentException("上传的文件不能为空");
        }

        // 尝试将字符串ID转换为整数
        try {
            Integer numericUserId = Integer.parseInt(userId);
            if (numericUserId <= 0) {
                logger.error("无效的用户ID: {}", userId);
                throw new IllegalArgumentException("上传用户头像必须指定有效的用户ID");
            }
            // 使用统一的user-前缀
            String fileKey = "user-" + numericUserId;
            logger.info("用户头像路径: {}", fileKey);
            return uploadFile(file, fileKey);
        } catch (NumberFormatException e) {
            logger.error("无效的用户ID格式: {}", userId);
            throw new IllegalArgumentException("上传用户头像必须指定有效的用户ID");
        }
    }

    @Override
    public String uploadItemImage(MultipartFile file, Integer itemId) {
        logger.info("上传商品图片: itemId={}, 文件名={}, 大小={}", itemId, file.getOriginalFilename(), file.getSize());
        if (file.isEmpty()) {
            logger.error("上传的商品图片文件为空");
            throw new IllegalArgumentException("上传的文件不能为空");
        }

        String fileKey;
        if (itemId != null && itemId > 0) {
            // 使用item-{itemId}格式
            fileKey = "item-" + itemId;
        } else {
            // 如果没有指定商品ID，则抛出异常，不再使用临时文件名
            throw new IllegalArgumentException("上传商品图片必须指定商品ID");
        }
        logger.debug("生成的商品图片文件路径: {}", fileKey);
        return uploadFile(file, fileKey);
    }

    @Override
    public InputStream getObject(String fileKey) {
        logger.info("获取文件对象: {}", fileKey);
        
        // 检查并移除可能存在的flea-market/前缀
        String originalFileKey = fileKey;
        
        // 处理文件键，确保格式正确
        if (fileKey.startsWith("flea-market/")) {
            fileKey = fileKey.substring("flea-market/".length());
            logger.debug("1.移除flea-market/前缀，实际查询路径: {}", fileKey);
        }
        
        // 移除可能存在的任何路径前缀，只保留文件名
        int lastSlashIndex = fileKey.lastIndexOf('/');
        if (lastSlashIndex != -1) {
            String oldKey = fileKey;
            fileKey = fileKey.substring(lastSlashIndex + 1);
            logger.debug("2.提取文件名，移除路径部分: {} -> {}", oldKey, fileKey);
        }
        
        // 尝试找出最终的文件键格式
        if (fileKey.contains("-") && (fileKey.startsWith("item-") || fileKey.startsWith("user-"))) {
            // 已经是正确格式，无需处理
            logger.debug("文件键已经是期望格式: {}", fileKey);
        } else if (fileKey.contains("item") || fileKey.contains("user")) {
            // 尝试规范化格式
            String oldKey = fileKey;
            if (fileKey.contains("item")) {
                String idPart = fileKey.replaceAll(".*item[^0-9]*([0-9]+).*", "$1");
                fileKey = "item-" + idPart;
            } else if (fileKey.contains("user")) {
                String idPart = fileKey.replaceAll(".*user[^0-9]*([0-9]+).*", "$1");
                fileKey = "user-" + idPart;
            }
            logger.debug("3.规范化文件键格式: {} -> {}", oldKey, fileKey);
        }
        
        // 记录最终使用的文件键
        logger.info("最终查询的文件键: {}", fileKey);
        
        try {
            // 尝试直接获取文件对象
            try {
                InputStream stream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .object(fileKey)
                                .build()
                );
                logger.debug("文件获取成功: {}", fileKey);
                return stream;
            } catch (Exception e) {
                logger.debug("直接路径获取失败: {} - {}", fileKey, e.getMessage());
                
                // 如果失败，尝试使用原始路径
                if (!originalFileKey.equals(fileKey)) {
                    try {
                        logger.debug("尝试使用原始路径: {}", originalFileKey);
                        InputStream stream = minioClient.getObject(
                                GetObjectArgs.builder()
                                        .bucket(minioConfig.getBucketName())
                                        .object(originalFileKey)
                                        .build()
                        );
                        logger.info("使用原始路径成功: {}", originalFileKey);
                        return stream;
                    } catch (Exception ex) {
                        logger.debug("原始路径获取失败: {}", ex.getMessage());
                    }
                }
                
                // 尝试其他格式 - 只在前面的尝试都失败时执行
                String[] possibleKeys = new String[] {
                    fileKey,
                    "flea-market/" + fileKey,
                    fileKey.replaceAll("-", "/")
                };
                
                for (String key : possibleKeys) {
                    // 跳过已经尝试过的键
                    if (key.equals(fileKey) || key.equals(originalFileKey)) continue;
                    
                    try {
                        logger.debug("尝试备选路径: {}", key);
                        InputStream stream = minioClient.getObject(
                                GetObjectArgs.builder()
                                        .bucket(minioConfig.getBucketName())
                                        .object(key)
                                        .build()
                        );
                        logger.info("使用备选路径成功: {}", key);
                        return stream;
                    } catch (Exception ex) {
                        logger.debug("备选路径获取失败: {} - {}", key, ex.getMessage());
                    }
                }
                
                // 所有尝试都失败了
                logger.error("所有路径尝试都失败: 原始={}, 处理后={}", originalFileKey, fileKey);
                return null;
            }
        } catch (Exception e) {
            logger.error("获取文件过程中发生异常: {}", fileKey, e);
            return null; // 返回null而不是抛出异常，便于上层处理
        }
    }

    @Override
    public boolean deleteFile(String fileKey) {
        logger.info("删除文件: {}", fileKey);
        if (fileKey == null || fileKey.isEmpty()) {
            logger.error("文件路径为空，无法删除");
            return false;
        }
        
        // 保存原始文件键
        String originalFileKey = fileKey;
        
        // 处理文件键，尝试规范化格式
        // 1. 检查并移除可能存在的flea-market/前缀
        if (fileKey.startsWith("flea-market/")) {
            fileKey = fileKey.substring("flea-market/".length());
            logger.debug("1.移除flea-market/前缀，实际删除路径: {}", fileKey);
        }
        
        // 2. 移除可能存在的任何路径前缀，只保留文件名
        int lastSlashIndex = fileKey.lastIndexOf('/');
        if (lastSlashIndex != -1) {
            String oldKey = fileKey;
            fileKey = fileKey.substring(lastSlashIndex + 1);
            logger.debug("2.提取文件名，移除路径部分: {} -> {}", oldKey, fileKey);
        }
        
        // 3. 尝试找出最终的文件键格式
        if (fileKey.contains("-") && (fileKey.startsWith("item-") || fileKey.startsWith("user-"))) {
            // 已经是正确格式，无需处理
            logger.debug("文件键已经是期望格式: {}", fileKey);
        } else if (fileKey.contains("item") || fileKey.contains("user")) {
            // 尝试规范化格式
            String oldKey = fileKey;
            if (fileKey.contains("item")) {
                String idPart = fileKey.replaceAll(".*item[^0-9]*([0-9]+).*", "$1");
                fileKey = "item-" + idPart;
            } else if (fileKey.contains("user")) {
                String idPart = fileKey.replaceAll(".*user[^0-9]*([0-9]+).*", "$1");
                fileKey = "user-" + idPart;
            }
            logger.debug("3.规范化文件键格式: {} -> {}", oldKey, fileKey);
        }
        
        // 记录最终使用的文件键
        logger.info("最终删除的文件键: {}", fileKey);
        
        // 定义可能的文件键格式
        String[] possibleKeys = new String[] {
            fileKey,
            originalFileKey,
            "flea-market/" + fileKey,
            fileKey.replaceAll("-", "/"),
            "flea-market/" + fileKey.replaceAll("-", "/")
        };
        
        boolean success = false;
        
        // 尝试所有可能的文件键格式
        for (String key : possibleKeys) {
            try {
                logger.debug("尝试删除路径: {}", key);
                minioClient.removeObject(
                    RemoveObjectArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .object(key)
                        .build()
                );
                logger.info("文件删除成功: {}", key);
                success = true;
                break; // 一旦成功删除就退出循环
            } catch (Exception e) {
                logger.debug("删除路径失败: {} - {}", key, e.getMessage());
                // 继续尝试下一个可能的键
            }
        }
        
        if (!success) {
            logger.warn("所有删除尝试都失败: 原始={}, 处理后={}", originalFileKey, fileKey);
        }
        
        return success;
    }

    /**
     * 通用上传文件方法
     * @param file 文件
     * @param fileKey 文件键（路径）
     * @return 文件访问路径
     */
    private String uploadFile(MultipartFile file, String fileKey) {
        logger.info("上传文件: fileKey={}, 文件名={}, 类型={}, 大小={}", 
                fileKey, file.getOriginalFilename(), file.getContentType(), file.getSize());
                
        // 严格确保文件键不会生成文件夹结构
        
        // 1. 检查并移除可能存在的flea-market/前缀
        if (fileKey.startsWith("flea-market/")) {
            fileKey = fileKey.substring("flea-market/".length());
            logger.debug("1.移除flea-market/前缀，实际上传路径: {}", fileKey);
        }
        
        // 2. 检查并移除任何可能存在的路径分隔符，确保是单纯的文件名
        int lastSlashIndex = fileKey.lastIndexOf('/');
        if (lastSlashIndex != -1) {
            String oldKey = fileKey;
            fileKey = fileKey.substring(lastSlashIndex + 1);
            logger.debug("2.提取文件名，移除路径部分: {} -> {}", oldKey, fileKey);
        }
        
        // 3. 替换所有斜杠为连字符，确保没有文件夹结构
        if (fileKey.contains("/")) {
            String oldKey = fileKey;
            fileKey = fileKey.replace("/", "-");
            logger.debug("3.将斜杠替换为连字符: {} -> {}", oldKey, fileKey);
        }
        
        // 4. 确保前缀为item-或user-开头，即使文件键被处理过也保留格式
        if (!fileKey.startsWith("item-") && !fileKey.startsWith("user-") && 
            (fileKey.contains("item") || fileKey.contains("user"))) {
            String oldKey = fileKey;
            if (fileKey.contains("item")) {
                // 提取数字ID部分
                String idPart = fileKey.replaceAll(".*item[^0-9]*([0-9]+).*", "$1");
                fileKey = "item-" + idPart;
            } else if (fileKey.contains("user")) {
                String idPart = fileKey.replaceAll(".*user[^0-9]*([0-9]+).*", "$1");
                fileKey = "user-" + idPart;
            }
            logger.debug("4.规范化文件键格式: {} -> {}", oldKey, fileKey);
        }
        
        logger.info("最终处理后的上传路径: {}", fileKey);
        
        try {
            // 获取文件信息
            String contentType = file.getContentType();
            InputStream inputStream = file.getInputStream();
            long size = file.getSize();

            logger.debug("开始上传文件到MinIO: bucket={}, object={}", minioConfig.getBucketName(), fileKey);
            // 上传到Minio
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileKey)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );

            // 关闭流
            inputStream.close();
            logger.info("文件上传成功: {}", fileKey);
            
            return fileKey;
        } catch (Exception e) {
            logger.error("上传文件失败: {}", fileKey, e);
            throw new RuntimeException("上传文件失败", e);
        }
    }
} 