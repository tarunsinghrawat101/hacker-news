package com.example.hackerNews.service;

import com.example.hackerNews.entity.StoryEvent;
import com.example.hackerNews.entity.StoryState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StoryStateMachineTest {
    private final StoryStateMachine machine = new StoryStateMachine();

    @Test
    void supportsTheConfiguredLifecycleTransitions() {
        assertEquals(StoryState.PENDING_REVIEW,
                machine.transition(StoryState.DRAFT, StoryEvent.SUBMIT));
        assertEquals(StoryState.PUBLISHED,
                machine.transition(StoryState.PENDING_REVIEW, StoryEvent.APPROVE));
        assertEquals(StoryState.REJECTED,
                machine.transition(StoryState.PENDING_REVIEW, StoryEvent.REJECT));
        assertEquals(StoryState.PENDING_REVIEW,
                machine.transition(StoryState.REJECTED, StoryEvent.RESUBMIT));
        assertEquals(StoryState.ARCHIVED,
                machine.transition(StoryState.PUBLISHED, StoryEvent.ARCHIVE));
        assertEquals(StoryState.PUBLISHED,
                machine.transition(StoryState.ARCHIVED, StoryEvent.RESTORE));
    }

    @Test
    void adminStateOverridesAreExplicitEvents() {
        for (StoryState from : StoryState.values()) {
            for (StoryState to : StoryState.values()) {
                if (from != to) {
                    assertEquals(to, machine.transition(from, machine.adminEventFor(to)));
                }
            }
        }
    }

    @Test
    void rejectsTransitionsThatAreNotConfigured() {
        assertThrows(IllegalStateException.class,
                () -> machine.transition(StoryState.PUBLISHED, StoryEvent.APPROVE));
        assertThrows(IllegalStateException.class,
                () -> machine.transition(StoryState.ARCHIVED, StoryEvent.ARCHIVE));
    }

    @Test
    void rejectsNullStateOrEvent() {
        assertThrows(IllegalArgumentException.class,
                () -> machine.transition(null, StoryEvent.SUBMIT));
        assertThrows(IllegalArgumentException.class,
                () -> machine.transition(StoryState.DRAFT, null));
    }
}
