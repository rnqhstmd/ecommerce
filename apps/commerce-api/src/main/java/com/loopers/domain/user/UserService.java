package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public User signUp(String userId, String email, String birthDate, Gender gender) {
        if (userRepository.existsByUserId(userId)) {
            throw new CoreException(ErrorType.CONFLICT, "이미 가입된 ID입니다.");
        }

        User user = User.create(userId, email, birthDate, gender);
        User savedUser = userRepository.save(user);

        eventPublisher.publishEvent(new UserSignedUpEvent(savedUser.getUserIdValue()));

        return savedUser;
    }

    public User getUserByUserId(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }
}
