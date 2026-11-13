package com.example.hackerNews.controller;

import com.example.hackerNews.entity.NewsEntity;
import com.example.hackerNews.entity.StoryEvent;
import com.example.hackerNews.service.NewsService;
import com.example.hackerNews.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class NewsController {
    private final NewsService newsService;
    private final UserService userService;

    public NewsController(NewsService newsService, UserService userService) {
        this.newsService=newsService;
        this.userService=userService;
    }

    @ModelAttribute
    public void common(Model model) {
        model.addAttribute("sessionUser", userService.getCurrentUser());
        model.addAttribute("admin", userService.isAdmin());
    }

    @GetMapping("/")
    public String home(Model model) {
        return listing(1,"id","desc","", "showAllNews","/page",1,model);
    }

    @GetMapping("/login")
    public String login() {
        var a = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        return a == null || a instanceof org.springframework.security.authentication.AnonymousAuthenticationToken ? "login" : "redirect:/";
    }

    @GetMapping("/createNewNews")
    public String createForm(Model model) {
        model.addAttribute("user", userService.getCurrentUser());
        if (!model.containsAttribute("newsEntity")) model.addAttribute("newsEntity", new NewsEntity());

        return "createNews";
    }

    @PostMapping("/saveNews")
    public String save(@ModelAttribute("newsEntity") NewsEntity news,
                       @RequestParam(name = "saveAsDraft", defaultValue = "false") boolean saveAsDraft,
                       RedirectAttributes redirect) {
        try {
            if (saveAsDraft) newsService.createDraft(news, userService.getCurrentUser());
            else newsService.create(news, userService.getCurrentUser());
            return "redirect:/userProfile";
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("newsEntity", news);
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/createNewNews";
        }
    }

    /** Explicit lifecycle endpoint; existing create/edit/delete routes are unchanged. */
    @PostMapping("/story/{id}/transition/{event}")
    public String transitionStory(@PathVariable Long id, @PathVariable String event) {
        StoryEvent storyEvent;
        try {
            storyEvent = StoryEvent.valueOf(event.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown story event: " + event, ex);
        }
        newsService.transitionState(id, storyEvent,
                userService.getCurrentUser(), userService.isAdmin());
        return "redirect:/userNews";
    }

    @PostMapping("/updateNews")
    public String update(@ModelAttribute("newsEntity") NewsEntity news,
                         @RequestParam(name = "saveAsDraft", defaultValue = "false") boolean saveAsDraft,
                         RedirectAttributes redirect) {
        try {
            newsService.update(news, userService.getCurrentUser(), userService.isAdmin(), saveAsDraft);
            return "redirect:/userProfile";
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("newsEntity", news);
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/editNews/" + news.getId();
        }
    }

    @GetMapping("/editNews/{id}")
    public String edit(@PathVariable Long id, Model model) {
        if (!model.containsAttribute("newsEntity")) model.addAttribute("newsEntity", newsService.get(id));

        return "editNews";
    }

    @PostMapping("/deleteNews/{id}")
    public String delete(@PathVariable Long id) {
        newsService.delete(id, userService.getCurrentUser(), userService.isAdmin());

        return "redirect:/";
    }

    @GetMapping("/page/{pageNo}")
    public String page(@PathVariable int pageNo,
                       @RequestParam(defaultValue="id") String sortField,
                       @RequestParam(defaultValue="desc") String sortDir,
                       @RequestParam(defaultValue="") String keyword,Model model) {
        return listing(pageNo,sortField,sortDir,keyword,"showAllNews","/page",1,model);
    }

    @GetMapping("/welcome")
    public String welcome() {
        return "welcome.html";
    }

    @GetMapping("/pastNews/{pageNo}")
    public String past(@PathVariable int pageNo,
                       @RequestParam(defaultValue="id") String sortField,
                       @RequestParam(defaultValue="asc") String sortDir,
                       @RequestParam(defaultValue="") String keyword,Model model) {
        return listing(pageNo,sortField,sortDir,keyword,"pastNews","/pastNews",3,model);
    }

    @GetMapping("/newNews/{pageNo}")
    public String newest(@PathVariable int pageNo,
                         @RequestParam(defaultValue="id") String sortField,
                         @RequestParam(defaultValue="desc") String sortDir,
                         @RequestParam(defaultValue="") String keyword,Model model) {
        return listing(pageNo,sortField,sortDir,keyword,"newNews","/newNews",2,model);
    }
    @PostMapping("/addLike/{newsId}/{value}")
    public String addLike(@PathVariable Long newsId, @PathVariable Integer value) {
        newsService.addLike(newsId,userService.getCurrentUser());

        return value == 2 ? "redirect:/newNews/1?sortField=id&sortDir=desc&keyword=" :
                value == 3 ? "redirect:/pastNews/1?sortField=id&sortDir=asc&keyword=" :
                        "redirect:/";
    }

    @PostMapping("/addLike/{newsId}")
    public String addLikeFromList(@PathVariable Long newsId) {
        newsService.addLike(newsId, userService.getCurrentUser());

        return "redirect:/";
    }

    @PostMapping("/removeLike/{newsId}")
    public String removeLike(@PathVariable Long newsId) {
        newsService.removeLike(newsId, userService.getCurrentUser());

        return "redirect:/";
    }

    @GetMapping("/userProfile")
    public String profile(Model model) {
        model.addAttribute("user", userService.getCurrentUser());
        model.addAllAttributes(newsService.profileStatistics(userService.getCurrentUser()));
        model.addAttribute("myPosts", newsService.getStoriesForAuthor(userService.getCurrentUser().getEmail(), userService.getCurrentUser()));

        return "userProfile";
    }

    @GetMapping("/userNews")
    public String userNews(Model model) {
        model.addAttribute("newsList", newsService.getUserNews(userService.getCurrentUser(), "mine"));
        model.addAttribute("user", userService.getCurrentUser());
        model.addAttribute("value",4);

        return "userNews";
    }

    @GetMapping("/hiddenNews")
    public String hidden(Model model) {
        model.addAttribute("newsList", newsService.getUserNews(userService.getCurrentUser(),"hidden"));

        return "hiddenNews";
    }

    @GetMapping("/favoriteNews")
    public String favorites(Model model) {
        model.addAttribute("newsList", newsService.getUserNews(userService.getCurrentUser(),"favorite"));

        return "favoriteNews";
    }

    @PostMapping("/hideNews/{newsId}")
    public String hide(@PathVariable Long newsId) {
        newsService.hide(newsId, userService.getCurrentUser(),true);

        return "redirect:/";
    }

    @PostMapping("/profile/hide/{newsId}")
    public String hideFromProfile(@PathVariable Long newsId) {
        newsService.hide(newsId, userService.getCurrentUser(), true);
        return "redirect:/userProfile";
    }

    @PostMapping("/profile/hide")
    public String hideSelectedFromProfile(@RequestParam(value = "postIds", required = false) java.util.List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) return "redirect:/userProfile";
        for (Long id : new java.util.LinkedHashSet<>(postIds)) {
            if (id != null) newsService.hide(id, userService.getCurrentUser(), true);
        }
        return "redirect:/userProfile";
    }

    @PostMapping("/hideNewsFromNew/{newsId}")
    public String hideFromNew(@PathVariable Long newsId) {
        newsService.hide(newsId, userService.getCurrentUser(),true);

        return "redirect:/newNews/1";
    }

    @PostMapping("/removeHide/{newsId}")
    public String unhide(@PathVariable Long newsId) {
        newsService.hide(newsId, userService.getCurrentUser(), false);

        return "redirect:/";
    }

    @PostMapping("/favoriteNews/{newsId}")
    public String favorite(@PathVariable Long newsId) {
        newsService.favorite(newsId, userService.getCurrentUser(),true);

        return "redirect:/";
    }

    @PostMapping("/removeFavorite/{newsId}")
    public String unfavorite(@PathVariable Long newsId) {
        newsService.favorite(newsId, userService.getCurrentUser(),false);

        return "redirect:/";
    }

    private String listing(int page,String sort,String dir,String keyword,String view,String path,int value,Model model) {
        var user=userService.getCurrentUser();
        Page<NewsEntity> result=newsService.getPostsWithPagination(page,20,sort,dir,keyword,user);
        model.addAttribute("currentPage",page);
        model.addAttribute("listingPath",path);
        model.addAttribute("user",user);
        model.addAttribute("sortField",sort);
        model.addAttribute("sortDir",dir);
        model.addAttribute("totalPages",result.getTotalPages());
        model.addAttribute("keyword",keyword);
        model.addAttribute("newsList",result.getContent());
        model.addAttribute("value",value);
        model.addAttribute("reverseSortDir","asc".equalsIgnoreCase(dir) ?"desc" : "asc");

        return view;
    }
}
