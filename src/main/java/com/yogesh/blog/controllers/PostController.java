package com.yogesh.blog.controllers;

import com.yogesh.blog.model.*;
import com.yogesh.blog.services.PostService;
import com.yogesh.blog.services.TagService;
import com.yogesh.blog.services.UserPrincipalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Controller
public class PostController {
    private final PostService postService;
    private final TagService tagService;
    private final UserPrincipalService userPrincipalService;

    @Autowired
    public PostController(PostService postService, TagService tagService, UserPrincipalService userPrincipalService) {
        this.postService = postService;
        this.tagService = tagService;
        this.userPrincipalService = userPrincipalService;
    }

    @GetMapping("/")
    String getAllPosts(
            @RequestParam(name = "page", defaultValue = "0", required = false) Integer page,
            @RequestParam(name = "size", defaultValue = "5", required = false) Integer size,
            @RequestParam(name = "order", defaultValue = "desc", required = false) String sortingOrder,
            @RequestParam(name = "keyword", defaultValue = "", required = false) String keyword,
            @RequestParam(name = "sort-field", defaultValue = "publishedAt", required = false) String sortingField,
            @RequestParam(name = "start-date", defaultValue = "", required = false) String startDate,
            @RequestParam(name = "end-date", defaultValue = "", required = false) String endDate,
            @RequestParam(name = "author", defaultValue = "", required = false) String author,
            @RequestParam(name = "selected-tags", defaultValue = "", required = false) List<String> selectedTags,
            Model model) {
        Page<Post> posts;
        List<String> authors;
        List<String> tags;
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;
        User activeUser = null;
        boolean isPublishedDateDurationAvailable = !startDate.isEmpty() || !endDate.isEmpty();

        if (startDate.isEmpty()) {
            startDate = "1970-01-01";
        }
        if (endDate.isEmpty()) {
            endDate = LocalDate.now().toString();
        }
        startDateTime = LocalDate.parse(startDate).atStartOfDay();
        endDateTime = LocalDate.parse(endDate).plusDays(1).atStartOfDay();

        if (!keyword.isEmpty() && !author.isEmpty() && !selectedTags.isEmpty()) {
            posts = postService.findPostsByKeywordAndAuthorAndTagAndPublishedDateDuration(keyword, author, selectedTags, startDateTime, endDateTime, sortingField, sortingOrder, page, size);
        } else if (!keyword.isEmpty() && !author.isEmpty()) {
            posts = postService.findPostsByKeywordAndAuthorAndPublishedDateDuration(keyword, author, startDateTime, endDateTime, sortingField, sortingOrder, page, size);
        } else if (!keyword.isEmpty() && !selectedTags.isEmpty()) {
            posts = postService.findPostsByKeywordAndTagsAndPublishedDateDuration(keyword, selectedTags, startDateTime, endDateTime, sortingField, sortingOrder, page, size);
        } else if (!keyword.isEmpty()) {
            posts = postService.findPostsByKeywordAndPublishedDateDuration(keyword, startDateTime, endDateTime, sortingField, sortingOrder, page, size);
        } else if (!author.isEmpty() && !selectedTags.isEmpty()) {
            posts = postService.findPostByAuthorAndTagsAndPublishedDateDuration(author, selectedTags, startDateTime, endDateTime, sortingField, sortingOrder, page, size);
        } else if (!author.isEmpty()) {
            posts = postService.findPostsByAuthorAndPublishedDateDuration(author, startDateTime, endDateTime, sortingField, sortingOrder, page, size);
        } else if (!selectedTags.isEmpty()) {
            posts = postService.findPostByTagsAndPublishedDateDuration(selectedTags, startDateTime, endDateTime, sortingField, sortingOrder, page, size);
        } else if (isPublishedDateDurationAvailable) {
            posts = postService.findPostsByPublishedDateDuration(startDateTime, endDateTime, sortingField, sortingOrder, page, size);
        } else {
            posts = postService.findAllPost(sortingField, sortingOrder, page, size);
        }
        authors = postService.findAllAuthors();
        tags = tagService.findAllTagNames();

        UserPrincipal userPrincipal = userPrincipalService.getLoggedInUser();
        if (!Objects.isNull(userPrincipal)) {
            activeUser = userPrincipal.getUser();
        }
        model.addAttribute("posts", posts.getContent());
        model.addAttribute("authors", authors);
        model.addAttribute("tags", tags);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("sortingField", sortingField);
        model.addAttribute("sortingOrder", sortingOrder);
        model.addAttribute("keyword", keyword);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("author", author);
        model.addAttribute("selectedTags", selectedTags);
        model.addAttribute("totalPages", posts.getTotalPages());
        model.addAttribute("user", activeUser);
        return "home-page";
    }

    @GetMapping("show-post")
    String getPostById(@RequestParam("id") Integer id, Model model) {
        Post post = postService.findPostById(id);
        User user = null;
        UserPrincipal userPrincipal = userPrincipalService.getLoggedInUser();
        if (!Objects.isNull(userPrincipal)) {
            user = userPrincipal.getUser();
        }
        model.addAttribute("post", post);
        model.addAttribute("user", user);
        return "post";
    }

    @PostMapping("save-post")
    String savePost(@ModelAttribute("post") Post post, @RequestParam(name = "tag-string") String tagString) {
        if (post.getCreatedAt() == null) {
            post.setCreatedAt(LocalDateTime.now());
            post.setPublishedAt(LocalDateTime.now());
            post.setUser(Objects.requireNonNull(userPrincipalService.getLoggedInUser()).getUser());
        } else {
            post.setUpdatedAt(LocalDateTime.now());
        }
        Set<Tag> tags = new HashSet<>();
        for (String tagName : tagString.split(" ")) {
            Tag tag = tagService.findTagByName(tagName);
            if (Objects.isNull(tag)) {
                tag = new Tag();
                tag.setName(tagName);
                tag.setCreatedAt(LocalDateTime.now());
            }
            tags.add(tag);
        }
        post.setTags(tags.stream().toList());
        postService.savePost(post);
        return "redirect:/";
    }

    @GetMapping("delete-post")
    String deletePostById(@RequestParam("id") Integer id) {
        Post post = postService.findPostById(id);
        if (!userPrincipalService.isUserAuthorizedForPostOperation(post)) {
            return "error";
        }
        postService.deletePostById(id);
        return "redirect:/";
    }

    @GetMapping("edit-post")
    String getPostForUpdate(@RequestParam("id") Integer id, Model model) {
        Post post = postService.findPostById(id);
        if (!userPrincipalService.isUserAuthorizedForPostOperation(post)) {
            return "error";
        }
        StringBuilder tags = new StringBuilder();
        for (Tag tag : post.getTags()) {
            tags.append(tag.getName()).append(" ");
        }
        model.addAttribute("post", post);
        model.addAttribute("tags", tags.toString());
        return "post-form";
    }

    @GetMapping("/new-post")
    String showFormForNewPost(Model model) {
        model.addAttribute("post", new Post());
        return "post-form";
    }
}
