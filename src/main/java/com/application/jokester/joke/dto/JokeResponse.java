package com.application.jokester.joke.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JokeResponse implements Serializable {

    private UUID id;
    private String title;
    private String content;
    private String category;
    private String postedBy;
    private int upvotes;
    private int downvotes;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}