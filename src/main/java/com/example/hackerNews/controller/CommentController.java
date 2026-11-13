package com.example.hackerNews.controller;

import com.example.hackerNews.entity.Comment;
import com.example.hackerNews.service.CommentService;
import com.example.hackerNews.service.NewsService;
import com.example.hackerNews.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class CommentController {
    private final CommentService commentService;
    private final NewsService newsService;
    private final UserService userService;

    public CommentController(CommentService comments,NewsService news,UserService users) {
        commentService=comments; newsService=news; userService=users;
    }

    @PostMapping("/saveComment/{id}")
    public String save(@PathVariable Long id,@ModelAttribute("commentAdd") Comment comment) {
        commentService.add(id,comment,userService.getCurrentUser());

        return "redirect:/newsOpen/"+id;
    }

    @GetMapping("/newsOpen/{id}")
    public String open(@PathVariable Long id,Model model) {
        model.addAttribute("sessionUser",userService.getCurrentUser());
        model.addAttribute("admin",userService.isAdmin());
        model.addAttribute("currentPage",1);
        model.addAttribute("keyword","");
        model.addAttribute("news",newsService.get(id));
        model.addAttribute("commentAdd",new Comment());
        model.addAttribute("comments",commentService.findAllCommentsByNewsId(id));

        return "news";
    }

    @GetMapping("/updateComment/{commentId}")
    public String edit(@PathVariable Long commentId,Model model) {
        Comment comment = commentService.getForUpdate(commentId,userService.getCurrentUser(),userService.isAdmin());
        model.addAttribute("comments",commentService.findAllCommentsByNewsId(comment.getNewsId()));
        model.addAttribute("commentUpdate",comment);
        model.addAttribute("news",newsService.get(comment.getNewsId()));

        return "updateComment";
    }

    @PostMapping("/commentUpdate/{commentId}")
    public String update(@PathVariable Long commentId,@ModelAttribute("commentUpdate") Comment submitted) {
        Long newsId = commentService.update(commentId,submitted.getComment(),
                userService.getCurrentUser(),userService.isAdmin());

        return "redirect:/newsOpen/"+newsId;
    }

    @PostMapping("/deleteComment/{commentId}")
    public String delete(@PathVariable Long commentId) {
        Long newsId=commentService.delete(commentId,userService.getCurrentUser(),userService.isAdmin());

        return "redirect:/newsOpen/"+newsId;
    }
}
