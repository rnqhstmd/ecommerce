package com.loopers.interfaces.api.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class CursorPageResponseTest {

    @DisplayName("of() - entities가 size보다 크면 hasNext는 true이다.")
    @Test
    void of_hasNext_whenEntitiesExceedSize() {
        // arrange - size+1개(3+1=4개) 조회된 상황
        List<TestEntity> entities = List.of(
                new TestEntity(10L, "A"),
                new TestEntity(9L, "B"),
                new TestEntity(8L, "C"),
                new TestEntity(7L, "D")
        );
        int size = 3;

        // act
        CursorPageResponse<String> response = CursorPageResponse.of(
                entities, size, TestEntity::id, TestEntity::name
        );

        // assert
        assertAll(
                () -> assertThat(response.hasNext()).isTrue(),
                () -> assertThat(response.content()).hasSize(3),
                () -> assertThat(response.nextCursor()).isEqualTo(8L),
                () -> assertThat(response.content()).containsExactly("A", "B", "C")
        );
    }

    @DisplayName("of() - entities가 size 이하이면 hasNext는 false이다.")
    @Test
    void of_noNext_whenEntitiesWithinSize() {
        // arrange - size 이하(2개, size=3)
        List<TestEntity> entities = List.of(
                new TestEntity(5L, "X"),
                new TestEntity(4L, "Y")
        );
        int size = 3;

        // act
        CursorPageResponse<String> response = CursorPageResponse.of(
                entities, size, TestEntity::id, TestEntity::name
        );

        // assert
        assertAll(
                () -> assertThat(response.hasNext()).isFalse(),
                () -> assertThat(response.content()).hasSize(2),
                () -> assertThat(response.nextCursor()).isNull(),
                () -> assertThat(response.content()).containsExactly("X", "Y")
        );
    }

    @DisplayName("of() - entities가 정확히 size개이면 hasNext는 false이다.")
    @Test
    void of_noNext_whenEntitiesEqualToSize() {
        // arrange
        List<TestEntity> entities = List.of(
                new TestEntity(3L, "A"),
                new TestEntity(2L, "B"),
                new TestEntity(1L, "C")
        );
        int size = 3;

        // act
        CursorPageResponse<String> response = CursorPageResponse.of(
                entities, size, TestEntity::id, TestEntity::name
        );

        // assert
        assertAll(
                () -> assertThat(response.hasNext()).isFalse(),
                () -> assertThat(response.content()).hasSize(3),
                () -> assertThat(response.nextCursor()).isNull()
        );
    }

    @DisplayName("of() - 빈 리스트이면 hasNext는 false이고 content는 비어있다.")
    @Test
    void of_emptyList_returnsFalseAndEmpty() {
        // act
        CursorPageResponse<String> response = CursorPageResponse.of(
                Collections.emptyList(), 10, TestEntity::id, TestEntity::name
        );

        // assert
        assertAll(
                () -> assertThat(response.hasNext()).isFalse(),
                () -> assertThat(response.content()).isEmpty(),
                () -> assertThat(response.nextCursor()).isNull()
        );
    }

    @DisplayName("of() - mapper를 통해 엔티티가 DTO로 변환된다.")
    @Test
    void of_mapsEntitiesToDto() {
        // arrange
        List<TestEntity> entities = List.of(
                new TestEntity(1L, "hello"),
                new TestEntity(2L, "world")
        );

        // act
        CursorPageResponse<String> response = CursorPageResponse.of(
                entities, 5, TestEntity::id, e -> e.name().toUpperCase()
        );

        // assert
        assertThat(response.content()).containsExactly("HELLO", "WORLD");
    }

    private record TestEntity(Long id, String name) {}
}
