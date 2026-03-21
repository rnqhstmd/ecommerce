package com.loopers.interfaces.api.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
    List<T> content,
    long totalElements,
    int totalPages,
    int currentPage,
    int size
) {
    public static <T> PageResponse<T> of(Page<?> page, List<T> content) {
        return new PageResponse<>(
            content,
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.getSize()
        );
    }

    public <R> PageResponse<R> map(Function<T, R> mapper) {
        List<R> mapped = content.stream().map(mapper).toList();
        return new PageResponse<>(mapped, totalElements, totalPages, currentPage, size);
    }
}
