package com.project.pointsync.dto;

import com.project.pointsync.domain.User;

public record UserResDto(
        Long id,
        String name,
        String email
) {
    public static UserResDto from(User user) {
        return new UserResDto(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}