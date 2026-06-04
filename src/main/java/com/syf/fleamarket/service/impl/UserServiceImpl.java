package com.syf.fleamarket.service.impl;

import com.syf.fleamarket.dao.UserMapper;
import com.syf.fleamarket.entity.User;
import com.syf.fleamarket.entity.enums.UserRole;
import com.syf.fleamarket.service.UserService;
import com.syf.fleamarket.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    
    private final UserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;
    private final FileService fileService;
    
    @Autowired
    public UserServiceImpl(UserMapper userMapper, JdbcTemplate jdbcTemplate, FileService fileService) {
        this.userMapper = userMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.fileService = fileService;
        log.info("UserServiceImpl初始化完成");
        
        // 在初始化时检查数据库表结构
        try {
            log.info("正在检查数据库表结构...");
            
            // 查询users表结构
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_KEY FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'users'"
            );
            
            log.debug("users表结构获取成功，列数: {}", columns.size());
            
            log.debug("users表结构:");
            for (Map<String, Object> column : columns) {
                log.debug("列名: {}, 数据类型: {}, 键类型: {}", 
                         column.get("COLUMN_NAME"), 
                         column.get("DATA_TYPE"), 
                         column.get("COLUMN_KEY"));
            }
            
            // 查询一个用户记录，检查实际数据
            Map<String, Object> adminUser = null;
            try {
                adminUser = jdbcTemplate.queryForMap("SELECT * FROM users WHERE username = 'admin'");
                log.debug("admin用户数据:");
                for (Map.Entry<String, Object> entry : adminUser.entrySet()) {
                    log.debug("{}: {} (类型: {})", 
                             entry.getKey(), 
                             entry.getValue(),
                             entry.getValue() != null ? entry.getValue().getClass().getName() : "null");
                }
            } catch (Exception e) {
                log.warn("查询admin用户数据失败: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("检查数据库表结构失败", e);
        }
    }
    
    /**
     * 验证密码
     */
    private boolean verifyPassword(String rawPassword, String storedPassword) {
        // 不记录密码相关信息，即使是debug级别
        return rawPassword.equals(storedPassword);
    }

    @Override
    public User authenticate(String username, String password) {
        log.info("尝试验证用户: {}", username);
        
        User user = userMapper.findByUsername(username);
        
        if (user == null) {
            log.warn("用户不存在: {}", username);
            return null;
        }
        
        // 减少不必要的调试信息
        if (log.isDebugEnabled()) {
            log.debug("用户 {} 信息查询成功", username);
        }
        
        if (verifyPassword(password, user.getPassword())) {
            return user;
        }
        
        log.warn("用户 {} 密码不匹配", username);
        return null;
    }

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }
    
    @Override
    public boolean changePassword(Integer userId, String oldPassword, String newPassword) {
        log.info("处理修改密码请求 - userId={}", userId);
        
        if (userId == null) {
            log.warn("修改密码失败 - 用户ID为空");
            return false;
        }
        
        if (newPassword == null) {
            log.warn("修改密码失败 - 新密码为空");
            return false;
        }
        
        // 根据ID查找用户
        User user = userMapper.findById(userId);
        
        // 如果找不到用户，尝试通过用户名查找（假设ID为1的是admin用户）
        if (user == null && userId != null && userId == 1) {
            log.info("通过ID找不到用户，尝试查找admin用户");
            user = userMapper.findByUsername("admin");
            
            // 如果找到了admin用户，但ID为null，设置ID为1
            if (user != null && user.getUserId() == null) {
                log.info("找到admin用户，但ID为null，设置为1");
                user.setUserId(1);
                try {
                    userMapper.updateUser(user);
                    log.info("已更新admin用户ID为1");
                } catch (Exception e) {
                    log.error("更新admin用户ID失败", e);
                }
            }
        }
        
        // 如果用户不存在
        if (user == null) {
            log.warn("修改密码失败 - 用户ID {} 不存在", userId);
            return false;
        }
        
        // 如果提供了旧密码并且不匹配
        if (oldPassword != null && !verifyPassword(oldPassword, user.getPassword())) {
            log.warn("修改密码失败 - 用户 {} 旧密码不匹配", user.getUsername());
            return false;
        }
        
        // 修改密码
        user.setPassword(newPassword);
        try {
            userMapper.updateUser(user);
            log.info("密码修改成功 - 用户: {}", user.getUsername());
            return true;
        } catch (Exception e) {
            log.error("修改密码失败 - 数据库更新异常", e);
            return false;
        }
    }


    @Override
    public Map<String, Object> getUserList(Map<String, Object> params) {
        log.debug("获取用户列表 - 参数: {}", params);
        
        // 处理分页参数
        Integer page = (Integer) params.get("page");
        Integer size = (Integer) params.get("size");
        
        if (page != null && size != null) {
            // 计算偏移量
            int offset = (page - 1) * size;
            params.put("offset", offset);
            params.put("limit", size);
            log.debug("计算分页参数: offset={}, limit={}", offset, size);
        }
        
        try {
            // 查询用户列表
            List<User> users = userMapper.findUserList(params);
            log.debug("查询结果: 获取到 {} 条用户记录", users.size());
            
            // 查询总数
            int total = userMapper.countUsers(params);
            log.debug("总记录数: {}", total);
            
            // 封装结果
            Map<String, Object> result = new HashMap<>();
            result.put("list", users);
            result.put("total", total);
            
            return result;
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            
            // 返回空结果，避免前端出错
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("list", new ArrayList<>());
            emptyResult.put("total", 0);
            return emptyResult;
        }
    }
    
    @Override
    public User findById(Integer userId) {
        log.debug("根据ID查询用户 - ID: {}", userId);
        try {
            User user = userMapper.findById(userId);
            if (user == null) {
                log.debug("用户不存在 - ID: {}", userId);
            } else {
                log.debug("用户查询成功 - ID: {}, 用户名: {}", userId, user.getUsername());
            }
            return user;
        } catch (Exception e) {
            log.error("查询用户失败 - ID: {}", userId, e);
            return null;
        }
    }
    
    @Override
    @Transactional
    public User createUser(User user) {
        log.debug("创建用户 - 用户名: {}, 邮箱: {}", user.getUsername(), user.getEmail());
        
        try {
            // 检查用户名是否已存在
            User existingUser = userMapper.findByUsername(user.getUsername());
            if (existingUser != null) {
                log.warn("创建用户失败 - 用户名已存在: {}", user.getUsername());
                throw new RuntimeException("用户名已存在");
            }
            
            // 检查邮箱是否已存在
            existingUser = userMapper.findByEmail(user.getEmail());
            if (existingUser != null) {
                log.warn("创建用户失败 - 邮箱已被使用: {}", user.getEmail());
                throw new RuntimeException("邮箱已被使用");
            }
            
            // 设置默认头像（如果没有提供）
            if (user.getUserImgUrl() == null || user.getUserImgUrl().isEmpty()) {
                user.setUserImgUrl("user-default.png");
            }
            
            // 处理transient字段
            if (user.getAverageRating() == null) {
                user.setAverageRating(0.0);
            }
            
            // 确保角色是有效的枚举值
            if (user.getRole() == null) {
                user.setRole(UserRole.USER); // 默认设置为普通用户
            } else {
                // 尝试确保角色是有效的枚举值
                try {
                    if (!(user.getRole() instanceof UserRole)) {
                        // 如果不是枚举类型，尝试转换
                        String roleStr = user.getRole().toString();
                        user.setRole(UserRole.valueOf(roleStr));
                    }
                } catch (Exception e) {
                    log.warn("无效的用户角色: {}, 将设置为默认角色USER", user.getRole());
                    user.setRole(UserRole.USER);
                }
            }
            
            // 插入用户
            userMapper.insert(user);
            log.info("用户创建成功 - ID: {}, 用户名: {}", user.getUserId(), user.getUsername());
            
            // 查询新创建的用户
            User createdUser = userMapper.findById(user.getUserId());
            
            // 设置transient字段的默认值
            createdUser.setAverageRating(0.0);
            
            return createdUser;
        } catch (RuntimeException e) {
            log.error("创建用户失败 - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("创建用户失败", e);
            throw new RuntimeException("创建用户失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public User updateUser(User user) {
        log.debug("更新用户 - ID: {}, 用户名: {}", user.getUserId(), user.getUsername());
        
        try {
            // 检查用户是否存在
            User existingUser = userMapper.findById(user.getUserId());
            if (existingUser == null) {
                log.warn("更新用户失败 - 用户不存在: {}", user.getUserId());
                return null;
            }
            
            // 检查用户名是否被其他用户使用
            if (user.getUsername() != null && !user.getUsername().isEmpty()) {
                User userByUsername = userMapper.findByUsername(user.getUsername());
                if (userByUsername != null && !userByUsername.getUserId().equals(user.getUserId())) {
                    log.warn("更新用户失败 - 用户名已被其他用户使用: {}", user.getUsername());
                    throw new RuntimeException("用户名已被其他用户使用");
                }
            } else {
                // 如果没有提供用户名，保留原用户名
                user.setUsername(existingUser.getUsername());
            }
            
            // 检查邮箱是否被其他用户使用
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                User userByEmail = userMapper.findByEmail(user.getEmail());
                if (userByEmail != null && !userByEmail.getUserId().equals(user.getUserId())) {
                    log.warn("更新用户失败 - 邮箱已被其他用户使用: {}", user.getEmail());
                    throw new RuntimeException("邮箱已被其他用户使用");
                }
            } else {
                // 如果没有提供邮箱，保留原邮箱
                user.setEmail(existingUser.getEmail());
            }
            
            // 确保角色是有效的枚举值
            if (user.getRole() == null) {
                // 保留原角色
                user.setRole(existingUser.getRole());
            } else {
                // 尝试确保角色是有效的枚举值
                try {
                    if (!(user.getRole() instanceof UserRole)) {
                        // 如果不是枚举类型，尝试转换
                        String roleStr = user.getRole().toString();
                        user.setRole(UserRole.valueOf(roleStr));
                    }
                } catch (Exception e) {
                    log.warn("无效的用户角色: {}, 将保留原角色: {}", user.getRole(), existingUser.getRole());
                    user.setRole(existingUser.getRole());
                }
            }
            
            // 保留原密码（如果没有提供新密码）
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                user.setPassword(existingUser.getPassword());
            }
            
            // 保留原头像（如果没有提供新头像）
            if (user.getUserImgUrl() == null || user.getUserImgUrl().isEmpty()) {
                user.setUserImgUrl(existingUser.getUserImgUrl());
            }
            
            // 设置transient字段
            user.setAverageRating(0.0);
            
            // 保留原电话（如果没有提供新电话）
            if (user.getPhone() == null) {
                user.setPhone(existingUser.getPhone());
            }
            
            // 更新用户
            userMapper.updateUser(user);
            log.info("用户更新成功 - ID: {}, 用户名: {}", user.getUserId(), user.getUsername());
            
            // 查询更新后的完整用户信息
            User updatedUser = userMapper.findById(user.getUserId());
            
            // 设置transient字段的默认值
            updatedUser.setAverageRating(0.0);
            
            return updatedUser;
        } catch (RuntimeException e) {
            log.error("更新用户失败 - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("更新用户失败", e);
            throw new RuntimeException("更新用户失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public boolean deleteUser(Integer userId) {
        // 检查用户是否存在
        User existingUser = userMapper.findById(userId);
        if (existingUser == null) {
            return false;
        }
        
        try {
            // 删除用户头像（如果存在）
            if (existingUser.getUserImgUrl() != null && !existingUser.getUserImgUrl().isEmpty()) {
                // 图片在MinIO中的路径格式为user-{userId}
                String fileKey = "user-" + userId;
                log.info("删除用户的头像图片: {}", fileKey);
                
                try {
                    // 调用文件服务删除图片
                    fileService.deleteFile(fileKey);
                    log.info("用户头像删除成功: {}", fileKey);
                } catch (Exception e) {
                    // 即使删除图片失败，也继续删除用户数据
                    log.error("删除用户头像失败: {}, 但仍将继续删除用户", fileKey, e);
                }
            }
            
            // 删除用户
            userMapper.delete(userId);
            log.info("用户数据删除成功 - ID: {}", userId);
            return true;
        } catch (Exception e) {
            log.error("删除用户过程中发生错误: {}", e.getMessage(), e);
            throw e; // 由于@Transactional注解，抛出异常会导致事务回滚
        }
    }
    
    @Override
    public List<String> getAllRoles() {
        return Arrays.asList("ADMIN", "CATEGORY_MANAGER", "USER");
    }
}
