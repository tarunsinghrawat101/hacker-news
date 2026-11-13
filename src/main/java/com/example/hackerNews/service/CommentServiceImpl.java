package com.example.hackerNews.service;
import com.example.hackerNews.entity.Comment;
import com.example.hackerNews.entity.User;
import com.example.hackerNews.repository.CommentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional(readOnly=true)
public class CommentServiceImpl implements CommentService {
    private final CommentRepository repository;

    public CommentServiceImpl(CommentRepository repository) {
        this.repository=repository;
    }

    @Override
    public Comment get(Long id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("Comment not found: "+id));
    }

    @Override
    public Comment getForUpdate(Long id,User user,boolean admin) {
        Comment comment = get(id);
        authorize(comment,user,false,admin);

        return comment;
    }

    @Override
    public List<Comment> findAllCommentsByNewsId(Long id) {
        return repository.findAllCommentsByNewsId(id);
    }

    @Override
    public List<Comment> getAll() {
        return repository.findAll();
    }

    @Override
    @Transactional
    public void add(Long newsId, Comment comment, User user) {
        requireUser(user);
        comment.setId(null);
        comment.setNewsId(newsId);
        comment.setName(user.getName());
        comment.setEmail(user.getEmail());
        comment.setCreatedAt(new Date().toString());

        repository.save(comment);
    }

    @Override
    @Transactional
    public Long update(Long id, String text, User user, boolean admin) {
        Comment comment=get(id);
        authorize(comment,user,false,admin);
        if(text == null || text.trim().isEmpty())
            throw new IllegalArgumentException("Comment cannot be empty");

        comment.setComment(text.trim());

        return comment.getNewsId();
    }

    @Override
    @Transactional
    public Long delete(Long id, User user, boolean admin) {
        Comment comment = get(id);
        authorize(comment, user,true,admin);

        Long newsId = comment.getNewsId();

        repository.delete(comment);

        return newsId;
    }

    private void authorize(Comment comment, User user, boolean allowAdmin, boolean admin) {
        requireUser(user);

        if(admin && allowAdmin)
            return;

        if(admin || !user.getEmail().equalsIgnoreCase(comment.getEmail()))
            throw new AccessDeniedException("Not allowed to modify this comment");
    }

    private void requireUser(User user) {
        if(user == null)
            throw new AccessDeniedException("Authentication required");
    }
}
