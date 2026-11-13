package com.example.hackerNews.discovery;

public final class StoryCandidate {
    private final Long id;
    private final String title;
    private final String url;
    private final String content;
    private final String createdAt;
    private final int votes;
    private final int comments;

    public StoryCandidate(Long id, String title, String url, String content,
                          String createdAt, int votes, int comments) {
        this.id = id;
        this.title = title == null ? "" : title;
        this.url = url == null ? "" : url;
        this.content = content == null ? "" : content;
        this.createdAt = createdAt == null ? "" : createdAt;
        this.votes = Math.max(0, votes);
        this.comments = Math.max(0, comments);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getContent() {
        return content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public int getVotes() {
        return votes;
    }

    public int getComments() {
        return comments;
    }
}
