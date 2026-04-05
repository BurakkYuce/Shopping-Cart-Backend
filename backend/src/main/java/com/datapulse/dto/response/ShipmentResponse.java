package com.datapulse.dto.response;

import com.datapulse.model.Shipment;
import lombok.Data;

@Data
public class ShipmentResponse {
    private String id;
    private String orderId;
    private String warehouse;
    private String mode;
    private String status;
    private Integer customerCareCalls;
    private Integer customerRating;
    private Integer weightGms;

    public static ShipmentResponse from(Shipment shipment) {
        ShipmentResponse r = new ShipmentResponse();
        r.id = shipment.getId();
        r.orderId = shipment.getOrderId();
        r.warehouse = shipment.getWarehouse();
        r.mode = shipment.getMode();
        r.status = shipment.getStatus();
        r.customerCareCalls = shipment.getCustomerCareCalls();
        r.customerRating = shipment.getCustomerRating();
        r.weightGms = shipment.getWeightGms();
        return r;
    }
}
