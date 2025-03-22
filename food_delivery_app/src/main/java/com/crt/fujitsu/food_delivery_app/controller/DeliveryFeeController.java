package com.crt.fujitsu.food_delivery_app.controller;

import com.crt.fujitsu.food_delivery_app.service_logic.DeliveryFeeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/* Controller class to handle delivery fee requests.
   Gives an endpoint to calculate fee based on city and vehicle type.
*/
@RestController
public class DeliveryFeeController {

    private final DeliveryFeeService deliveryFeeService;

    public DeliveryFeeController(DeliveryFeeService deliveryFeeService) {
        this.deliveryFeeService = deliveryFeeService;
    }

    @GetMapping("/delivery-fee")
    public ResponseEntity<String> getDeliveryFee(@RequestParam String city, @RequestParam String vehicle) {
        String feeMessage = deliveryFeeService.calculateDeliveryFee(city, vehicle);
        return ResponseEntity.ok(feeMessage);
    }
}
