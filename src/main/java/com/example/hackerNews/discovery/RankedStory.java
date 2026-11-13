package com.example.hackerNews.discovery;

public final class RankedStory {
    private final StoryCandidate story;
    private final double score;
    private final String explanation;

    public RankedStory(StoryCandidate story, double score, String explanation) {
        this.story = story;
        this.score = score;
        this.explanation = explanation;
    }

    public StoryCandidate getStory() {
        return story;
    }

    public double getScore() {
        return score;
    }

    public String getExplanation() {
        return explanation;
    }
}
