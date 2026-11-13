package com.example.hackerNews.service;

import com.example.hackerNews.entity.Comment;
import com.example.hackerNews.entity.NewsEntity;
import com.example.hackerNews.entity.User;
import com.example.hackerNews.entity.StoryEvent;
import com.example.hackerNews.entity.StoryState;
import com.example.hackerNews.repository.CommentRepository;
import com.example.hackerNews.repository.NewsRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class NewsServiceImpl implements NewsService {
    private static final Set<String> SORT_FIELDS = new HashSet<>(Arrays.asList("id", "title", "name", "createdAt"));
    private final NewsRepository newsRepository;
    private final CommentRepository commentRepository;
    private final StoryStateMachine storyStateMachine;

    public NewsServiceImpl(NewsRepository newsRepository,
                           CommentRepository commentRepository,
                           StoryStateMachine storyStateMachine) {
        this.newsRepository = newsRepository;
        this.commentRepository = commentRepository;
        this.storyStateMachine = storyStateMachine;
    }

    @Override
    public NewsEntity get(Long id) {
        return newsRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Story not found: " + id));
    }

    @Override
    public List<NewsEntity> getNews() {
        return newsRepository.findAllInStateOrLegacy(StoryState.PUBLISHED);
    }

    @Override
    public Page<NewsEntity> getPostsWithPagination(int page, int size, String sort, String direction, String keyword, User user) {
        if (page < 1 || size < 1)
            throw new IllegalArgumentException("Page and size must be positive");

        String field = SORT_FIELDS.contains(sort) ? sort : "id";
        Sort order = "asc".equalsIgnoreCase(direction) ? Sort.by(field).ascending() : Sort.by(field).descending();
        PageRequest pageRequest = PageRequest.of(page - 1, size, order);
        Page<NewsEntity> result = keyword == null || keyword.trim().isEmpty()
                ? newsRepository.findAllInStateOrLegacy(StoryState.PUBLISHED, pageRequest)
                : newsRepository.findByKeywordInStateOrLegacy(keyword.trim(), StoryState.PUBLISHED, pageRequest);
        prepare(result.getContent(), user);

        return result;
    }

    @Override
    @Transactional
    public void create(NewsEntity news, User user) {
        saveNewStory(news, user, false);
    }

    @Override
    @Transactional
    public void createDraft(NewsEntity news, User user) {
        saveNewStory(news, user, true);
    }

    private void saveNewStory(NewsEntity news, User user, boolean draft) {
        requireUser(user);
        news.setId(null);
        news.setCreatedAt(new Date().toString());
        news.setName(user.getName());
        news.setAuthorEmail(user.getEmail());
        news.setLikeByUser(false);
        news.setFavorite(false);
        news.setHidden(false);
        news.setUrl(news.getUrl() == null || news.getUrl().trim().isEmpty() ? null : news.getUrl().trim());
        news.setState(StoryState.DRAFT);
        if (!draft) {
            validateSubmissionContent(news.getContent());
            news.setState(storyStateMachine.transition(news.getState(), StoryEvent.SUBMIT));
        }
        newsRepository.save(news);
    }

    @Override
    public List<NewsEntity> getStoriesForAuthor(String email) {
        if (email == null || email.trim().isEmpty())
            throw new IllegalArgumentException("Author required");
        return newsRepository.findByAuthorEmailIgnoreCaseOrderByIdDesc(email);
    }

    @Override
    public List<NewsEntity> getStoriesForAuthor(String email, User viewer) {
        List<NewsEntity> stories = getStoriesForAuthor(email);
        prepare(stories, viewer);
        return stories;
    }

    @Override
    public List<NewsEntity> getUnassignedStories() {
        return newsRepository.findByAuthorEmailIsNullOrderByIdDesc();
    }

    @Override
    @Transactional
    public void adminSetStateForAuthor(String authorEmail, List<Long> postIds,
                                       StoryState targetState, User actingAdmin, boolean admin) {
        if (postIds == null || postIds.isEmpty())
            throw new IllegalArgumentException("Select at least one post");
        Set<Long> uniqueIds = new LinkedHashSet<>(postIds);
        requireUser(actingAdmin);

        if (!admin)
            throw new org.springframework.security.access.AccessDeniedException("Admin access required");
        if (targetState == null)
            throw new IllegalArgumentException("Choose a state");
        if (uniqueIds.contains(null))
            throw new IllegalArgumentException("Invalid post ID");

        List<NewsEntity> selected = newsRepository.findAllById(uniqueIds);

        if (selected.size() != uniqueIds.size())
            throw new IllegalArgumentException("One or more posts no longer exist");

        Map<NewsEntity, StoryState> changes = new LinkedHashMap<>();
        for (NewsEntity story : selected) {
            boolean owned = authorEmail == null ? story.getAuthorEmail() == null
                    : authorEmail.equalsIgnoreCase(story.getAuthorEmail());
            if (!owned)
                throw new org.springframework.security.access.AccessDeniedException(
                    "Selected post does not belong to this member");
            if (story.isAdminDeleted())
                throw new IllegalStateException("An admin-deleted post cannot be changed");
            if (story.getState() != targetState) {
                changes.put(story, storyStateMachine.transition(story.getState(),
                        storyStateMachine.adminEventFor(targetState)));
            }
        }
        changes.forEach(NewsEntity::setState);
        newsRepository.saveAll(changes.keySet());
    }

    @Override
    @Transactional
    public void adminDeleteStoriesForAuthor(String authorEmail, List<Long> postIds, User actingAdmin, boolean admin) {
        requireUser(actingAdmin);
        if (!admin) throw new org.springframework.security.access.AccessDeniedException("Admin access required");
        if (postIds == null || postIds.isEmpty()) throw new IllegalArgumentException("Select at least one post");
        Set<Long> ids = new LinkedHashSet<>(postIds);
        if (ids.contains(null)) throw new IllegalArgumentException("Invalid post ID");
        List<NewsEntity> selected = newsRepository.findAllById(ids);
        if (selected.size() != ids.size()) throw new IllegalArgumentException("One or more posts no longer exist");
        for (NewsEntity story : selected) {
            boolean owned = authorEmail == null ? story.getAuthorEmail() == null
                    : authorEmail.equalsIgnoreCase(story.getAuthorEmail());
            if (!owned) throw new org.springframework.security.access.AccessDeniedException("Selected post does not belong to this member");
            story.setAdminDeleted(true);
        }
        // Keep a tombstone so the author can still see that the administrator removed the post.
        newsRepository.saveAll(selected);
    }

    @Override
    @Transactional
    public void update(NewsEntity submitted, User user, boolean admin, boolean saveAsDraft) {
        NewsEntity existing = get(submitted.getId());
        requireOwner(existing, user, admin);
        if (existing.isAdminDeleted()) throw new IllegalStateException("This post was deleted by an administrator and can no longer be edited");
        String content = submitted.getContent() == null ? "" : submitted.getContent().trim();
        existing.setTitle(submitted.getTitle());
        existing.setUrl(submitted.getUrl() == null || submitted.getUrl().trim().isEmpty() ? null : submitted.getUrl().trim());
        existing.setContent(submitted.getContent());
        if (saveAsDraft) {
            existing.setState(StoryState.DRAFT);
        } else if (existing.getState() == StoryState.DRAFT) {
            validateSubmissionContent(content);
            existing.setState(storyStateMachine.transition(existing.getState(), StoryEvent.SUBMIT));
        }
        newsRepository.save(existing);
    }

    private void validateSubmissionContent(String content) {
        int words = content == null || content.trim().isEmpty() ? 0 : content.trim().split("\\s+").length;
        if (words < 1292) throw new IllegalArgumentException("A post must contain at least 1,292 words before it can be submitted. Current word count: " + words);
    }
    @Override
    @Transactional
    public void delete(Long id, User user, boolean admin) {
        NewsEntity news=get(id);
        requireOwner(news,user,admin);
        if (news.isAdminDeleted()) throw new IllegalStateException("This post was deleted by an administrator");
        newsRepository.delete(news);
    }

    @Override
    @Transactional
    public void transitionState(Long id, StoryEvent event, User user, boolean admin) {
        requireUser(user);
        NewsEntity story = get(id);
        if (story.isAdminDeleted()) throw new IllegalStateException("This post was deleted by an administrator and its state is locked");

        if (event == null)
            throw new IllegalArgumentException("Story event is required");
        if (event == StoryEvent.APPROVE || event == StoryEvent.REJECT ||
                event.name().startsWith("ADMIN_")) {
            if (!admin)
                throw new org.springframework.security.access.AccessDeniedException(
                    "Only administrators may perform this transition");
        } else {
            requireOwner(story, user, admin);
        }

        if (event == StoryEvent.SUBMIT || event == StoryEvent.RESUBMIT) validateSubmissionContent(story.getContent());
        StoryState nextState = storyStateMachine.transition(story.getState(), event);
        story.setState(nextState);
        newsRepository.save(story);
    }

    @Override
    @Transactional
    public void addLike(Long id, User user) {
        requireUser(user);
        NewsEntity n=get(id);
        if(!n.getUserLikes().contains(user))
            n.getUserLikes().add(user);
    }

    @Override
    @Transactional
    public void removeLike(Long id, User user) {
        requireUser(user);
        get(id).getUserLikes().remove(user);
    }

    @Override
    @Transactional
    public void hide(Long id, User user, boolean hidden) {
        requireUser(user);
        NewsEntity n=get(id);

        if(hidden) {
            if(!n.getHiddenNews().contains(user)) n.getHiddenNews().add(user);
        } else
            n.getHiddenNews().remove(user);
    }

    @Override
    @Transactional
    public void favorite(Long id, User user, boolean favorite) {
        requireUser(user);
        NewsEntity n=get(id);

        if(favorite) {
            if(!n.getFavoriteNews().contains(user))
                n.getFavoriteNews().add(user);
        } else
            n.getFavoriteNews().remove(user);
    }

    @Override
    public Map<String, Long> profileStatistics(User user) {
        requireUser(user);
        List<NewsEntity> news=newsRepository.findAll();
        long likes=news.stream().filter(n -> n.getUserLikes().contains(user)).count();
        long posts=news.stream().filter(n -> user.getEmail().equalsIgnoreCase(String.valueOf(n.getAuthorEmail()))).count();

        long comments=commentRepository.countByEmailIgnoreCase(user.getEmail());

        Map<String,Long> stats=new HashMap<>();
        stats.put("newsLike",likes);
        stats.put("newsPosted",posts);
        stats.put("comments",comments);

        return stats;
    }

    @Override public List<NewsEntity> getUserNews(User user, String filter) {
        requireUser(user);
        List<NewsEntity> all=newsRepository.findAll();

        return all.stream().filter(n -> "hidden".equals(filter) ?
                n.getHiddenNews().contains(user) : "favorite".equals(filter) ?
                n.getFavoriteNews().contains(user) : "mine".equals(filter) ?
                user.getEmail().equalsIgnoreCase(String.valueOf(n.getAuthorEmail())) : true)
                .peek(n -> prepareOne(n,user))
                .collect(Collectors.toList());
    }

    private void prepare(List<NewsEntity> news, User user) {
        news.forEach(n -> prepareOne(n,user));
    }

    private void prepareOne(NewsEntity newsEntity, User user) {
        newsEntity.setPointsPerPost(newsEntity.getUserLikes() == null ? 0 : newsEntity.getUserLikes().size());
        newsEntity.setLikeByUser(user != null && newsEntity.getUserLikes().contains(user));
        newsEntity.setHidden(user != null && newsEntity.getHiddenNews().contains(user));
        newsEntity.setFavorite(user != null && newsEntity.getFavoriteNews().contains(user));
    }

    private void requireOwner(NewsEntity news,User user,boolean admin) {
        requireUser(user);
        if(!admin && (news.getAuthorEmail()==null || !news.getAuthorEmail().equalsIgnoreCase(user.getEmail())))
            throw new org.springframework.security.access.AccessDeniedException("You cannot modify this story");
    }

    private void requireUser(User user) {
        if(user==null)
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
    }
}
