// S0 인수 테스트 — 너의 Spring Boot 프로젝트 src/test/java/... 아래에 복사해 green 만들면 S0 완료.
// 이 테스트는 "행동"만 검증한다. 엔티티/서비스/리포지토리 구조는 네가 정한다.
package com.example.flashsale;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class S0FlashSaleTest {

    @Autowired
    TestRestTemplate http;

    // HTTP 계약 (이것만 지키면 내부 구현은 자유):
    //   POST /products        body {"name","price","stock"}     -> 201, body {"id", ...}
    //   GET  /products/{id}                                       -> 200, body {"id","name","price","stock"}
    //   POST /orders          body {"productId","userId"}         -> 201 (성공) / 409 (재고 없음)

    @Test
    void 한정수량은_정확히_재고만큼만_팔린다() {
        // given: 재고 3개짜리 상품
        Long productId = createProduct("한정판 굿즈", 10000, 3);

        // when: 4명이 순서대로 주문
        assertThat(order(productId, 1L)).isEqualTo(HttpStatus.CREATED);
        assertThat(order(productId, 2L)).isEqualTo(HttpStatus.CREATED);
        assertThat(order(productId, 3L)).isEqualTo(HttpStatus.CREATED);
        assertThat(order(productId, 4L)).isEqualTo(HttpStatus.CONFLICT); // 재고 소진

        // then: 재고는 정확히 0
        assertThat(stockOf(productId)).isEqualTo(0);
    }

    @Test
    void 상품_상세를_조회할_수_있다() {
        Long productId = createProduct("티셔츠", 25000, 50);

        ResponseEntity<Map> res = http.getForEntity("/products/" + productId, Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().get("name")).isEqualTo("티셔츠");
        assertThat(((Number) res.getBody().get("stock")).intValue()).isEqualTo(50);
    }

    // --- helpers ---

    private Long createProduct(String name, int price, int stock) {
        ResponseEntity<Map> res = http.postForEntity("/products",
                Map.of("name", name, "price", price, "stock", stock), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) res.getBody().get("id")).longValue();
    }

    private HttpStatus order(Long productId, Long userId) {
        ResponseEntity<Map> res = http.postForEntity("/orders",
                Map.of("productId", productId, "userId", userId), Map.class);
        return (HttpStatus) res.getStatusCode();
    }

    private int stockOf(Long productId) {
        ResponseEntity<Map> res = http.getForEntity("/products/" + productId, Map.class);
        return ((Number) res.getBody().get("stock")).intValue();
    }
}
