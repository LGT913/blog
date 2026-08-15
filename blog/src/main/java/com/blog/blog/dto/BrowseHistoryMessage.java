package com.blog.blog.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class BrowseHistoryMessage implements Serializable {
    private Long userId;
    private Long articleId;
    private Long timestamp;
}