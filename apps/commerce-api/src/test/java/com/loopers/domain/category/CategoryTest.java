package com.loopers.domain.category;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryTest {

    @Nested
    @DisplayName("루트 카테고리 생성 (Category.createRoot)")
    class CreateRoot {

        @DisplayName("루트 카테고리를 생성할 수 있다.")
        @Test
        void createRootCategory() {
            // act
            Category category = Category.createRoot("의류");

            // assert
            assertThat(category.getName()).isEqualTo("의류");
            assertThat(category.getParentId()).isNull();
            assertThat(category.getDepth()).isZero();
        }

        @DisplayName("카테고리명이 null이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenNameIsNull() {
            // act & assert
            assertThatThrownBy(() -> Category.createRoot(null))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("카테고리명이 빈 문자열이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenNameIsBlank() {
            // act & assert
            assertThatThrownBy(() -> Category.createRoot("   "))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("카테고리명이 50자 초과이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenNameExceeds50() {
            // arrange
            String longName = "가".repeat(51);

            // act & assert
            assertThatThrownBy(() -> Category.createRoot(longName))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("자식 카테고리 생성 (Category.createChild)")
    class CreateChild {

        @DisplayName("부모 카테고리로부터 자식 카테고리를 생성할 수 있다.")
        @Test
        void createChildCategory() {
            // arrange
            Category parent = Category.createRoot("의류");

            // act
            Category child = Category.createChild("상의", parent);

            // assert
            assertThat(child.getName()).isEqualTo("상의");
            assertThat(child.getParentId()).isEqualTo(parent.getId());
            assertThat(child.getDepth()).isEqualTo(1);
        }

        @DisplayName("부모가 null이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenParentIsNull() {
            // act & assert
            assertThatThrownBy(() -> Category.createChild("상의", null))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
