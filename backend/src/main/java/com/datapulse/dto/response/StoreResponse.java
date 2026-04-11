package com.datapulse.dto.response;

import com.datapulse.model.Store;
import lombok.Data;

@Data
public class StoreResponse {
    private String id;
    private String ownerId;
    private String name;
    private String status;
    private String description;
    private String address;
    private String city;
    private String phone;
    private String logoUrl;

    public static StoreResponse from(Store store) {
        StoreResponse r = new StoreResponse();
        r.id = store.getId();
        r.ownerId = store.getOwnerId();
        r.name = store.getName();
        r.status = store.getStatus();
        r.description = store.getDescription();
        r.address = store.getAddress();
        r.city = store.getCity();
        r.phone = store.getPhone();
        r.logoUrl = store.getLogoUrl();
        return r;
    }
}
