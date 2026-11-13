package com.example.hackerNews.repository;

import com.example.hackerNews.entity.NewsEntity;
import com.example.hackerNews.entity.StoryState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsRepository extends JpaRepository<NewsEntity, Long> {
    java.util.List<NewsEntity> findByAuthorEmailIgnoreCaseOrderByIdDesc(String authorEmail);

    java.util.List<NewsEntity> findByAuthorEmailIsNullOrderByIdDesc();

    long countByAuthorEmailIgnoreCase(String authorEmail);

    long countByAuthorEmailIgnoreCaseAndState(String authorEmail, StoryState state);

    long countByAuthorEmailIsNull();

    long countByAuthorEmailIsNullAndState(StoryState state);

    @Query("SELECT DISTINCT news FROM NewsEntity news WHERE news.title LIKE %?1%")
    Page<NewsEntity> findByKeyword(@Param("keyword") String keyword, Pageable pagable);

    @Query("SELECT news FROM NewsEntity news WHERE (news.state = :state OR news.state IS NULL) AND news.adminDeleted = false")
    Page<NewsEntity> findAllInStateOrLegacy(@Param("state") StoryState state, Pageable pageable);

    @Query("SELECT DISTINCT news FROM NewsEntity news WHERE (news.state = :state OR news.state IS NULL) AND news.adminDeleted = false AND news.title LIKE %:keyword%")
    Page<NewsEntity> findByKeywordInStateOrLegacy(@Param("keyword") String keyword,
                                                   @Param("state") StoryState state,
                                                   Pageable pageable);

    @Query("SELECT news FROM NewsEntity news WHERE (news.state = :state OR news.state IS NULL) AND news.adminDeleted = false")
    java.util.List<NewsEntity> findAllInStateOrLegacy(@Param("state") StoryState state);
}
