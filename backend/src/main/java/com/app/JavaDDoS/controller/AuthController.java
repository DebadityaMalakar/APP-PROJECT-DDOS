package com.app.JavaDDoS.controller;

import com.app.JavaDDoS.model.User;
import com.app.JavaDDoS.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/signup")
    public String signup(@RequestParam String username, @RequestParam String password) {
        if (userRepository.findByUsername(username) != null) {
            return "Username already exists!";
        }
        userRepository.save(new User(username, password));
        return username;
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session) {
        User user = userRepository.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            session.setAttribute("user", user.getUsername());
            return user.getUsername(); // Return username directly
        }
        return "Invalid credentials!";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "Logged out!";
    }

    @GetMapping("/me")
    public String currentUser(HttpSession session) {
        Object user = session.getAttribute("user");
        return user != null ? "Logged in as: " + user : "Not logged in.";
    }
}