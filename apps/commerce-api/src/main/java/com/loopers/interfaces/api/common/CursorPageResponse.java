package com.loopers.interfaces.api.common;

import java.util.List;
import java.util.function.Function;

public record CursorPageResponse<T>(
        List<T> content,
        Long nextCursor,
        boolean hasNext
) {
    /**
     * 엔티티 리스트로부터 CursorPageResponse를 생성한다.
     * 조회 시 size+1개를 가져온 뒤, hasNext 판별 후 실제 size개만 반환한다.
     *
     * @param entities    size+1개로 조회된 엔티티 목록
     * @param size        요청된 페이지 크기
     * @param idExtractor 엔티티에서 ID(cursor)를 추출하는 함수
     * @param mapper      엔티티를 응답 DTO로 변환하는 함수
     */
    public static <T, E> CursorPageResponse<T> of(
            List<E> entities,
            int size,
            Function<E, Long> idExtractor,
            Function<E, T> mapper
    ) {
        boolean hasNext = entities.size() > size;
        List<E> content = hasNext ? entities.subList(0, size) : entities;

        List<T> mapped = content.stream().map(mapper).toList();
        Long nextCursor = hasNext ? idExtractor.apply(content.get(content.size() - 1)) : null;

        return new CursorPageResponse<>(mapped, nextCursor, hasNext);
    }
}
