package com.loopers.domain.like;

import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    @Transactional
    public void addLike(String userId, Long productId) {
        if (likeRepository.existsByUserIdAndProductId(userId, productId)) {
            return;
        }
        Like like = Like.create(userId, productId);
        likeRepository.save(like);

        productRepository.incrementLikeCount(productId);
        productService.evictProductCache(productId);
    }

    @Transactional
    public void removeLike(String userId, Long productId) {
        likeRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(like -> {
                    likeRepository.delete(like);
                    productRepository.decrementLikeCount(productId);
                    productService.evictProductCache(productId);
                });
    }

    public Long getLikeCount(Long productId) {
        return likeRepository.countByProductId(productId);
    }

    public Map<Long, Long> getLikeCountsByProductIds(List<Long> productIds) {
        return likeRepository.findLikeCountsByProductIds(productIds);
    }

    public List<Like> getLikesByUserId(String userId) {
        return likeRepository.findByUserId(userId);
    }

    public Boolean getIsLiked(String userId, Long productId) {
        if (isUserIdMissing(userId)) return null;
        return likeRepository.existsByUserIdAndProductId(userId, productId);
    }

    public Map<Long, Boolean> getIsLikedMap(String userId, List<Long> productIds) {
        if (isUserIdMissing(userId) || productIds.isEmpty()) return Collections.emptyMap();
        List<Long> likedIds = likeRepository.findProductIdsByUserIdAndProductIds(userId, productIds);
        Set<Long> likedSet = new HashSet<>(likedIds);
        return productIds.stream()
                .collect(Collectors.toMap(Function.identity(), likedSet::contains));
    }

    private boolean isUserIdMissing(String userId) {
        return userId == null || userId.isBlank();
    }
}
