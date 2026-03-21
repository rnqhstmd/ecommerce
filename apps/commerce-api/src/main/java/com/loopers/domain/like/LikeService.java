package com.loopers.domain.like;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private final LikeRepository likeRepository;

    @Transactional
    public void addLike(String userId, Long productId) {
        if (likeRepository.existsByUserIdAndProductId(userId, productId)) {
            return;
        }
        Like like = Like.create(userId, productId);
        likeRepository.save(like);
    }

    @Transactional
    public void removeLike(String userId, Long productId) {
        likeRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(likeRepository::delete);
    }

    public Long getLikeCount(Long productId) {
        return likeRepository.countByProductId(productId);
    }

    public Map<Long, Long> getLikeCountsByProductIds(List<Long> productIds) {
        return likeRepository.findLikeCountsByProductIds(productIds);
    }
}
