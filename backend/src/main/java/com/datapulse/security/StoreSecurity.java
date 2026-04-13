package com.datapulse.security;

import com.datapulse.model.Store;
import com.datapulse.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StoreSecurity {

    private final StoreRepository storeRepository;

    public boolean isOwner(Authentication auth, String storeId) {
        if (auth == null || storeId == null) return false;
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
        if ("ADMIN".equals(user.getRole().name())) return true;
        return storeRepository.findById(storeId)
                .map(store -> store.getOwnerId().equals(user.getId()))
                .orElse(false);
    }
}
