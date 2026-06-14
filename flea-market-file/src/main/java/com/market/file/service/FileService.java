package com.market.file.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileService {
    /**
     * 上传用户头像

     */
    String uploadUserAvatar(MultipartFile file, Integer userId);

    /**
     * 上传用户头像（支持临时ID字符串）

     */
    String uploadUserAvatar(MultipartFile file, String userId);

    /**
     * 上传商品图片

     */
    String uploadItemImage(MultipartFile file, Integer itemId);

    /**
     * 获取文件流

     */
    InputStream getObject(String fileKey);

    /**
     * 删除文件

     */
    boolean deleteFile(String fileKey);
} 