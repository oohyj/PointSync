package com.project.pointsync.service;

import com.project.pointsync.domain.User;
import com.project.pointsync.dto.User.UserResDto;
import com.project.pointsync.global.exception.CustomException;
import com.project.pointsync.global.exception.ErrorCode;
import com.project.pointsync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**  회원 가입 처리 (중복 이메일 검증 후 저장) */
    @Transactional
    public UserResDto signUp(String name, String email) {
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.USER_EMAIL_DUPLICATE,email);
        }
        User user = User.createUser(name, email);
        User saved = userRepository.save(user);
        return UserResDto.from(saved);
    }

    /** 사용자 정보 조회 (ID로 조회) */
    public UserResDto get(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return UserResDto.from(user);
    }

    /** 이메일로 사용자 정보 조회  */
    public Optional<UserResDto> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserResDto::from);
    }

    /** 이메일 존재 여부 확인 */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /** 사용자 삭제 처리 */
    @Transactional
    public void delete(Long userId) {
        userRepository.deleteById(userId);
    }
}
