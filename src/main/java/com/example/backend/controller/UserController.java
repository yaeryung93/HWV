package com.example.backend.controller;

import com.example.backend.dto.RegisterRequest;
import com.example.backend.entity.User;
import com.example.backend.service.UserService;
import org.springframework.web.bind.annotation.*;
import com.example.backend.dto.LoginRequest;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Map<String, String> register(@RequestBody RegisterRequest request) {

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setName(request.getName());

        userService.register(user);

        return Map.of("message", "회원가입 성공!");
    }
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {

        User user = userService.login(
                request.getEmail(),
                request.getPassword()
        );

        return Map.of(
                "message", "로그인 성공!",
                "userId", user.getId(),
                "name", user.getName()
        );
    }
}

