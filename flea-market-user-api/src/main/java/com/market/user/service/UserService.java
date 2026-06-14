package com.market.user.service;

import com.market.user.entity.User;
import com.market.user.entity.UserRole;

import java.util.List;
import java.util.Map;

public interface UserService {
    User authenticate(String username, String password);
    User findByUsername(String username);
    
    /**
     * 修改用户密码

     */
    boolean changePassword(Integer userId, String oldPassword, String newPassword);
    
    /**
     * 获取用户列表（分页）

     */
    Map<String, Object> getUserList(Map<String, Object> params);
    
    /**
     * 根据ID查询用户

     */
    User findById(Integer userId);
    
    /**
     * 创建新用户

     */
    User createUser(User user);
    
    /**
     * 更新用户信息

     */
    User updateUser(User user);
    
    /**
     * 删除用户

     */
    boolean deleteUser(Integer userId);
    
    /**
     * 获取所有可用的用户角色
     */
    List<String> getAllRoles();
}
