package com.example.backend.controller;

import com.example.backend.dto.RegisterRequest;
import com.example.backend.entity.User;
import com.example.backend.service.UserService;
import org.springframework.web.bind.annotation.*;
import com.example.backend.dto.LoginRequest;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public String register(@RequestBody RegisterRequest request) {

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setName(request.getName());

        userService.register(user);

        return "회원가입 성공!";
    }
    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request){

        userService.login(request.getEmail(), request.getPassword());

        return "로그인 성공!";
    }
}

