package com.syf.fleamarket.dao;

import com.syf.fleamarket.entity.Category;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface CategoryMapper {
    /**
     * 插入分类

     */
    @Insert("INSERT INTO categories (name, creator) VALUES (#{name}, #{creator})")
    @Options(useGeneratedKeys = true, keyProperty = "categoryId")
    void insert(Category category);
    
    /**
     * 根据ID查询分类

     */
    @Select("SELECT category_id, name, created_at, creator FROM categories WHERE category_id = #{categoryId}")
    @Results({
        @Result(property = "categoryId", column = "category_id"),
        @Result(property = "name", column = "name"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "creator", column = "creator")
    })
    Category findById(Integer categoryId);
    
    /**
     * 更新分类

     */
    @Update("UPDATE categories SET name = #{name}, creator = #{creator} WHERE category_id = #{categoryId}")
    void update(Category category);
    
    /**
     * 删除分类

     */
    @Delete("DELETE FROM categories WHERE category_id = #{categoryId}")
    void delete(Integer categoryId);
    
    /**
     * 查询所有分类
     */
    @Select("SELECT category_id, name, created_at, creator FROM categories ORDER BY category_id")
    @Results({
        @Result(property = "categoryId", column = "category_id"),
        @Result(property = "name", column = "name"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "creator", column = "creator")
    })
    List<Category> findAll();
    
    /**
     * 查询分类列表（基本查询，无筛选条件，有分页）
     */
    @Select("SELECT category_id, name, created_at, creator FROM categories ORDER BY category_id LIMIT #{offset}, #{limit}")
    @Results({
        @Result(property = "categoryId", column = "category_id"),
        @Result(property = "name", column = "name"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "creator", column = "creator")
    })
    List<Category> findCategoryListWithPaging(Integer offset, Integer limit);
    
    /**
     * 按名称查询分类列表（有分页）

     */
    @Select("SELECT category_id, name, created_at, creator FROM categories WHERE name LIKE CONCAT('%', #{name}, '%') ORDER BY category_id LIMIT #{offset}, #{limit}")
    @Results({
        @Result(property = "categoryId", column = "category_id"),
        @Result(property = "name", column = "name"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "creator", column = "creator")
    })
    List<Category> findCategoryListByNameWithPaging(String name, Integer offset, Integer limit);
    
    /**
     * 按创建者查询分类列表（有分页）

     */
    @Select("SELECT category_id, name, created_at, creator FROM categories WHERE creator = #{creator} ORDER BY category_id LIMIT #{offset}, #{limit}")
    @Results({
        @Result(property = "categoryId", column = "category_id"),
        @Result(property = "name", column = "name"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "creator", column = "creator")
    })
    List<Category> findCategoryListByCreatorWithPaging(String creator, Integer offset, Integer limit);
    
    /**
     * 按名称和创建者查询分类列表（有分页）

     */
    @Select("SELECT category_id, name, created_at, creator FROM categories WHERE name LIKE CONCAT('%', #{name}, '%') AND creator = #{creator} ORDER BY category_id LIMIT #{offset}, #{limit}")
    @Results({
        @Result(property = "categoryId", column = "category_id"),
        @Result(property = "name", column = "name"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "creator", column = "creator")
    })
    List<Category> findCategoryListByNameAndCreatorWithPaging(String name, String creator, Integer offset, Integer limit);
    
    /**
     * 统计所有分类总数

     */
    @Select("SELECT COUNT(*) FROM categories")
    int countAllCategories();
    
    /**
     * 按名称统计分类总数

     */
    @Select("SELECT COUNT(*) FROM categories WHERE name LIKE CONCAT('%', #{name}, '%')")
    int countCategoriesByName(String name);
    
    /**
     * 按创建者统计分类总数

     */
    @Select("SELECT COUNT(*) FROM categories WHERE creator = #{creator}")
    int countCategoriesByCreator(String creator);
    
    /**
     * 按名称和创建者统计分类总数

     */
    @Select("SELECT COUNT(*) FROM categories WHERE name LIKE CONCAT('%', #{name}, '%') AND creator = #{creator}")
    int countCategoriesByNameAndCreator(String name, String creator);
    
    /**
     * 查询分类列表（向下兼容原来的方法）

     */
    default List<Category> findCategoryList(Map<String, Object> params) {
        String name = params.get("name") != null ? params.get("name").toString() : null;
        String creator = params.get("creator") != null ? params.get("creator").toString() : null;
        Integer offset = params.get("offset") != null ? Integer.parseInt(params.get("offset").toString()) : 0;
        Integer limit = params.get("limit") != null ? Integer.parseInt(params.get("limit").toString()) : 10;
        
        boolean hasName = name != null && !name.isEmpty();
        boolean hasCreator = creator != null && !creator.isEmpty();
        
        if (hasName && hasCreator) {
            return findCategoryListByNameAndCreatorWithPaging(name, creator, offset, limit);
        } else if (hasName) {
            return findCategoryListByNameWithPaging(name, offset, limit);
        } else if (hasCreator) {
            return findCategoryListByCreatorWithPaging(creator, offset, limit);
        } else {
            return findCategoryListWithPaging(offset, limit);
        }
    }
    
    /**
     * 统计符合条件的分类总数（向下兼容原来的方法）
     */
    default int countCategories(Map<String, Object> params) {
        String name = params.get("name") != null ? params.get("name").toString() : null;
        String creator = params.get("creator") != null ? params.get("creator").toString() : null;
        
        boolean hasName = name != null && !name.isEmpty();
        boolean hasCreator = creator != null && !creator.isEmpty();
        
        if (hasName && hasCreator) {
            return countCategoriesByNameAndCreator(name, creator);
        } else if (hasName) {
            return countCategoriesByName(name);
        } else if (hasCreator) {
            return countCategoriesByCreator(creator);
        } else {
            return countAllCategories();
        }
    }
} 