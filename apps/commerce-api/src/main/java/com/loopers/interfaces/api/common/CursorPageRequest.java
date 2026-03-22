package com.loopers.interfaces.api.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CursorPageRequest(
        Long cursor,
        @Min(1) @Max(100) int size
) {
    private static final int DEFAULT_SIZE = 20;

    public CursorPageRequest {
        if (size == 0) {
            size = DEFAULT_SIZE;
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size는 1 이상 100 이하여야 합니다.");
        }
    }
}
