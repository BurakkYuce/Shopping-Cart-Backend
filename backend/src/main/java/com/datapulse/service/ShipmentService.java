package com.datapulse.service;

import com.datapulse.dto.response.ShipmentResponse;
import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.exception.UnauthorizedAccessException;
import com.datapulse.model.Order;
import com.datapulse.model.RoleType;
import com.datapulse.model.Shipment;
import com.datapulse.model.Store;
import com.datapulse.repository.OrderRepository;
import com.datapulse.repository.ShipmentRepository;
import com.datapulse.repository.StoreRepository;
import com.datapulse.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;

    private UserDetailsImpl getCurrentUser(Authentication auth) {
        return (UserDetailsImpl) auth.getPrincipal();
    }

    private void verifyOrderAccess(Order order, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        if (role == RoleType.INDIVIDUAL) {
            if (!order.getUserId().equals(currentUser.getId())) {
                throw new UnauthorizedAccessException("Access denied: this order does not belong to you");
            }
        } else if (role == RoleType.CORPORATE) {
            List<String> storeIds = storeRepository.findByOwnerId(currentUser.getId())
                    .stream().map(Store::getId).toList();
            if (!storeIds.contains(order.getStoreId())) {
                throw new UnauthorizedAccessException("Access denied: this order is not from your store");
            }
        }
        // ADMIN has access to all
    }

    public ShipmentResponse getByOrderId(String orderId, Authentication auth) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));

        verifyOrderAccess(order, auth);

        Shipment shipment = shipmentRepository.findFirstByOrderId(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Shipment for orderId", orderId));
        return ShipmentResponse.from(shipment);
    }

    public ShipmentResponse getById(String id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Shipment", id));
        return ShipmentResponse.from(shipment);
    }

    public ShipmentResponse createShipment(String orderId, Shipment shipment, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));

        if (role == RoleType.CORPORATE) {
            List<String> storeIds = storeRepository.findByOwnerId(currentUser.getId())
                    .stream().map(Store::getId).toList();
            if (!storeIds.contains(order.getStoreId())) {
                throw new UnauthorizedAccessException("Access denied: this order is not from your store");
            }
        } else if (role != RoleType.ADMIN) {
            throw new UnauthorizedAccessException("Access denied: ADMIN or CORPORATE store owner required");
        }

        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        shipment.setId(id);
        shipment.setOrderId(orderId);

        return ShipmentResponse.from(shipmentRepository.save(shipment));
    }

    public ShipmentResponse updateShipment(String id, Shipment updates, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Shipment", id));

        Order order = orderRepository.findById(shipment.getOrderId())
                .orElseThrow(() -> new EntityNotFoundException("Order", shipment.getOrderId()));

        if (role == RoleType.CORPORATE) {
            List<String> storeIds = storeRepository.findByOwnerId(currentUser.getId())
                    .stream().map(Store::getId).toList();
            if (!storeIds.contains(order.getStoreId())) {
                throw new UnauthorizedAccessException("Access denied: this shipment's order is not from your store");
            }
        } else if (role != RoleType.ADMIN) {
            throw new UnauthorizedAccessException("Access denied: ADMIN or CORPORATE store owner required");
        }

        if (updates.getStatus() != null) {
            shipment.setStatus(updates.getStatus());
        }
        if (updates.getMode() != null) {
            shipment.setMode(updates.getMode());
        }
        if (updates.getWarehouse() != null) {
            shipment.setWarehouse(updates.getWarehouse());
        }
        if (updates.getCustomerCareCalls() != null) {
            shipment.setCustomerCareCalls(updates.getCustomerCareCalls());
        }
        if (updates.getCustomerRating() != null) {
            shipment.setCustomerRating(updates.getCustomerRating());
        }
        if (updates.getWeightGms() != null) {
            shipment.setWeightGms(updates.getWeightGms());
        }

        return ShipmentResponse.from(shipmentRepository.save(shipment));
    }

    public void deleteShipment(String id, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        if (role != RoleType.ADMIN) {
            throw new UnauthorizedAccessException("Access denied: ADMIN role required");
        }

        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Shipment", id));

        shipmentRepository.delete(shipment);
    }
}
