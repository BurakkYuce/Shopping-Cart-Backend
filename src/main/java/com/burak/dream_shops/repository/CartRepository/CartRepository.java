package com.burak.dream_shops.repository.CartRepository;
import com.burak.dream_shops.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart,Long >{
    Cart findByUserId(Long userId);
}
