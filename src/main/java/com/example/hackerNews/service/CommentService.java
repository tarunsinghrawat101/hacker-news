package com.example.hackerNews.service;
import com.example.hackerNews.entity.Comment;
import com.example.hackerNews.entity.User;
import java.util.List;

public interface CommentService {
    Comment get(Long id);
    Comment getForUpdate(Long id, User user, boolean admin);
    List<Comment> findAllCommentsByNewsId(Long newsId);
    List<Comment> getAll();
    void add(Long newsId, Comment comment, User user);
    Long update(Long id, String text, User user, boolean admin);
    Long delete(Long id, User user, boolean admin);
}
