package com.loopers.domain.product;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderPlaceCommand;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.point.PointService;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserService;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class StockConcurrencyTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PointService pointService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Long productId;

    @BeforeEach
    void setUp() {
        Brand brand = brandRepository.save(Brand.create("Concurrency Brand"));
        Product product = productRepository.save(Product.create("Concurrency Product", 1000L, 10, brand.getId()));
        productId = product.getId();

        // 11명의 사용자 생성 및 포인트 충전
        for (int i = 1; i <= 11; i++) {
            String userId = String.format("stockuser%02d", i);
            userService.signUp(userId, userId + "@mail.com", "1990-01-01", Gender.MALE);
            pointService.chargePoint(userId, 100000L);
        }
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("재고 10개 상품에 11건 동시 주문 시, 10건만 성공한다.")
    @Test
    void concurrentStockDeduction() throws InterruptedException {
        int threadCount = 11;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 1; i <= threadCount; i++) {
            String userId = String.format("stockuser%02d", i);
            executorService.submit(() -> {
                try {
                    OrderPlaceCommand command = new OrderPlaceCommand(
                            userId,
                            List.of(new OrderPlaceCommand.OrderItemCommand(productId, 1))
                    );
                    orderFacade.placeOrder(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // assert
        Product product = productService.getProduct(productId);
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(1);
        assertThat(product.getStockValue()).isEqualTo(0);
    }
}
