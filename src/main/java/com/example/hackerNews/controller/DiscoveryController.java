
package com.example.hackerNews.controller;

import com.example.hackerNews.discovery.DiscoveryService;
import com.example.hackerNews.discovery.RankedStory;
import com.example.hackerNews.service.UserService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;

@Controller
public class DiscoveryController {

    private final DiscoveryService discoveryService;
    private final UserService userService;

    public DiscoveryController(DiscoveryService discoveryService, UserService userService) {
        this.discoveryService = discoveryService;
        this.userService = userService;
    }

    @ResponseBody
    @GetMapping("/api/discovery/ranked")
    public List<RankedStory> ranked(@RequestParam(value = "topic", required = false) String topic) {
        return discoveryService.rankedStories(topic);
    }

    @ResponseBody
    @GetMapping("/api/discovery/trending")
    public List<RankedStory> trending() {
        return discoveryService.trendingStories();
    }

    @ResponseBody
    @GetMapping("/api/discovery/duplicates")
    public Map<String, ?> duplicates() {
        return discoveryService.duplicateStories();
    }

    @GetMapping("/discover/recommended")
    public String recommended(@RequestParam(value = "topic", required = false) String topic, Model model) {
        List<RankedStory> stories = discoveryService.rankedStories(topic);

        addCommonModelAttributes(model);

        model.addAttribute("stories", stories);
        model.addAttribute("discoveryMode", "Recommended News");
        model.addAttribute("topic", topic);

        return "discovery";
    }

    @GetMapping("/discover/trending")
    public String trending(Model model) {
        List<RankedStory> stories =
                discoveryService.trendingStories();

        addCommonModelAttributes(model);

        model.addAttribute("stories", stories);
        model.addAttribute("discoveryMode", "Trending News");

        return "discovery";
    }

    private void addCommonModelAttributes(Model model) {
        model.addAttribute("sessionUser",userService.getCurrentUser());
        model.addAttribute("user",userService.getCurrentUser());
        model.addAttribute("admin",userService.isAdmin());
        model.addAttribute("currentPage",1);
        model.addAttribute("keyword","");
    }
}