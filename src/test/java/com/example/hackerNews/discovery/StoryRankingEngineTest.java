package com.example.hackerNews.discovery;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StoryRankingEngineTest {
    private final StoryRankingEngine engine = new StoryRankingEngine();

    @Test
    void ranksRelevantAndEngagingRecentStoryHigher() {
        Instant now = Instant.parse("2026-11-11T00:00:00Z");
        StoryCandidate relevant = new StoryCandidate(1L, "Spring Boot retry patterns",
                "https://example.com/spring", "A detailed Spring Boot guide with practical examples.",
                now.minusSeconds(3600).toString(), 25, 12);
        StoryCandidate old = new StoryCandidate(2L, "Gardening tips",
                "https://example.com/garden", "Short note.", now.minusSeconds(86400L * 30).toString(), 0, 0);

        List<RankedStory> result = engine.rank(Arrays.asList(old, relevant), "spring boot", now);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getStory().getId());
        assertTrue(result.get(0).getScore() >= 0 && result.get(0).getScore() <= 100);
    }

    @Test
    void handlesEmptyInputAndMissingDates() {
        assertTrue(engine.rank(null, null, Instant.now()).isEmpty());
        StoryCandidate undated = new StoryCandidate(3L, "A useful title", "", "", "", 0, 0);
        assertDoesNotThrow(() -> engine.rank(Arrays.asList(undated), "", Instant.now()));
    }

    @Test
    void scoreIsDeterministicForSameInputs() {
        Instant now = Instant.parse("2026-11-11T00:00:00Z");
        StoryCandidate story = new StoryCandidate(4L, "Java testing", "https://example.com",
                "Testing services", now.toString(), 4, 2);
        assertEquals(engine.rank(Arrays.asList(story), "java", now).get(0).getScore(),
                engine.rank(Arrays.asList(story), "java", now).get(0).getScore());
    }
}
