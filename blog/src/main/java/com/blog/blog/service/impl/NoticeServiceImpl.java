package com.blog.blog.service.impl;

import com.blog.blog.common.RedisUtil;
import com.blog.blog.entity.Notice;
import com.blog.blog.repository.NoticeRepository;
import com.blog.blog.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NoticeServiceImpl implements NoticeService {

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private RedisUtil redisUtil;

    // Redis key 常量
    private static final String NOTICE_LIST_KEY = "notice:list";
    private static final long NOTICE_LIST_EXPIRE = 600L;

    @Override
    public List<Notice> getAllNotices() {
        // 1. 先从 Redis 查
        Object cacheObj = redisUtil.get(NOTICE_LIST_KEY);
        if (!ObjectUtils.isEmpty(cacheObj)) {
            return (List<Notice>) cacheObj;
        }

        // 2. 缓存未命中，从数据库查
        List<Notice> notices = noticeRepository.findAllByOrderByCreateTimeDesc();

        // 3. 写入 Redis，过期时间 10 分钟
        redisUtil.set(NOTICE_LIST_KEY, notices, NOTICE_LIST_EXPIRE);
        return notices;
    }

    @Override
    public Notice createNotice(String title, String content) {
        Notice notice = new Notice();
        notice.setTitle(title);
        notice.setContent(content);
        notice.setCreateTime(LocalDateTime.now());
        notice.setUpdateTime(LocalDateTime.now());
        notice = noticeRepository.save(notice);

        // 新增数据 → 只删除列表缓存
        redisUtil.delete(NOTICE_LIST_KEY);

        return notice;
    }

    @Override
    public Notice updateNotice(Long id, String title, String content) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("公告不存在"));
        notice.setTitle(title);
        notice.setContent(content);
        notice.setUpdateTime(LocalDateTime.now());
        notice = noticeRepository.save(notice);

        // 更新数据 → 只删除列表缓存
        redisUtil.delete(NOTICE_LIST_KEY);

        return notice;
    }

    @Override
    public void deleteNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("公告不存在"));
        noticeRepository.delete(notice);

        // 删除数据 → 清理列表缓存
        redisUtil.delete(NOTICE_LIST_KEY);
    }
}
