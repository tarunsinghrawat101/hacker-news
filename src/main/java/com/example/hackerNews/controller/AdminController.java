package com.example.hackerNews.controller;

import com.example.hackerNews.entity.NewsEntity;
import com.example.hackerNews.entity.StoryState;
import com.example.hackerNews.entity.User;
import com.example.hackerNews.repository.NewsRepository;
import com.example.hackerNews.repository.UserRepository;
import com.example.hackerNews.service.NewsService;
import com.example.hackerNews.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final UserService users;
    private final UserRepository userRepository;
    private final NewsRepository newsRepository;
    private final NewsService news;

    public AdminController(UserService users, UserRepository userRepository,
                           NewsRepository newsRepository, NewsService news) {
        this.users = users;
        this.userRepository = userRepository;
        this.newsRepository = newsRepository;
        this.news = news;
    }

    private void requireAdmin(Model model) {
        if (!users.isAdmin()) throw new AccessDeniedException("Admin only");
        model.addAttribute("sessionUser", users.getCurrentUser());
        model.addAttribute("admin", true);
    }

    @GetMapping({"", "/"})
    public String members(Model model) {
        requireAdmin(model);
        List<MemberSummary> summaries = users.getAllUser().stream()
                .map(u -> new MemberSummary(u,
                        newsRepository.countByAuthorEmailIgnoreCase(u.getEmail()),
                        newsRepository.countByAuthorEmailIgnoreCaseAndState(u.getEmail(), StoryState.PENDING_REVIEW)))
                .collect(Collectors.toList());
        model.addAttribute("members", summaries);
        model.addAttribute("unassignedCount", newsRepository.countByAuthorEmailIsNull());
        model.addAttribute("unassignedPending", newsRepository.countByAuthorEmailIsNullAndState(StoryState.PENDING_REVIEW));
        return "adminMembers";
    }

    @GetMapping("/member/{id}")
    public String member(@PathVariable Long id, Model model) {
        requireAdmin(model);
        User member = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Member not found"));
        model.addAttribute("member", member);
        model.addAttribute("posts", news.getStoriesForAuthor(member.getEmail()));
        model.addAttribute("postAction", "/admin/member/" + id + "/state");
        model.addAttribute("deleteAction", "/admin/member/" + id + "/delete");
        return "adminMemberPosts";
    }

    @GetMapping("/unassigned")
    public String unassigned(Model model) {
        requireAdmin(model);
        model.addAttribute("member", null);
        model.addAttribute("posts", news.getUnassignedStories());
        model.addAttribute("postAction", "/admin/unassigned/state");
        model.addAttribute("deleteAction", "/admin/unassigned/delete");
        return "adminMemberPosts";
    }

    @PostMapping("/member/{id}/state")
    public String memberState(@PathVariable Long id,
                              @RequestParam(value = "postIds", required = false) List<Long> postIds,
                              @RequestParam("targetState") StoryState targetState,
                              RedirectAttributes redirect) {
        if (!users.isAdmin()) throw new AccessDeniedException("Admin only");
        User member = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Member not found"));
        apply(member.getEmail(), postIds, targetState, redirect);
        return "redirect:/admin/member/" + id;
    }

    @PostMapping("/unassigned/state")
    public String unassignedState(@RequestParam(value = "postIds", required = false) List<Long> postIds,
                                  @RequestParam("targetState") StoryState targetState,
                                  RedirectAttributes redirect) {
        if (!users.isAdmin()) throw new AccessDeniedException("Admin only");
        apply(null, postIds, targetState, redirect);
        return "redirect:/admin/unassigned";
    }

    @PostMapping("/member/{id}/delete")
    public String deleteMemberPosts(@PathVariable Long id,
                                   @RequestParam(value = "postIds", required = false) List<Long> postIds,
                                   RedirectAttributes redirect) {
        if (!users.isAdmin()) throw new AccessDeniedException("Admin only");
        User member = userRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Member not found"));
        deleteSelected(member.getEmail(), postIds, redirect);
        return "redirect:/admin/member/" + id;
    }

    @PostMapping("/unassigned/delete")
    public String deleteUnassignedPosts(@RequestParam(value = "postIds", required = false) List<Long> postIds,
                                        RedirectAttributes redirect) {
        if (!users.isAdmin()) throw new AccessDeniedException("Admin only");
        deleteSelected(null, postIds, redirect);
        return "redirect:/admin/unassigned";
    }

    private void deleteSelected(String authorEmail, List<Long> postIds, RedirectAttributes redirect) {
        try {
            news.adminDeleteStoriesForAuthor(authorEmail, postIds, users.getCurrentUser(), users.isAdmin());
            redirect.addFlashAttribute("success", "Selected posts removed from public view. Authors can still see the removal status in their profiles.");
        } catch (IllegalArgumentException | AccessDeniedException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
    }

    private void apply(String authorEmail, List<Long> postIds, StoryState target, RedirectAttributes redirect) {
        try {
            news.adminSetStateForAuthor(authorEmail, postIds, target, users.getCurrentUser(), users.isAdmin());
            redirect.addFlashAttribute("success", "Selected posts updated to " + target + ".");
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
    }

    public static class MemberSummary {
        private final User member;
        private final long total;
        private final long pending;
        public MemberSummary(User member, long total, long pending) {
            this.member = member;
            this.total = total;
            this.pending = pending;
        }
        public User getMember() { return member; }
        public long getTotal() { return total; }
        public long getPending() { return pending; }
    }
}
