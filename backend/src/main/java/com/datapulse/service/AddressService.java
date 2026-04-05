package com.datapulse.service;

import com.datapulse.dto.request.CreateAddressRequest;
import com.datapulse.dto.response.AddressResponse;
import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.exception.UnauthorizedAccessException;
import com.datapulse.model.Address;
import com.datapulse.repository.AddressRepository;
import com.datapulse.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    private String getUserId(Authentication auth) {
        return ((UserDetailsImpl) auth.getPrincipal()).getId();
    }

    public List<AddressResponse> getMyAddresses(Authentication auth) {
        return addressRepository.findByUserId(getUserId(auth))
                .stream().map(AddressResponse::from).toList();
    }

    public AddressResponse createAddress(CreateAddressRequest req, Authentication auth) {
        String userId = getUserId(auth);

        if (Boolean.TRUE.equals(req.getIsDefault())) {
            clearDefaultAddress(userId);
        }

        Address address = new Address();
        address.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        address.setUserId(userId);
        address.setTitle(req.getTitle());
        address.setFullName(req.getFullName());
        address.setPhone(req.getPhone());
        address.setAddressLine1(req.getAddressLine1());
        address.setAddressLine2(req.getAddressLine2());
        address.setCity(req.getCity());
        address.setDistrict(req.getDistrict());
        address.setZipCode(req.getZipCode());
        address.setCountry(req.getCountry());
        address.setIsDefault(req.getIsDefault());

        return AddressResponse.from(addressRepository.save(address));
    }

    public AddressResponse updateAddress(String id, CreateAddressRequest req, Authentication auth) {
        String userId = getUserId(auth);
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Address", id));

        if (!address.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Access denied");
        }

        if (Boolean.TRUE.equals(req.getIsDefault())) {
            clearDefaultAddress(userId);
        }

        if (req.getTitle() != null) address.setTitle(req.getTitle());
        if (req.getFullName() != null) address.setFullName(req.getFullName());
        if (req.getPhone() != null) address.setPhone(req.getPhone());
        if (req.getAddressLine1() != null) address.setAddressLine1(req.getAddressLine1());
        if (req.getAddressLine2() != null) address.setAddressLine2(req.getAddressLine2());
        if (req.getCity() != null) address.setCity(req.getCity());
        if (req.getDistrict() != null) address.setDistrict(req.getDistrict());
        if (req.getZipCode() != null) address.setZipCode(req.getZipCode());
        if (req.getCountry() != null) address.setCountry(req.getCountry());
        if (req.getIsDefault() != null) address.setIsDefault(req.getIsDefault());

        return AddressResponse.from(addressRepository.save(address));
    }

    public void deleteAddress(String id, Authentication auth) {
        String userId = getUserId(auth);
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Address", id));

        if (!address.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Access denied");
        }

        addressRepository.delete(address);
    }

    private void clearDefaultAddress(String userId) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(a -> { a.setIsDefault(false); addressRepository.save(a); });
    }
}
