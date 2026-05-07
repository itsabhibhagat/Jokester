package com.application.jokester.comment.entity;

import com.application.jokester.auth.entity.User;
import com.application.jokester.joke.entity.Joke;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "joke_id", nullable = false)
    private Joke joke;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // Null for top-level comments, set for replies.
    // We only allow one level of replies — you cannot reply to a reply.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parent;

    // Replies are loaded explicitly when needed — not eagerly loaded.
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Comment> replies = new ArrayList<>();

    // Maintained atomically via @Modifying queries.
    // Avoids COUNT(comment_likes) on every fetch which would be slow at scale.
    @Column(name = "likes_count", nullable = false)
    @Builder.Default
    private int likesCount = 0;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}