package com.datapulse.dto.response;

import com.datapulse.model.Address;
import lombok.Data;

@Data
public class AddressResponse {
    private String id;
    private String title;
    private String fullName;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String district;
    private String zipCode;
    private String country;
    private Boolean isDefault;

    public static AddressResponse from(Address a) {
        AddressResponse r = new AddressResponse();
        r.id = a.getId();
        r.title = a.getTitle();
        r.fullName = a.getFullName();
        r.phone = a.getPhone();
        r.addressLine1 = a.getAddressLine1();
        r.addressLine2 = a.getAddressLine2();
        r.city = a.getCity();
        r.district = a.getDistrict();
        r.zipCode = a.getZipCode();
        r.country = a.getCountry();
        r.isDefault = a.getIsDefault();
        return r;
    }
}
