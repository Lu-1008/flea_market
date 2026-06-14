package com.market.file.config;

import io.minio.MinioClient;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.*;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "minio.config")
public class MinioConfig {
    private static final Logger logger = LoggerFactory.getLogger(MinioConfig.class);

    /**
     * 服务地址
     */
    private String url;

    /**
     * 用户名
     */
    private String accessKey;

    /**
     * 密码
     */
    private String secretKey;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 对象前缀（不使用任何前缀，直接存储在桶根目录）
     * 如果设置了此值，将在所有对象键前添加此前缀
     */
    private String objectPrefix = "";

    public String getFullObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isEmpty()) {
            return "";
        }
        
        // 如果有设置前缀并且对象键不是以前缀开头，则添加前缀
        if (!objectPrefix.isEmpty() && !objectKey.startsWith(objectPrefix)) {
            logger.debug("添加对象前缀: {} -> {}{}", objectKey, objectPrefix, objectKey);
            return objectPrefix + objectKey;
        }
        
        return objectKey;
    }

    @Bean
    public MinioClient getMinioClient() {
        logger.info("初始化 MinioClient: url={}, bucketName={}, objectPrefix={}", 
                url, bucketName, objectPrefix.isEmpty() ? "<无前缀>" : objectPrefix);
        
        // 设置API端口和参数
        String endpoint = "localhost"; // 默认主机名
        int port = 9000; // MinIO的API端口固定为9000
        boolean secure = false;
        
        if (url != null && url.startsWith("http")) {
            secure = url.startsWith("https");
            // 从URL中提取主机名
            String host = url.replaceAll("^https?://", "");
            if (host.contains(":")) {
                String[] parts = host.split(":");
                endpoint = parts[0];
                logger.info("从配置中提取主机名: {} (忽略端口号，使用9000作为API端口)", endpoint);
            } else {
                endpoint = host;
            }
        } else {
            logger.warn("URL格式不正确，使用默认endpoint: localhost");
        }
        
        logger.info("MinioClient最终配置: endpoint={}, port={}, secure={}, accessKey={}, bucketName={}", 
                endpoint, port, secure, accessKey, bucketName);
                
        return MinioClient.builder()
                .endpoint(endpoint, port, secure)
                .credentials(accessKey, secretKey)
                .build();
    }
}
