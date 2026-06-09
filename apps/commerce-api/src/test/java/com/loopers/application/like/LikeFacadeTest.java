package com.loopers.application.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LikeFacadeTest {

    @Mock
    private LikeService likeService;

    @InjectMocks
    private LikeFacade likeFacade;

    @DisplayName("좋아요를 추가하면 LikeService.addLike를 userId/productId로 호출한다.")
    @Test
    void addLike_delegatesToLikeService() {
        // given
        LikeCommand command = new LikeCommand("testuser", 100L);

        // when
        likeFacade.addLike(command);

        // then
        verify(likeService, times(1)).addLike("testuser", 100L);
    }

    @DisplayName("좋아요를 취소하면 LikeService.removeLike를 userId/productId로 호출한다.")
    @Test
    void removeLike_delegatesToLikeService() {
        // given
        LikeCommand command = new LikeCommand("testuser", 100L);

        // when
        likeFacade.removeLike(command);

        // then
        verify(likeService, times(1)).removeLike("testuser", 100L);
    }

    @DisplayName("내 좋아요 목록을 조회하면 Like 목록을 productId 목록으로 변환하여 반환한다.")
    @Test
    void getMyLikes_returnsProductIds() {
        // given
        when(likeService.getLikesByUserId("testuser"))
                .thenReturn(List.of(Like.create("testuser", 100L), Like.create("testuser", 200L)));

        // when
        List<Long> result = likeFacade.getMyLikes("testuser");

        // then
        verify(likeService, times(1)).getLikesByUserId("testuser");
        assertThat(result).containsExactly(100L, 200L);
    }
}
