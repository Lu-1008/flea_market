package com.market.common;

import lombok.Data;

/**
 * 统一返回结果类
 * @param <T> 数据类型
 */
@Data
public class Result<T> {
    // 状态码
    private Integer code;
    // 返回信息
    private String message;
    // 返回数据
    private T data;

    private Result() {}

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功返回结果
     * @return 统一返回结果
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    /**
     * 成功返回结果
     * @param data 返回数据
     * @param <T> 数据类型
     * @return 统一返回结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 成功返回结果
     * @param data 返回数据
     * @param message 返回信息
     * @param <T> 数据类型
     * @return 统一返回结果
     */
    public static <T> Result<T> success(T data, String message) {
        return new Result<>(200, message, data);
    }

    /**
     * 失败返回结果
     * @param code 状态码
     * @param message 返回信息
     * @param <T> 数据类型
     * @return 统一返回结果
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 失败返回结果
     * @param message 返回信息
     * @param <T> 数据类型
     * @return 统一返回结果
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }
} 