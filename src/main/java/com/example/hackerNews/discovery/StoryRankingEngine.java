package com.example.hackerNews.discovery;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class StoryRankingEngine {
    private static final DateTimeFormatter LEGACY_DATE =
            DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);

    public List<RankedStory> rank(Collection<StoryCandidate> stories, String topic, Instant now) {
        if (stories == null || stories.isEmpty())
            return Collections.emptyList();

        final String normalizedTopic = normalize(topic);
        List<RankedStory> ranked = new ArrayList<>();

        for (StoryCandidate story : stories) {
            double engagement = Math.min(1.0,
                    Math.log1p(story.getVotes() * 2.0 + story.getComments() * 1.5) / Math.log(101.0));
            double freshness = freshness(story.getCreatedAt(), now);
            double relevance = relevance(story, normalizedTopic);
            double completeness = completeness(story);
            double score = 100.0 * (0.35 * engagement + 0.25 * freshness
                    + 0.25 * relevance + 0.15 * completeness);
            String explanation = String.format(Locale.ROOT,
                    "Engagement %.0f%% · freshness %.0f%% · topic match %.0f%%",
                    engagement * 100, freshness * 100, relevance * 100);
            ranked.add(new RankedStory(story, Math.round(score * 100.0) / 100.0, explanation));
        }
        ranked.sort(Comparator.comparingDouble(RankedStory::getScore).reversed()
                .thenComparing(r -> r.getStory().getId(), Comparator.nullsLast(Comparator.naturalOrder())));

        return Collections.unmodifiableList(ranked);
    }

    private double freshness(String createdAt, Instant now) {
        Instant created = parseDate(createdAt);
        if (created == null || now == null)
            return 0.25;

        double hours = Math.max(0.0, Duration.between(created, now).toMillis() / 3_600_000.0);

        return 1.0 / (1.0 + hours / 24.0);
    }

    private Instant parseDate(String value) {
        if (value == null || value.trim().isEmpty())
            return null;
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) { }
        try {
            return ZonedDateTime.parse(value, LEGACY_DATE).toInstant();
        } catch (DateTimeParseException ignored) { return null; }
    }

    private double relevance(StoryCandidate story, String topic) {
        if (topic.isEmpty())
            return 0.5;
        String haystack = normalize(story.getTitle() + " " + story.getContent() + " " + story.getUrl());

        if (haystack.contains(topic))
            return 1.0;

        String[] terms = topic.split("\\s+");
        long matches = Arrays.stream(terms).filter(t -> !t.isEmpty() && haystack.contains(t)).count();

        return terms.length == 0 ? 0.5 : 0.25 + 0.75 * ((double) matches / terms.length);
    }

    private double completeness(StoryCandidate story) {
        double title = Math.min(1.0, story.getTitle().trim().length() / 50.0);
        double body = Math.min(1.0, story.getContent().trim().length() / 300.0);
        double link = story.getUrl().trim().isEmpty() ? 0.4 : 1.0;
        return 0.45 * title + 0.35 * body + 0.20 * link;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ")
                .trim().replaceAll("\\s+", " ");
    }
}
