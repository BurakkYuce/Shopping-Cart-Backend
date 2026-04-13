package com.datapulse.service;

import com.datapulse.dto.request.CreateStoreRequest;
import com.datapulse.dto.response.StoreResponse;
import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.exception.UnauthorizedAccessException;
import com.datapulse.logging.LogEventPublisher;
import com.datapulse.logging.LogEventType;
import com.datapulse.model.RoleType;
import com.datapulse.model.Store;
import com.datapulse.model.enums.StoreStatus;
import com.datapulse.repository.StoreRepository;
import com.datapulse.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final LogEventPublisher logEventPublisher;

    private UserDetailsImpl getCurrentUser(Authentication auth) {
        return (UserDetailsImpl) auth.getPrincipal();
    }

    public List<StoreResponse> getStores(Authentication auth) {
        if (auth == null) {
            return storeRepository.findAll().stream().map(StoreResponse::from).toList();
        }

        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        List<Store> stores;
        if (role == RoleType.ADMIN) {
            stores = storeRepository.findAll();
        } else if (role == RoleType.CORPORATE) {
            stores = storeRepository.findByOwnerId(currentUser.getId());
        } else {
            // INDIVIDUAL - read-only access to all stores
            stores = storeRepository.findAll();
        }

        return stores.stream().map(StoreResponse::from).toList();
    }

    public StoreResponse getStoreById(String id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Store", id));
        return StoreResponse.from(store);
    }

    public StoreResponse createStore(CreateStoreRequest request, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        if (role != RoleType.CORPORATE && role != RoleType.ADMIN) {
            throw new UnauthorizedAccessException("Access denied: CORPORATE or ADMIN role required");
        }

        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        Store store = new Store();
        store.setId(id);
        store.setOwnerId(currentUser.getId());
        store.setName(request.getName());
        store.setStatus(StoreStatus.PENDING_APPROVAL);
        store.setDescription(request.getDescription());
        store.setAddress(request.getAddress());
        store.setCity(request.getCity());
        store.setPhone(request.getPhone());
        store.setLogoUrl(request.getLogoUrl());

        storeRepository.save(store);
        return StoreResponse.from(store);
    }

    public StoreResponse updateStore(String id, CreateStoreRequest request, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Store", id));

        if (role == RoleType.CORPORATE && !store.getOwnerId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("Access denied: you do not own this store");
        } else if (role != RoleType.ADMIN && role != RoleType.CORPORATE) {
            throw new UnauthorizedAccessException("Access denied: insufficient permissions");
        }

        if (request.getName() != null) {
            store.setName(request.getName());
        }
        if (request.getDescription() != null) {
            store.setDescription(request.getDescription());
        }
        if (request.getAddress() != null) {
            store.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            store.setCity(request.getCity());
        }
        if (request.getPhone() != null) {
            store.setPhone(request.getPhone());
        }
        if (request.getLogoUrl() != null) {
            store.setLogoUrl(request.getLogoUrl());
        }

        storeRepository.save(store);
        return StoreResponse.from(store);
    }

    public StoreResponse updateStatus(String storeId, StoreStatus newStatus, String actingAdminId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("Store", storeId));
        StoreStatus previous = store.getStatus();
        store.setStatus(newStatus);
        storeRepository.save(store);

        logEventPublisher.publish(
                LogEventType.STORE_STATUS_CHANGED,
                actingAdminId,
                "ADMIN",
                Map.of(
                        "store_id", storeId,
                        "previous_status", previous != null ? previous.name() : "null",
                        "new_status", newStatus.name()));

        return StoreResponse.from(store);
    }

    public void requireActive(String storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("Store", storeId));
        if (store.getStatus() != StoreStatus.ACTIVE) {
            throw new UnauthorizedAccessException(
                    "Store is " + store.getStatus().name() + " — operation not permitted");
        }
    }

    public void deleteStore(String id, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Store", id));

        if (role == RoleType.CORPORATE && !store.getOwnerId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("Access denied: you do not own this store");
        } else if (role != RoleType.ADMIN && role != RoleType.CORPORATE) {
            throw new UnauthorizedAccessException("Access denied: insufficient permissions");
        }

        storeRepository.delete(store);
    }
}
