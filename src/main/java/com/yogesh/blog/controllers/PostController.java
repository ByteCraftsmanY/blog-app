package com.yogesh.blog.controllers;


import com.yogesh.blog.entities.Comment;
import com.yogesh.blog.entities.Post;
import com.yogesh.blog.services.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
public class PostController {
    private final PostService postService;

    @Autowired
    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("feed")
    String getPosts(Model model) {
        List<Post> posts = postService.getBlogPostList();
        model.addAttribute("posts", posts);
        return "feed";
    }

    @GetMapping("show-post")
    String fetchPost(@RequestParam("id") String id, Model model) {
        Comment comment = new Comment();
        Optional<Post> blogPost = postService.getPost(Integer.parseInt(id));
        Post post = blogPost.orElseGet(Post::new);
        comment.setPost(post);
        model.addAttribute("post", post);
        model.addAttribute("comment",comment);
        return "post";
    }

    @PostMapping("save-post")
    String savePost(@ModelAttribute("post") Post blogPost) {
        postService.addPost(blogPost);
        return "redirect:/feed";
    }

    @GetMapping("delete-post")
    String deletePost(@RequestParam("id") Integer id) {
        postService.deletePost(id);
        return "redirect:/feed";
    }

    @GetMapping("edit-post")
    String fetchPostForUpdate(@RequestParam("id") String id, Model model) {
        Optional<Post> post = postService.getPost(Integer.parseInt(id));
        model.addAttribute("post", post.orElseGet(Post::new));
        return "post-form";
    }

    @GetMapping("new-post")
    String showPostForm(Model model) {
        model.addAttribute("post", new Post());
        return "post-form";
    }
}
