package com.project.pointsync.controller;

import com.project.pointsync.dto.UserResDto;
import com.project.pointsync.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResDto signUp(@RequestParam String name, @RequestParam String email) {
        return userService.signUp(name, email);
    }

    @GetMapping("/{id}")
    public UserResDto getUser(@PathVariable Long id) {
        return userService.get(id);
    }

    @GetMapping("/email")
    public Optional<UserResDto> getUserByEmail(@RequestParam String email) {
        return userService.findByEmail(email);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userService.delete(id);
    }
}
