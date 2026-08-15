package com.blog.blog.repository;

import com.blog.blog.entity.BrowseHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrowseHistoryRepository extends JpaRepository<BrowseHistory, Long> {
    Page<BrowseHistory> findByUserIdOrderByBrowseTimeDesc(Long userId, Pageable pageable);
    void deleteByUserId(Long userId);
}