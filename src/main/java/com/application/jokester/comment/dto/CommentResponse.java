package com.application.jokester.comment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private UUID id;
    private String content;
    private String postedBy;
    private int likesCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // True when this comment is a reply to another comment.
    private boolean isReply;

    // Populated only for top-level comments — null for replies to keep response flat.
    private List<CommentResponse> replies;

    // Number of replies — useful for UI to show reply count without loading all replies.
    private int replyCount;
}