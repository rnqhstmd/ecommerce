package com.loopers.interfaces.api.common;

public record CursorPageRequest(
        Long cursor,
        int size
) {
    public CursorPageRequest {
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size는 1 이상 100 이하여야 합니다.");
        }
    }
}
