package com.example.flashsale.products;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Products {
    private Long id;
    private String name;
    private int price;
    private int stock;

    public Products(String name, int price, int stock) {
        this.name = name;
        this.price = price;
        this.stock = stock;
    }
}
