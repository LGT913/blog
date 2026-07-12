package com.blog.blog.service;

import com.blog.blog.entity.Notice;

import java.util.List;

public interface NoticeService {

    // 查询公告列表（走缓存）
    List<Notice> getAllNotices();

    // 新增公告（创建后删除缓存）
    Notice createNotice(String title, String content);

    // 修改公告（更新后删除缓存）
    Notice updateNotice(Long id, String title, String content);

    // 删除公告（删除后清理缓存）
    void deleteNotice(Long id);
}
