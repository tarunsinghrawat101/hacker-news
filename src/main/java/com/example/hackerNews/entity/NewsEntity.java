package com.example.hackerNews.entity;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "news")
public class NewsEntity {
    @Id
    @GeneratedValue
    private Long id;
    private String title;
    private String name;
    @Column(name = "author_email")
    private String authorEmail;
    private String url;
    @Column(columnDefinition = "text")
    private String content;
    private String createdAt;
    private Boolean hide;
    private Boolean likeByUser;
    private Integer pointsPerPost;
    private Boolean isHidden;
    private Boolean isFavorite;
    @Column(name = "admin_deleted",
            nullable = false,
            columnDefinition = "boolean default false")
    private boolean adminDeleted = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "story_state", length = 32)
    private StoryState state = StoryState.PUBLISHED;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(name="user_like",
            joinColumns = @JoinColumn(name = "news_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<User> userLikes = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinTable(name="user_hide",
            joinColumns = @JoinColumn(name = "news_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<User> hiddenNews = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinTable(name="user_favorite",
            joinColumns = @JoinColumn(name = "news_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<User> favoriteNews = new ArrayList<>();

    public boolean isAdminDeleted() { return adminDeleted; }

    public void setAdminDeleted(boolean adminDeleted) { this.adminDeleted = adminDeleted; }

    public StoryState getState() {
        return state == null ? StoryState.PUBLISHED : state;
    }

    public void setState(StoryState state) {
        this.state = state == null ? StoryState.PUBLISHED : state;
    }

    public Boolean getHide() {
        return hide;
    }

    public void setHide(Boolean hide) {
        this.hide = hide;
    }

    public NewsEntity() {
    }

    public NewsEntity(Long id, String title, String name, String url, String content, String createdAt, Boolean hide, Boolean likeByUser, Integer pointsPerPost, Boolean isHidden, Boolean isFavorite, List<User> userLikes, List<User> hiddenNews, List<User> favoriteNews) {
        this.id = id;
        this.title = title;
        this.name = name;
        this.url = url;
        this.content = content;
        this.createdAt = createdAt;
        this.hide = hide;
        this.likeByUser = likeByUser;
        this.pointsPerPost = pointsPerPost;
        this.isHidden = isHidden;
        this.isFavorite = isFavorite;
        this.userLikes = userLikes;
        this.hiddenNews = hiddenNews;
        this.favoriteNews = favoriteNews;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getLikeByUser() {
        return likeByUser;
    }

    public void setLikeByUser(Boolean likeByUser) {
        this.likeByUser = likeByUser;
    }

    public List<User> getUserLikes() {
        return userLikes;
    }

    public void setUserLikes(List<User> userLikes) {
        this.userLikes = userLikes;
    }

    public Integer getPointsPerPost() {
        return pointsPerPost;
    }

    public void setPointsPerPost(Integer pointsPerPost) {
        this.pointsPerPost = pointsPerPost;
    }

    public List<User> getHiddenNews() {
        return hiddenNews;
    }

    public void setHiddenNews(List<User> hiddenNews) {
        this.hiddenNews = hiddenNews;
    }

    public List<User> getFavoriteNews() {
        return favoriteNews;
    }

    public void setFavoriteNews(List<User> favoriteNews) {
        this.favoriteNews = favoriteNews;
    }

    public Boolean getHidden() {
        return isHidden;
    }

    public void setHidden(Boolean hidden) {
        isHidden = hidden;
    }

    public Boolean getFavorite() {
        return isFavorite;
    }

    public void setFavorite(Boolean favorite) {
        isFavorite = favorite;
    }
}