package com.blog.blog.controller;

import com.blog.blog.common.constant.BlogCacheConstants;
import com.blog.blog.common.result.Result;
import com.blog.blog.common.security.UserPrincipal;
import com.blog.blog.common.util.RedisUtil;
import com.blog.blog.config.RabbitMQConfig;
import com.blog.blog.dto.LikeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/like")
@RequiredArgsConstructor
public class LikeController {

    private static final Logger log = LoggerFactory.getLogger(LikeController.class);
    private final RedisUtil redisUtil;
    private final RabbitTemplate rabbitTemplate;

     //点赞/取消点赞
     //action: like | unlike
    @PostMapping("/article/{articleId}")
    public Result<Map<String, Object>> likeArticle(
            @PathVariable Long articleId,
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) String action,
            @AuthenticationPrincipal UserPrincipal principal) {

        // action 兼容两种传参：优先 query，其次 JSON body（前端 likeApi.toggle 当前走 body）
        String finalAction = action;
        if (finalAction == null || finalAction.isEmpty()) {
            finalAction = (body != null) ? body.getOrDefault("action", "like") : "like";
        }

        Long userId = principal.getUserId();
        String userSetKey = BlogCacheConstants.ARTICLE_LIKE_USER_SET_KEY + articleId;
        String countKey = BlogCacheConstants.ARTICLE_LIKE_COUNT_KEY + articleId;

        // 判断当前状态
        boolean isLiked = Boolean.TRUE.equals(redisUtil.sismember(userSetKey, userId));

        // 幂等：已点赞再点"like" 或 未点赞再点"unlike" → 直接返回当前状态
        if (("like".equals(finalAction) && isLiked) || ("unlike".equals(finalAction) && !isLiked)) {
            Object count = redisUtil.get(countKey);
            int likeCount = count instanceof Number ? ((Number) count).intValue() : 0;
            Map<String, Object> result = new HashMap<>();
            result.put("liked", isLiked);
            result.put("likeCount", likeCount);
            return Result.success(result);
        }

        // 执行点赞/取消点赞
        int delta;
        if ("like".equals(finalAction)) {
            redisUtil.sadd(userSetKey, userId);
            redisUtil.increment(countKey);
            delta = 1;
        } else {
            redisUtil.srem(userSetKey, userId);
            redisUtil.incrementBy(countKey, -1);
            delta = -1;
        }
        redisUtil.sadd(BlogCacheConstants.ARTICLE_LIKE_DIRTY_KEY, articleId);

        // 发送 MQ 异步持久化（MQ 不可用时降级，不影响点赞主流程；定时任务 syncLikeCountToDB 会兜底同步）
        try {
            LikeMessage msg = new LikeMessage();
            msg.setArticleId(articleId);
            msg.setUserId(userId);
            msg.setDelta(delta);
            msg.setTimestamp(System.currentTimeMillis());
            rabbitTemplate.convertAndSend(RabbitMQConfig.LIKE_EXCHANGE,
                    RabbitMQConfig.LIKE_ROUTING_KEY, msg);
        } catch (Exception e) {
            log.warn("发送点赞消息失败,articleId={},userId={},MQ可能未启动，稍后由定时任务兜底同步", articleId, userId, e);
        }

        // 返回最新状态
        Object count = redisUtil.get(countKey);
        int likeCount = count instanceof Number ? ((Number) count).intValue() : 0;
        Map<String, Object> result = new HashMap<>();
        result.put("liked", "like".equals(finalAction));
        result.put("likeCount", likeCount);
        return Result.success(result);
    }

    // 查询当前用户对某篇文章的点赞状态
    @GetMapping("/status/{articleId}")
    public Result<Map<String, Object>> getLikeStatus(
            @PathVariable Long articleId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getUserId();
        String userSetKey = BlogCacheConstants.ARTICLE_LIKE_USER_SET_KEY + articleId;
        String countKey = BlogCacheConstants.ARTICLE_LIKE_COUNT_KEY + articleId;

        boolean liked = Boolean.TRUE.equals(redisUtil.sismember(userSetKey, userId));
        Object count = redisUtil.get(countKey);
        int likeCount = count instanceof Number ? ((Number) count).intValue() : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", likeCount);
        return Result.success(result);
    }
}
