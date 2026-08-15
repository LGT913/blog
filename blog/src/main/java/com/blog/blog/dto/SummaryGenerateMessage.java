package com.blog.blog.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class SummaryGenerateMessage implements Serializable {
    private Long articleId;
    private Long timestamp;
}