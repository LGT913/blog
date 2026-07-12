package com.blog.blog.repository;

import com.blog.blog.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 查询所有公告，按创建时间倒序排列
    List<Notice> findAllByOrderByCreateTimeDesc();
}
