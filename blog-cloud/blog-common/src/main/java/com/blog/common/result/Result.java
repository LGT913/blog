package com.blog.common.result;

public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    // 无参构造：Jackson 反序列化需要
    private Result() {
    }

    // 核心：私有构造，禁止外部直接 new
    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 响应成功，携带数据
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }
    // 响应成功，无返回数据
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    // 失败：默认500，自定义提示
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
    // 失败：自定义状态码 + 提示（401/403/自定义业务码）
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    // getter 必须保留，JSON序列化需要
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}