package com.blog.blog.common;

public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    // 私有构造方法，不允许外部直接new
    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 成功时调用
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "成功", data);
    }

    // 失败时调用
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }


    // getter 方法（必须有，否则JSON序列化失败）
    public Integer getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}
