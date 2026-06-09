package com.loopers.interfaces.api;

/**
 * 목록 조회 API의 페이지네이션 기본값/제약 상수.
 *
 * <p>{@code @RequestParam(defaultValue = ...)}는 컴파일타임 String 상수만 허용하므로
 * 기본값은 {@code String}으로, 검증에 쓰이는 상한은 {@code int}로 정의한다.
 */
public final class PageConstants {

    private PageConstants() {
    }

    /** 기본 페이지 번호 (0부터 시작). */
    public static final String DEFAULT_PAGE = "0";

    /** 기본 페이지 크기. */
    public static final String DEFAULT_SIZE = "20";

    /** 기본 정렬 기준. */
    public static final String DEFAULT_SORT = "latest";

    /** 허용하는 최대 페이지 크기. */
    public static final int MAX_SIZE = 100;
}
