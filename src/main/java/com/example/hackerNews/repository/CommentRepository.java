package com.example.hackerNews.repository;
import com.example.hackerNews.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllCommentsByNewsId(Long newsId);
    long countByEmailIgnoreCase(String email);
}
