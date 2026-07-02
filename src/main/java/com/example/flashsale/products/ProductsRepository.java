package com.example.flashsale.products;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductsRepository extends JpaRepository<Products, Long> {
    @Query("Select p From Products p Where p.id = :id")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Products> findWithLock(@Param("id") long id);
}
