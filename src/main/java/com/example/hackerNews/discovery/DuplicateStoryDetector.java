package com.example.hackerNews.discovery;

import java.util.*;

public final class DuplicateStoryDetector {
    public Map<String, List<StoryCandidate>> findDuplicates(Collection<StoryCandidate> stories) {
        Map<String, List<StoryCandidate>> grouped = new TreeMap<>();

        if (stories == null)
            return grouped;

        for (StoryCandidate story : stories) {
            String key = UrlCanonicalizer.canonicalize(story.getUrl());
            if (!key.isEmpty())
                grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(story);
        }

        grouped.entrySet().removeIf(entry -> entry.getValue().size() < 2);

        Map<String, List<StoryCandidate>> result = new LinkedHashMap<>();
        grouped.forEach((key, value) -> result.put(key, Collections.unmodifiableList(value)));

        return Collections.unmodifiableMap(result);
    }
}
