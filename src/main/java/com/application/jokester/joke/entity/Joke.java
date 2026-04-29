package com.application.jokester.joke.entity;

import com.application.jokester.auth.entity.User;
import com.application.jokester.category.entity.Category;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "jokes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Joke {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private int upvotes;

    @Column(nullable = false)
    private int downvotes;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime created_at;
}