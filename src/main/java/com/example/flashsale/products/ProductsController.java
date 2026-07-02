package com.example.flashsale.products;

import com.example.flashsale.products.dto.ProductCreationRequest;
import com.example.flashsale.products.dto.ProductCreationResponse;
import com.example.flashsale.products.dto.ProductDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductsController {
    private final ProductsRepository productsRepository;

    @PostMapping
    public ResponseEntity<ProductCreationResponse> create(
            @RequestBody ProductCreationRequest request
    ) {
        Products result = productsRepository.save(new Products(request.name(), request.price(), request.stock()));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ProductCreationResponse(result.getId()));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponse> findProductById(
            @PathVariable("productId") long productId
    ) {
        Products result = productsRepository.findById(productId)
                .orElseThrow(IllegalArgumentException::new);

        return ResponseEntity.ok(
                new ProductDetailResponse(
                        result.getId(), result.getName(),
                        result.getPrice(), result.getStock()
                )
        );
    }
}
