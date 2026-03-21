package com.loopers.domain.notification;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Getter
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "read_at")
    private ZonedDateTime readAt;

    private Notification(String userId, NotificationType type, String title, String content) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.content = content;
    }

    public static Notification create(String userId, NotificationType type, String title, String content) {
        return new Notification(userId, type, title, content);
    }

    public void markRead() {
        if (this.readAt == null) {
            this.readAt = ZonedDateTime.now();
        }
    }
}
