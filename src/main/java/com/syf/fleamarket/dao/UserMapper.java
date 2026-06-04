package com.syf.fleamarket.dao;

import com.syf.fleamarket.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {
    @Insert("INSERT INTO users (username, password, email, phone, role, user_image_url, address) " +
            "VALUES (#{username}, #{password}, #{email}, #{phone}, #{role}, #{userImgUrl}, #{address})")
    @Options(useGeneratedKeys = true, keyProperty = "userId")
    void insert(User user);

    @Select("SELECT * FROM users WHERE user_id = #{id}")
    @Results({
        @Result(property = "userId", column = "user_id"),
        @Result(property = "username", column = "username"),
        @Result(property = "password", column = "password"),
        @Result(property = "email", column = "email"),
        @Result(property = "phone", column = "phone"),
        @Result(property = "role", column = "role"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "userImgUrl", column = "user_image_url"),
        @Result(property = "averageRating", column = "avg_rating"),
        @Result(property = "address", column = "address")
    })
    User findById(Integer id);
    
    @Select("SELECT * FROM users WHERE username = #{username}")
    @Results({
        @Result(property = "userId", column = "user_id"),
        @Result(property = "username", column = "username"),
        @Result(property = "password", column = "password"),
        @Result(property = "email", column = "email"),
        @Result(property = "phone", column = "phone"),
        @Result(property = "role", column = "role"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "userImgUrl", column = "user_image_url"),
        @Result(property = "averageRating", column = "avg_rating"),
        @Result(property = "address", column = "address")
    })
    User findByUsername(String username);
    
    @Select("SELECT * FROM users WHERE email = #{email}")
    @Results({
        @Result(property = "userId", column = "user_id"),
        @Result(property = "username", column = "username"),
        @Result(property = "password", column = "password"),
        @Result(property = "email", column = "email"),
        @Result(property = "phone", column = "phone"),
        @Result(property = "role", column = "role"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "userImgUrl", column = "user_image_url"),
        @Result(property = "averageRating", column = "avg_rating"),
        @Result(property = "address", column = "address")
    })
    User findByEmail(String email);

    @Update("UPDATE users SET " +
            "username=#{username}, " +
            "password=#{password}, " +
            "email=#{email}, " +
            "phone=#{phone}, " +
            "role=#{role}, " +
            "user_image_url=#{userImgUrl}, " +
            "address=#{address} " +
            "WHERE user_id=#{userId}")
    void update(User user);

    @Delete("DELETE FROM users WHERE user_id = #{id}")
    void delete(Integer id);
    
    /**
     * 查询用户列表（支持分页和条件筛选）
     */
    @Results({
        @Result(property = "userId", column = "user_id"),
        @Result(property = "username", column = "username"),
        @Result(property = "password", column = "password"),
        @Result(property = "email", column = "email"),
        @Result(property = "phone", column = "phone"),
        @Result(property = "role", column = "role"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "userImgUrl", column = "user_image_url"),
        @Result(property = "averageRating", column = "avg_rating"),
        @Result(property = "address", column = "address")
    })
    List<User> findUserList(Map<String, Object> params);
    
    /**
     * 统计符合条件的用户总数
     */
    int countUsers(Map<String, Object> params);

    /**
     * 更新用户信息
     * @param user 用户对象
     */
    @Update("UPDATE users SET " +
            "username=#{username}, " +
            "password=#{password}, " +
            "email=#{email}, " +
            "phone=#{phone}, " +
            "role=#{role}, " +
            "user_image_url=#{userImgUrl}, " +
            "address=#{address} " +
            "WHERE user_id=#{userId}")
    void updateUser(User user);
}
