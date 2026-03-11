package com.datapulse.dto.response;

import com.datapulse.model.Store;
import lombok.Data;

@Data
public class StoreResponse {
    private String id;
    private String ownerId;
    private String name;
    private String status;

    public static StoreResponse from(Store store) {
        StoreResponse r = new StoreResponse();
        r.id = store.getId();
        r.ownerId = store.getOwnerId();
        r.name = store.getName();
        r.status = store.getStatus();
        return r;
    }
}
