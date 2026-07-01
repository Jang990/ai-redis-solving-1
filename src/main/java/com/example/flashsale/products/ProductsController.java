package com.example.flashsale.products;

import com.example.flashsale.products.dto.ProductCreationRequest;
import com.example.flashsale.products.dto.ProductCreationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductsController {
    private final ProductsRepository productsRepository;

    @PostMapping
    public ResponseEntity<ProductCreationResponse> create(ProductCreationRequest request) {
        Products result = productsRepository.save(new Products(request.name(), request.price(), request.stock()));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ProductCreationResponse(result.getId()));
    }
}
