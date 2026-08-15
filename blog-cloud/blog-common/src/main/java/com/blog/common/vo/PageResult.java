package com.blog.common.vo;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;

/**
 * 分页结果 VO（避免直接序列化 PageImpl）
 * 用于返回给前端的标准分页数据结构
 */
@Data
public class PageResult<T> {
    /** 当前页数据列表 */
    private List<T> content;

    /** 总记录数 */
    private long totalElements;

    /** 总页数 */
    private int totalPages;

    /** 当前页码（从 0 开始） */
    private int number;

    /** 每页大小 */
    private int size;

    /** 是否是第一页 */
    private boolean first;

    /** 是否是最后一页 */
    private boolean last;

    /** 是否有下一页 */
    private boolean hasNext;

    /** 是否有上一页 */
    private boolean hasPrevious;

    /**
     * 从 Spring Data 的 Page 对象构造 PageResult
     */
    public static <T> PageResult<T> of(Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setContent(page.getContent());
        result.setTotalElements(page.getTotalElements());
        result.setTotalPages(page.getTotalPages());
        result.setNumber(page.getNumber());
        result.setSize(page.getSize());
        result.setFirst(page.isFirst());
        result.setLast(page.isLast());
        result.setHasNext(page.hasNext());
        result.setHasPrevious(page.hasPrevious());
        return result;
    }

    /**
     * 手动构造 PageResult（用于缓存命中时）
     */
    public static <T> PageResult<T> of(List<T> content, long totalElements, int pageNumber, int pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setContent(content);
        result.setTotalElements(totalElements);
        result.setSize(pageSize);
        result.setNumber(pageNumber);

        // 计算总页数
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        result.setTotalPages(totalPages);

        // 计算是否第一页/最后一页
        result.setFirst(pageNumber == 0);
        result.setLast(pageNumber >= totalPages - 1);
        result.setHasNext(pageNumber < totalPages - 1);
        result.setHasPrevious(pageNumber > 0);

        return result;
    }

    // 快速构造空结果（关键词为空时用，避免发 DB 查询）
    public static <T> PageResult<T> empty(int page, int size) {
        PageResult<T> result = new PageResult<>();
        result.setContent(Collections.emptyList());
        result.setNumber(page);
        result.setSize(size);
        result.setTotalElements(0);
        result.setTotalPages(0);
        result.setFirst(true);
        result.setLast(true);
        result.setHasNext(false);
        result.setHasPrevious(false);
        return result;
    }
}