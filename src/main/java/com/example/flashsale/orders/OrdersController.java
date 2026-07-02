package com.example.flashsale.orders;

import com.example.flashsale.orders.dto.OrderCreationResponse;
import com.example.flashsale.orders.dto.OrderRequest;
import com.example.flashsale.products.exception.OutOfStockException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrdersController {
    private final OrdersService ordersService;

    @PostMapping
    public ResponseEntity<OrderCreationResponse> orderProduct(
            @RequestBody OrderRequest orderRequest
    ) {
        try {
            long orderId = ordersService.order(orderRequest.productId(), orderRequest.userId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new OrderCreationResponse(orderId));
        } catch(OutOfStockException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
