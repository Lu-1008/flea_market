package com.market.product.dao;

import com.market.product.entity.Item;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ItemMapper {
    /**
     * 插入商品
     */
    @Insert("INSERT INTO items (title, description, price, user_id, category_id, status, valid_until, item_image_url) " +
            "VALUES (#{title}, #{description}, #{price}, #{userId}, #{categoryId}, #{status}, #{validUntil}, #{itemImageUrl})")
    @Options(useGeneratedKeys = true, keyProperty = "itemId")
    void insert(Item item);
    
    /**
     * 根据ID查询商品

     */
    @Select("SELECT i.*, c.name as category_name, u.username " +
            "FROM items i " +
            "LEFT JOIN categories c ON i.category_id = c.category_id " +
            "LEFT JOIN users u ON i.user_id = u.user_id " +
            "WHERE i.item_id = #{itemId}")
    @Results({
        @Result(property = "itemId", column = "item_id"),
        @Result(property = "title", column = "title"),
        @Result(property = "description", column = "description"),
        @Result(property = "price", column = "price"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "categoryId", column = "category_id"),
        @Result(property = "status", column = "status"),
        @Result(property = "validUntil", column = "valid_until"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "itemImageUrl", column = "item_image_url"),
        @Result(property = "categoryName", column = "category_name"),
        @Result(property = "username", column = "username")
    })
    Item findById(Integer itemId);
    
    /**
     * 更新商品
     */
    @Update("UPDATE items SET title = #{title}, description = #{description}, price = #{price}, " +
            "category_id = #{categoryId}, status = #{status}, valid_until = #{validUntil}, " +
            "item_image_url = #{itemImageUrl} WHERE item_id = #{itemId}")
    void update(Item item);
    
    /**
     * 删除商品
     */
    @Delete("DELETE FROM items WHERE item_id = #{itemId}")
    void delete(Integer itemId);
    
    /**
     * 查询商品列表（支持分页和条件筛选）
     */
    @Select("<script>" +
            "SELECT i.*, c.name as category_name, u.username " +
            "FROM items i " +
            "LEFT JOIN categories c ON i.category_id = c.category_id " +
            "LEFT JOIN users u ON i.user_id = u.user_id " +
            "WHERE 1=1 " +
            "<if test='title != null and title != \"\"'>AND i.title LIKE CONCAT('%', #{title}, '%')</if>" +
            "<if test='categoryId != null'>AND i.category_id = #{categoryId}</if>" +
            "<if test='userId != null'>AND i.user_id = #{userId}</if>" +
            "<if test='status != null and status != \"\"'>AND i.status = #{status}</if>" +
            "ORDER BY i.created_at DESC " +
            "<if test='offset != null and limit != null'>LIMIT #{offset}, #{limit}</if>" +
            "</script>")
    @Results({
        @Result(property = "itemId", column = "item_id"),
        @Result(property = "title", column = "title"),
        @Result(property = "description", column = "description"),
        @Result(property = "price", column = "price"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "categoryId", column = "category_id"),
        @Result(property = "status", column = "status"),
        @Result(property = "validUntil", column = "valid_until"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "itemImageUrl", column = "item_image_url"),
        @Result(property = "categoryName", column = "category_name"),
        @Result(property = "username", column = "username")
    })
    List<Item> findItemList(Map<String, Object> params);

    /**
     * 统计符合条件的商品总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM items i WHERE 1=1 " +
            "<if test='title != null and title != \"\"'>AND i.title LIKE CONCAT('%', #{title}, '%')</if>" +
            "<if test='categoryId != null'>AND i.category_id = #{categoryId}</if>" +
            "<if test='userId != null'>AND i.user_id = #{userId}</if>" +
            "<if test='status != null and status != \"\"'>AND i.status = #{status}</if>" +
            "</script>")
    int countItems(Map<String, Object> params);
    
    /**
     * 更新商品状态
     * @param itemId 商品ID
     * @param status 状态
     */
    @Update("UPDATE items SET status = #{status} WHERE item_id = #{itemId}")
    void updateStatus(@Param("itemId") Integer itemId, @Param("status") String status);
    
    /**
     * 批量更新商品分类
     * @param oldCategoryId 原分类ID
     * @param newCategoryId 新分类ID
     * @return 受影响的记录数
     */
    @Update("UPDATE items SET category_id = #{newCategoryId} WHERE category_id = #{oldCategoryId}")
    int updateCategoryBatch(@Param("oldCategoryId") Integer oldCategoryId, @Param("newCategoryId") Integer newCategoryId);
    
    /**
     * 查询指定分类下的商品
     * @param categoryId 分类ID
     * @return 商品列表
     */
    @Select("SELECT item_id FROM items WHERE category_id = #{categoryId}")
    List<Integer> findItemIdsByCategoryId(@Param("categoryId") Integer categoryId);

    /**
     * 删除商品关联的评价
     * @param itemId 商品ID
     * @return 受影响的记录数
     */
    @Delete("DELETE FROM reviews WHERE item_id = #{itemId}")
    int deleteReviewsByItemId(@Param("itemId") Integer itemId);
}
