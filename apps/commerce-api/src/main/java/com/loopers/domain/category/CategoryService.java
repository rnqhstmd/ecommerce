package com.loopers.domain.category;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public Category create(String name, Long parentId) {
        if (parentId == null) {
            return categoryRepository.save(Category.createRoot(name));
        }
        Category parent = categoryRepository.findById(parentId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상위 카테고리를 찾을 수 없습니다."));
        return categoryRepository.save(Category.createChild(name, parent));
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "카테고리를 찾을 수 없습니다."));
    }
}
