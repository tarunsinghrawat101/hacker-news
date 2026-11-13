package com.example.hackerNews.service;

import com.example.hackerNews.entity.*;
import com.example.hackerNews.repository.CommentRepository;
import com.example.hackerNews.repository.NewsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NewsStateManagementTest {
    private final NewsRepository repo = mock(NewsRepository.class);
    private final CommentRepository comments = mock(CommentRepository.class);
    private final NewsServiceImpl service = new NewsServiceImpl(repo, comments, new StoryStateMachine());

    private User user(String email) {
        return new User(email, email, "irrelevant", Collections.singletonList(new Role("ROLE_USER")));
    }

    private NewsEntity story(long id, String email, StoryState state) {
        NewsEntity story = new NewsEntity();
        story.setId(id);
        story.setAuthorEmail(email);
        story.setState(state);
        return story;
    }

    @Test
    void newSubmissionsRequireReviewAndDraftsRemainDrafts() {
        User author = user("member@example.com");
        NewsEntity submitted = new NewsEntity();
        submitted.setContent(String.join(" ", Collections.nCopies(1292, "word")));
        service.create(submitted, author);
        assertEquals(StoryState.PENDING_REVIEW, submitted.getState());
        assertEquals(author.getEmail(), submitted.getAuthorEmail());
        NewsEntity draft = new NewsEntity();
        service.createDraft(draft, author);
        assertEquals(StoryState.DRAFT, draft.getState());
        verify(repo, times(2)).save(any(NewsEntity.class));
    }

    @Test
    void submissionRequiresAtLeast1292WordsButDraftCanBeIncomplete() {
        User author = user("member@example.com");
        NewsEntity shortPost = new NewsEntity();
        shortPost.setContent("only a few words");
        assertThrows(IllegalArgumentException.class, () -> service.create(shortPost, author));
        NewsEntity draft = new NewsEntity();
        draft.setContent("unfinished draft");
        service.createDraft(draft, author);
        assertEquals(StoryState.DRAFT, draft.getState());
    }

    @Test
    void membersCannotUsePrivilegedAdminEvents() {
        NewsEntity story = story(1L, "member@example.com", StoryState.PENDING_REVIEW);
        when(repo.findById(1L)).thenReturn(Optional.of(story));
        assertThrows(AccessDeniedException.class, () ->
                service.transitionState(1L, StoryEvent.ADMIN_TO_PUBLISHED, user("member@example.com"), false));
        assertEquals(StoryState.PENDING_REVIEW, story.getState());
        verify(repo, never()).save(any());
    }

    @Test
    void membersCannotModifyOtherMembersStories() {
        NewsEntity story = story(1L, "other@example.com", StoryState.DRAFT);
        when(repo.findById(1L)).thenReturn(Optional.of(story));
        assertThrows(AccessDeniedException.class, () ->
                service.transitionState(1L, StoryEvent.SUBMIT, user("member@example.com"), false));
        verify(repo, never()).save(any());
    }

    @Test
    void adminRemovalKeepsPostForAuthorProfileAndMarksItDeleted() {
        NewsEntity post = story(7L, "member@example.com", StoryState.REJECTED);
        when(repo.findAllById(any())).thenReturn(Collections.singletonList(post));
        service.adminDeleteStoriesForAuthor("member@example.com", Collections.singletonList(7L),
                user("admin@example.com"), true);
        assertTrue(post.isAdminDeleted());
        verify(repo).saveAll(Collections.singletonList(post));
        verify(repo, never()).deleteAll(anyCollection());
    }

    @Test
    void adminCannotDeleteAnotherMembersPostFromMemberScreen() {
        NewsEntity post = story(8L, "other@example.com", StoryState.REJECTED);
        when(repo.findAllById(any())).thenReturn(Collections.singletonList(post));
        assertThrows(AccessDeniedException.class, () -> service.adminDeleteStoriesForAuthor(
                "member@example.com", Collections.singletonList(8L), user("admin@example.com"), true));
        verify(repo, never()).saveAll(any());
        verify(repo, never()).deleteAll(anyCollection());
    }

    @Test
    void membersCannotChangeStateOfAdminDeletedPosts() {
        NewsEntity post = story(9L, "member@example.com", StoryState.REJECTED);
        post.setAdminDeleted(true);
        when(repo.findById(9L)).thenReturn(Optional.of(post));
        assertThrows(IllegalStateException.class, () -> service.transitionState(
                9L, StoryEvent.RESUBMIT, user("member@example.com"), false));
        assertEquals(StoryState.REJECTED, post.getState());
        verify(repo, never()).save(any());
    }

    @Test
    void bulkModerationRejectsCrossMemberSelectionBeforeAnyChange() {
        NewsEntity first = story(1L, "member@example.com", StoryState.PENDING_REVIEW);
        NewsEntity second = story(2L, "other@example.com", StoryState.PENDING_REVIEW);
        when(repo.findAllById(any())).thenReturn(Arrays.asList(first, second));
        assertThrows(AccessDeniedException.class, () ->
                service.adminSetStateForAuthor("member@example.com", Arrays.asList(1L, 2L),
                        StoryState.PUBLISHED, user("admin@gmail.com"), true));
        assertEquals(StoryState.PENDING_REVIEW, first.getState());
        assertEquals(StoryState.PENDING_REVIEW, second.getState());
        verify(repo, never()).saveAll(any());
    }

    @Test
    void bulkModerationChangesOnlySelectedMemberPosts() {
        NewsEntity first = story(1L, "member@example.com", StoryState.PENDING_REVIEW);
        NewsEntity second = story(2L, "member@example.com", StoryState.REJECTED);
        when(repo.findAllById(any())).thenReturn(Arrays.asList(first, second));
        service.adminSetStateForAuthor("member@example.com", Arrays.asList(1L, 2L),
                StoryState.PUBLISHED, user("admin@gmail.com"), true);
        assertEquals(StoryState.PUBLISHED, first.getState());
        assertEquals(StoryState.PUBLISHED, second.getState());
        verify(repo).saveAll(any());
    }
}
