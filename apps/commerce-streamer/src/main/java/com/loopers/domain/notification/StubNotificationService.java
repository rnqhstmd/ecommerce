package com.loopers.domain.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StubNotificationService implements NotificationService {

    @Override
    public void send(Notification notification) {
        log.info("[Stub] 알림 발송 - userId={}, type={}, title={}",
                notification.getUserId(),
                notification.getType(),
                notification.getTitle());
    }
}
