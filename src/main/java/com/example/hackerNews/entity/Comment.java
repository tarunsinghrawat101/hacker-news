package com.example.hackerNews.entity;

import javax.persistence.*;

@Entity
@Table(name = "Comment")
public class Comment {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private String email;
    private String comment;
    private Long newsId;
    private String createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Long getNewsId() {
        return newsId;
    }

    public void setNewsId(Long newsId) {
        this.newsId = newsId;
    }

    public Comment(String name, String email, String comment, Long newsId, String createdAt) {
        this.name = name;
        this.email = email;
        this.comment = comment;
        this.newsId = newsId;
        this.createdAt = createdAt;
    }

    public Comment() {
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

