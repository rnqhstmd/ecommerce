package com.loopers.infrastructure.notification;

import com.loopers.domain.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJpaRepository extends JpaRepository<Notification, Long> {
}
