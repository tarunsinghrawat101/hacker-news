package com.example.hackerNews.discovery;
import java.util.List;
import java.util.Map;
public interface DiscoveryService {
    List<RankedStory> rankedStories(String topic);
    List<RankedStory> trendingStories();
    Map<String, List<StoryCandidate>> duplicateStories();
}
