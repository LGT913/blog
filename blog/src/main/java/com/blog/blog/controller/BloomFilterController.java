package com.blog.blog.controller;

import com.blog.blog.common.BloomFilterUtil;
import com.blog.blog.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/bloom")
@PreAuthorize("hasRole('ADMIN')")   // ← 类级别，所有方法都要管理员
public class BloomFilterController {

    private final BloomFilterUtil bloomFilterUtil;

    public BloomFilterController(BloomFilterUtil bloomFilterUtil) {
        this.bloomFilterUtil = bloomFilterUtil;
    }

    /**
     * 查看布隆过滤器状态
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("count", bloomFilterUtil.getCount());
        stats.put("expectedInsertions", bloomFilterUtil.getExpectedInsertions());
        stats.put("falseProbability", bloomFilterUtil.getFalseProbability());
        return Result.success(stats);
    }

    /**
     * 手动重建布隆过滤器（管理员接口）
     * @param newCapacity 新的预期容量
     */

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/rebuild")
    public Result<String> rebuild(@RequestParam Long newCapacity) {
        if (newCapacity == null || newCapacity < 1000) {
            return Result.error("新容量不能小于1000");
        }
        bloomFilterUtil.rebuild(newCapacity);
        return Result.success("重建完成");
    }

    /**
     * 测试：判断一个 ID 是否存在
     */
    @GetMapping("/check/{id}")
    public Result<Boolean> check(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return Result.success(false);
        }
        boolean result = bloomFilterUtil.mightContain(id);
        return Result.success(result);
    }
}