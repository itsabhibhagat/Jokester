package com.application.jokester.joke.dto;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class JokePageResponse implements Serializable {
    private List<JokeResponse> jokes;
    private int size;
    private boolean hasMore;
    private Integer lastUpvotes;
    private UUID lastId;
}