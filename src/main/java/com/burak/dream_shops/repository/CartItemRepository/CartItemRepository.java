package com.burak.dream_shops.repository.CartItemRepository;
import com.burak.dream_shops.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CartItemRepository extends JpaRepository<CartItem,Long>{
    void deleteAllByCartId (Long id);

}
