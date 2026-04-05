package com.datapulse.service;

import com.datapulse.dto.response.CustomerProfileResponse;
import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.exception.UnauthorizedAccessException;
import com.datapulse.model.CustomerProfile;
import com.datapulse.model.RoleType;
import com.datapulse.repository.CustomerProfileRepository;
import com.datapulse.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerProfileService {

    private final CustomerProfileRepository customerProfileRepository;

    private UserDetailsImpl getCurrentUser(Authentication auth) {
        return (UserDetailsImpl) auth.getPrincipal();
    }

    public CustomerProfileResponse getMyProfile(Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        CustomerProfile profile = customerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("CustomerProfile for userId", currentUser.getId()));
        return CustomerProfileResponse.from(profile);
    }

    public CustomerProfileResponse getProfileById(String id, Authentication auth) {
        UserDetailsImpl currentUser = getCurrentUser(auth);
        RoleType role = currentUser.getRole();

        CustomerProfile profile = customerProfileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CustomerProfile", id));

        boolean isAdmin = role == RoleType.ADMIN;
        boolean isSelf = profile.getUserId().equals(currentUser.getId());

        if (!isAdmin && !isSelf) {
            throw new UnauthorizedAccessException("Access denied: you can only view your own profile");
        }

        return CustomerProfileResponse.from(profile);
    }

    public CustomerProfileResponse updateMyProfile(Authentication auth, CustomerProfile updates) {
        UserDetailsImpl currentUser = getCurrentUser(auth);

        CustomerProfile profile = customerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("CustomerProfile for userId", currentUser.getId()));

        if (updates.getAge() != null) {
            profile.setAge(updates.getAge());
        }
        if (updates.getCity() != null) {
            profile.setCity(updates.getCity());
        }
        if (updates.getMembershipType() != null) {
            profile.setMembershipType(updates.getMembershipType());
        }
        if (updates.getTotalSpend() != null) {
            profile.setTotalSpend(updates.getTotalSpend());
        }
        if (updates.getItemsPurchased() != null) {
            profile.setItemsPurchased(updates.getItemsPurchased());
        }
        if (updates.getAverageRating() != null) {
            profile.setAverageRating(updates.getAverageRating());
        }
        if (updates.getSatisfactionLevel() != null) {
            profile.setSatisfactionLevel(updates.getSatisfactionLevel());
        }

        return CustomerProfileResponse.from(customerProfileRepository.save(profile));
    }
}
