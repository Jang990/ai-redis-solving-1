package com.example.flashsale.orders;

import com.example.flashsale.products.Products;
import com.example.flashsale.products.ProductsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrdersService {
    private final ProductsRepository productsRepository;
    private final OrdersRepository ordersRepository;

    @Transactional
    public long order(long productId, long userId) {
        Products products = productsRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 ProductId=" + productId));
        products.decreaseStock();

        return ordersRepository.save(new Orders(productId, userId)).getId();
    }
}
