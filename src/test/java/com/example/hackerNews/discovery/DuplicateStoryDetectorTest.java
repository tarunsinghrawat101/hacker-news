package com.example.hackerNews.discovery;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DuplicateStoryDetectorTest {
    @Test
    void groupsTrackingAndFragmentVariants() {
        StoryCandidate first = new StoryCandidate(1L, "First", "https://www.example.com/post?utm_source=x#top",
                "", "", 0, 0);
        StoryCandidate second = new StoryCandidate(2L, "Second", "https://example.com/post?fbclid=abc",
                "", "", 0, 0);

        Map<String, java.util.List<StoryCandidate>> duplicates =
                new DuplicateStoryDetector().findDuplicates(Arrays.asList(first, second));

        assertEquals(1, duplicates.size());
        assertEquals(2, duplicates.values().iterator().next().size());
    }

    @Test
    void doesNotReportUniqueUrlsAsDuplicates() {
        StoryCandidate first = new StoryCandidate(1L, "First", "https://one.example/a", "", "", 0, 0);
        StoryCandidate second = new StoryCandidate(2L, "Second", "https://two.example/b", "", "", 0, 0);
        assertTrue(new DuplicateStoryDetector().findDuplicates(Arrays.asList(first, second)).isEmpty());
    }
}
