package com.blog.blog.controller;

import com.blog.blog.common.constant.BlogCacheConstants;
import com.blog.blog.common.result.Result;
import com.blog.blog.common.security.UserPrincipal;
import com.blog.blog.common.util.RedisUtil;
import com.blog.blog.entity.Article;
import com.blog.blog.entity.BrowseHistory;
import com.blog.blog.repository.ArticleRepository;
import com.blog.blog.repository.BrowseHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserHistoryController {

    private final RedisUtil redisUtil;
    private final ArticleRepository articleRepository;
    private final BrowseHistoryRepository browseHistoryRepository;

    @GetMapping("/history")
    public Result<List<Article>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long userId = principal.getUserId();
        String historyKey = BlogCacheConstants.USER_HISTORY_KEY + userId;

        // 优先从 Redis 读取
        List<Object> articleIds = redisUtil.lRange(historyKey, page * size, (page + 1) * size - 1);
        if (articleIds != null && !articleIds.isEmpty()) {
            List<Long> ids = articleIds.stream()
                    .map(id -> Long.valueOf(id.toString()))
                    .collect(Collectors.toList());
            Map<Long, Article> orderedMap = new LinkedHashMap<>();
            for (Long id : ids) {
                articleRepository.findById(id).ifPresent(a -> orderedMap.put(id, a));
            }
            return Result.success(new ArrayList<>(orderedMap.values()));
        }

        // Redis 无数据 → 回源 DB
        Page<BrowseHistory> historyPage = browseHistoryRepository
                .findByUserIdOrderByBrowseTimeDesc(userId, PageRequest.of(page, size));
        List<Article> articles = historyPage.getContent().stream()
                .map(h -> articleRepository.findById(h.getArticleId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return Result.success(articles);
    }
}