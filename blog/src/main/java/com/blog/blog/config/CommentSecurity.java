package com.blog.blog.config;

import com.blog.blog.common.security.UserPrincipal;
import com.blog.blog.entity.Article;
import com.blog.blog.entity.Comment;
import com.blog.blog.repository.ArticleRepository;
import com.blog.blog.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("commentSecurity")
public class CommentSecurity {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ArticleRepository articleRepository;

    public boolean isArticleAuthor(Authentication authentication, Long commentId) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Long userId = principal.getUserId();
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) return false;
        Article article = articleRepository.findById(comment.getArticleId()).orElse(null);
        return article != null && article.getUserId().equals(userId);
    }
}