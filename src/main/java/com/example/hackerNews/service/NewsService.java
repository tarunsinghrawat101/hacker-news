package com.example.hackerNews.service;

import com.example.hackerNews.entity.NewsEntity;
import com.example.hackerNews.entity.User;
import com.example.hackerNews.entity.StoryEvent;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Map;

public interface NewsService {
    NewsEntity get(Long id);
    List<NewsEntity> getNews();
    Page<NewsEntity> getPostsWithPagination(int page, int size, String sort, String direction, String keyword, User user);
    void create(NewsEntity news, User user);
    void createDraft(NewsEntity news, User user);
    List<NewsEntity> getStoriesForAuthor(String email);
    List<NewsEntity> getStoriesForAuthor(String email, User viewer);
    List<NewsEntity> getUnassignedStories();
    void adminDeleteStoriesForAuthor(String authorEmail, List<Long> postIds, User actingAdmin, boolean admin);
    void adminSetStateForAuthor(String authorEmail, List<Long> postIds,
                                com.example.hackerNews.entity.StoryState targetState,
                                User actingAdmin, boolean admin);

    void update(NewsEntity news, User user, boolean admin, boolean saveAsDraft);
    void delete(Long id, User user, boolean admin);
    void addLike(Long id, User user);
    void removeLike(Long id, User user);
    void hide(Long id, User user, boolean hidden);
    void favorite(Long id, User user, boolean favorite);
    void transitionState(Long id, StoryEvent event, User user, boolean admin);
    Map<String, Long> profileStatistics(User user);
    List<NewsEntity> getUserNews(User user, String filter);
}
