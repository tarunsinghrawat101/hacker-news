
package com.example.hackerNews.discovery;

import com.example.hackerNews.entity.Comment;
import com.example.hackerNews.entity.NewsEntity;
import com.example.hackerNews.service.CommentService;
import com.example.hackerNews.service.NewsService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DiscoveryServiceImpl implements DiscoveryService {

    private static final DateTimeFormatter LEGACY_DATE = DateTimeFormatter.ofPattern(
                    "EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);

    private final NewsService newsService;
    private final CommentService commentService;

    public DiscoveryServiceImpl(NewsService newsService, CommentService commentService) {
        this.newsService = newsService;
        this.commentService = commentService;
    }

    private final StoryRankingEngine rankingEngine = new StoryRankingEngine();
    private final DuplicateStoryDetector duplicateDetector = new DuplicateStoryDetector();

    @Override public List<RankedStory> rankedStories(String topic) {
        return rankingEngine.rank(
                toCandidates(),
                topic,
                Instant.now()
        );
    }

    @Override public List<RankedStory> trendingStories() {

        List<StoryCandidate> candidates = toCandidates();

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        Instant now = Instant.now();

        List<RankedStory> trending = new ArrayList<>();

        for (StoryCandidate story : candidates) {

            double engagement = Math.log1p(
                    story.getVotes() * 2.0 +
                            story.getComments() * 1.5) / Math.log(101.0);

            engagement = Math.min(1.0, engagement);

            Instant createdAt = parseDate(story.getCreatedAt());

            double ageHours = createdAt == null ? 168.0 : Math.max(0.0,
                    Duration.between(createdAt, now).toMillis() / 3_600_000.0
            );

            double freshness = 1.0 / Math.sqrt(1.0 + ageHours / 24.0);

            double score = 100.0 * engagement * freshness;

            String explanation = String.format(
                    Locale.ROOT, "Engagement %.0f%% · freshness %.0f%%",
                    engagement * 100, freshness * 100
            );

            trending.add(new RankedStory(story,
                            Math.round(score * 100.0) / 100.0, explanation
                    ));
        }

        trending.sort(Comparator.comparingDouble(RankedStory::getScore)
                        .reversed()
                        .thenComparing(story -> story.getStory().getId(),
                                Comparator.nullsLast(Comparator.naturalOrder())
                        ));

        return Collections.unmodifiableList(trending);
    }

    @Override public Map<String, List<StoryCandidate>> duplicateStories() {
        return duplicateDetector.findDuplicates(toCandidates());
    }

    private List<StoryCandidate> toCandidates() {

        List<Comment> allComments = commentService.getAll();

        Map<Long, Integer> commentCounts = new HashMap<>();

        if (allComments != null) {
            for (Comment comment : allComments) {
                commentCounts.merge(comment.getNewsId(), 1, Integer::sum);
            }
        }

        List<NewsEntity> newsList = newsService.getNews();

        if (newsList == null) {
            return Collections.emptyList();
        }

        return newsList.stream()
                .map(news -> {

                    int votes = news.getUserLikes() == null ? 0 : news.getUserLikes().size();

                    int comments = commentCounts.getOrDefault(news.getId(), 0);

                    return new StoryCandidate(
                            news.getId(),
                            news.getTitle(),
                            news.getUrl(),
                            news.getContent(),
                            news.getCreatedAt(),
                            votes,
                            comments
                    );
                })
                .collect(Collectors.toList());
    }

    private Instant parseDate(String value) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return ZonedDateTime.parse(
                    value,
                    LEGACY_DATE
            ).toInstant();
        } catch (DateTimeParseException ignored) {

        }

        return null;
    }
}
