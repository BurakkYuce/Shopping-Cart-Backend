package com.burak.dream_shops.service.order;

import com.burak.dream_shops.dto.OrderDto;
import com.burak.dream_shops.enums.OrderStatus;
import com.burak.dream_shops.exceptions.ResourcesNotFoundException;
import com.burak.dream_shops.model.Cart;
import com.burak.dream_shops.model.Order;
import com.burak.dream_shops.model.OrderItem;
import com.burak.dream_shops.model.Product;
import com.burak.dream_shops.repository.OrderRepository.OrderRepository;
import com.burak.dream_shops.repository.ProductRepository.ProductRepository;
import com.burak.dream_shops.service.cart.CartService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

/**
 * Sipariş işlemlerini yöneten servis sınıfı.
 * Sipariş oluşturma, getirme ve kullanıcıya ait siparişleri listeleme işlemlerini gerçekleştirir.
 */
@Service
@RequiredArgsConstructor
public class OrderService implements IOrderService{
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final ModelMapper modelMapper;

    /**
     * Kullanıcının mevcut sepetini sipariş olarak kaydeder.
     * Adımlar:
     * 1. Kullanıcının sepetini getir
     * 2. Yeni Order nesnesi oluştur (PENDING statüsünde)
     * 3. Sepetteki her ürün için OrderItem oluştur ve stok miktarını düşür
     * 4. Sipariş toplam tutarını hesapla ve kaydet
     * 5. Siparişi oluşturduktan sonra sepeti temizle
     */
    @Override
    @Transactional
    public Order placeOrder(Long userId) {
        Cart cart = cartService.getCartByUserId(userId);
        Order order = createOrder(cart);
        List<OrderItem> orderItemList = createOrderItems(order, cart);
        order.setOrderItems(new HashSet<>(orderItemList));
        order.setTotalAmount(calculateTotalAmount(orderItemList));
        Order savedOrder = orderRepository.save(order);
        cartService.clearCart(cart.getId()); // Sipariş sonrası sepeti boşalt
        return savedOrder;
    }

    /**
     * Sepet bilgilerinden temel Order nesnesini oluşturur.
     * Kullanıcıyı, sipariş tarihini ve başlangıç statüsünü (PENDING) set eder.
     */
    private Order createOrder(Cart cart) {
        Order order = new Order();
        order.setUser(cart.getUser());
        order.setOrderStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDate.now());
        return order;
    }

    /**
     * Sepetteki CartItem listesini OrderItem listesine dönüştürür.
     * Her ürün için stok miktarını sepetteki kadar azaltır ve ürünü günceller.
     */
    private List<OrderItem> createOrderItems(Order order, Cart cart) {
        return cart.getItems().stream().map(cartItem -> {
            Product product = cartItem.getProduct();
            product.setInventory(product.getInventory() - cartItem.getQuantity()); // stok düş
            productRepository.save(product);
            return new OrderItem(order, product, cartItem.getQuantity(), cartItem.getUnitPrice());
        }).toList();
    }

    /**
     * OrderItem listesindeki tüm kalemlerin toplam tutarını hesaplar.
     * Her kalem için: fiyat * miktar, sonra hepsini topla.
     */
    private BigDecimal calculateTotalAmount(List<OrderItem> orderItemList) {
        return orderItemList.stream()
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Verilen ID'ye sahip siparişi OrderDto olarak döner.
     * ModelMapper ile Order → OrderDto dönüşümü yapar.
     */
    @Override
    public OrderDto getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .map(this::convertToDto)
                .orElseThrow(() -> new ResourcesNotFoundException("Order not found"));
    }

    /**
     * Belirli bir kullanıcının tüm siparişlerini listeler.
     * Her Order → OrderDto'ya dönüştürülerek döner.
     */
    @Override
    public List<OrderDto> getUserOrders(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream().map(this::convertToDto).toList();
    }

    /**
     * Order entity'sini OrderDto'ya dönüştürür (ModelMapper kullanır).
     */
    private OrderDto convertToDto(Order order) {
        return modelMapper.map(order, OrderDto.class);
    }
}
