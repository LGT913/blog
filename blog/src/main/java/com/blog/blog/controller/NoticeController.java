package com.blog.blog.controller;

import com.blog.blog.common.Result;
import com.blog.blog.entity.Notice;
import com.blog.blog.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notice")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    @GetMapping("/list")
    public Result<List<Notice>> getAllNotices() {
        List<Notice> notices = noticeService.getAllNotices();
        return Result.success(notices);
    }

    @PostMapping("/create")
    public Result<Notice> create(@RequestBody Notice notice) {
        Notice createdNotice = noticeService.createNotice(notice.getTitle(), notice.getContent());
        return Result.success(createdNotice);
    }

    @PutMapping("/update/{id}")
    public Result<Notice> update(@PathVariable Long id, @RequestBody Notice notice) {
        Notice updatedNotice = noticeService.updateNotice(id, notice.getTitle(), notice.getContent());
        return Result.success(updatedNotice);
    }

    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return Result.success("删除成功");
    }
}
