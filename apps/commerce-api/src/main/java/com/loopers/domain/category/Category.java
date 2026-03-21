package com.loopers.domain.category;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Table(name = "categories")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "depth", nullable = false)
    private int depth;

    private Category(String name, Long parentId, int depth) {
        validateName(name);
        this.name = name;
        this.parentId = parentId;
        this.depth = depth;
    }

    public static Category createRoot(String name) {
        return new Category(name, null, 0);
    }

    public static Category createChild(String name, Category parent) {
        if (parent == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상위 카테고리가 존재하지 않습니다.");
        }
        return new Category(name, parent.getId(), parent.getDepth() + 1);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카테고리명은 필수입니다.");
        }
        if (name.length() > 50) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카테고리명은 50자 이하여야 합니다.");
        }
    }
}
