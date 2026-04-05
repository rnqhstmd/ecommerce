package com.loopers.application.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.ProductUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeFacade {

    private final LikeService likeService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void addLike(LikeCommand command) {
        likeService.addLike(command.userId(), command.productId());
        eventPublisher.publishEvent(new ProductUpdatedEvent(command.productId()));
    }

    @Transactional
    public void removeLike(LikeCommand command) {
        likeService.removeLike(command.userId(), command.productId());
        eventPublisher.publishEvent(new ProductUpdatedEvent(command.productId()));
    }

    public List<Long> getMyLikes(String userId) {
        return likeService.getLikesByUserId(userId).stream()
                .map(Like::getProductId)
                .toList();
    }
}
