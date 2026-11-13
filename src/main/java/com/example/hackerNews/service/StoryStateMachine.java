package com.example.hackerNews.service;

import com.example.hackerNews.entity.StoryEvent;
import com.example.hackerNews.entity.StoryState;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/** Central, deterministic transition table for story lifecycle changes. */
@Component
public class StoryStateMachine {
    private final Map<StoryState, Map<StoryEvent, StoryState>> transitions =
            new EnumMap<>(StoryState.class);

    public StoryStateMachine() {
        add(StoryState.DRAFT, StoryEvent.SUBMIT, StoryState.PENDING_REVIEW);
        add(StoryState.PENDING_REVIEW, StoryEvent.APPROVE, StoryState.PUBLISHED);
        add(StoryState.PENDING_REVIEW, StoryEvent.REJECT, StoryState.REJECTED);
        add(StoryState.REJECTED, StoryEvent.RESUBMIT, StoryState.PENDING_REVIEW);
        add(StoryState.PUBLISHED, StoryEvent.ARCHIVE, StoryState.ARCHIVED);
        add(StoryState.ARCHIVED, StoryEvent.RESTORE, StoryState.PUBLISHED);

        // Administrator moderation controls use explicit state-machine events.
        // They never bypass the transition service's role checks.
        for (StoryState from : StoryState.values()) {
            for (StoryState to : StoryState.values()) {
                if (from != to) {
                    add(from, adminEventFor(to), to);
                }
            }
        }
    }

    public StoryEvent adminEventFor(StoryState target) {
        if (target == null) throw new IllegalArgumentException("Target state is required");
        switch (target) {
            case DRAFT: return StoryEvent.ADMIN_TO_DRAFT;
            case PENDING_REVIEW: return StoryEvent.ADMIN_TO_REVIEW;
            case PUBLISHED: return StoryEvent.ADMIN_TO_PUBLISHED;
            case REJECTED: return StoryEvent.ADMIN_TO_REJECTED;
            case ARCHIVED: return StoryEvent.ADMIN_TO_ARCHIVED;
            default: throw new IllegalArgumentException("Unsupported state: " + target);
        }
    }

    private void add(StoryState from, StoryEvent event, StoryState to) {
        transitions.computeIfAbsent(from, key -> new EnumMap<>(StoryEvent.class))
                .put(event, to);
    }

    public StoryState transition(StoryState currentState, StoryEvent event) {
        if (currentState == null || event == null) {
            throw new IllegalArgumentException("Current state and event are required");
        }
        Map<StoryEvent, StoryState> allowed = transitions.get(currentState);
        StoryState next = allowed == null ? null : allowed.get(event);
        if (next == null) {
            throw new IllegalStateException("Invalid story transition: " +
                    currentState + " -> " + event);
        }
        return next;
    }
}
