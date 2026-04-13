package com.datapulse.service;

import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.logging.LogEventPublisher;
import com.datapulse.logging.LogEventType;
import com.datapulse.model.User;
import com.datapulse.model.enums.AccountStatus;
import com.datapulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final LogEventPublisher logEventPublisher;

    @Transactional
    public User updateAccountStatus(String userId, AccountStatus status, String actingAdminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        AccountStatus previous = user.getAccountStatus();
        user.setAccountStatus(status);
        User saved = userRepository.save(user);

        logEventPublisher.publish(
                LogEventType.USER_STATUS_CHANGED,
                actingAdminId,
                "ADMIN",
                Map.of(
                        "target_user_id", userId,
                        "previous_status", previous != null ? previous.name() : "null",
                        "new_status", status.name()));

        return saved;
    }
}
