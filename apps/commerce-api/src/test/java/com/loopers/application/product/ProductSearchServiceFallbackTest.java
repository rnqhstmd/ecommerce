package com.loopers.application.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSearchCondition;
import com.loopers.domain.product.ProductSearchPort;
import com.loopers.domain.product.ProductSearchResult;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class ProductSearchServiceFallbackTest {

    @Autowired
    private ProductSearchService productSearchService;

    @MockitoSpyBean
    private ProductSearchPort productSearchPort;

    @MockitoSpyBean
    private ProductRepository productRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("ES가 정상일 때 ProductSearchPort.searchProducts()가 호출된다.")
    @Test
    void search_callsProductSearchPort_whenEsIsHealthy() {
        // arrange
        ProductSearchResult mockResult = new ProductSearchResult(List.of(1L, 2L), 2L);
        doReturn(mockResult).when(productSearchPort)
                .searchProducts(any(), any(), any(), any(), anyInt(), anyInt(), any());

        ProductGetListCommand command = new ProductGetListCommand(
                null, null, "운동화", null, null, PageRequest.of(0, 10)
        );

        // act
        ProductSearchInfo result = productSearchService.search(command);

        // assert
        assertAll(
                () -> verify(productSearchPort, times(1))
                        .searchProducts(eq("운동화"), isNull(), isNull(), isNull(), eq(0), eq(10), any()),
                () -> assertThat(result.productIds()).containsExactly(1L, 2L),
                () -> assertThat(result.totalHits()).isEqualTo(2L)
        );
    }

    @DisplayName("ES 예외 발생 시 fallback으로 ProductRepository.findProducts()가 호출된다.")
    @Test
    void search_fallsBackToMysql_whenEsThrows() {
        // arrange
        doThrow(new RuntimeException("ES connection refused"))
                .when(productSearchPort)
                .searchProducts(any(), any(), any(), any(), anyInt(), anyInt(), any());

        ProductGetListCommand command = new ProductGetListCommand(
                null, null, "운동화", null, null, PageRequest.of(0, 10)
        );

        // act
        ProductSearchInfo result = productSearchService.search(command);

        // assert
        assertAll(
                () -> verify(productSearchPort, times(1))
                        .searchProducts(any(), any(), any(), any(), anyInt(), anyInt(), any()),
                () -> verify(productRepository, times(1))
                        .findProducts(any(ProductSearchCondition.class)),
                () -> assertThat(result).isNotNull()
        );
    }

    @DisplayName("자동완성 ES 예외 발생 시 빈 리스트를 반환한다.")
    @Test
    void autocomplete_returnsEmptyList_whenEsThrows() {
        // arrange
        doThrow(new RuntimeException("ES timeout"))
                .when(productSearchPort)
                .autocomplete(anyString(), anyInt());

        // act
        List<String> result = productSearchService.autocomplete("나이", 10);

        // assert
        assertAll(
                () -> verify(productSearchPort, times(1)).autocomplete("나이", 10),
                () -> assertThat(result).isEmpty()
        );
    }
}
