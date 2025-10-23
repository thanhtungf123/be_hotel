package com.luxestay.hotel.controller;


import com.luxestay.hotel.model.HotelServiceDTO;
import com.luxestay.hotel.service.HotelServiceServ;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ServiceController {

    @Autowired
    private HotelServiceServ hotelService;

    @GetMapping("public/getall")
    public List<HotelServiceDTO> getServices() {
        return hotelService.getAllServices();
    }

    @PostMapping("admin/services")
    public HotelServiceDTO addService(@RequestBody HotelServiceDTO hotelServiceDTO) {
        return hotelService.createService(hotelServiceDTO);
    }
    @PutMapping("admin/services/{id}")
    public ResponseEntity<HotelServiceDTO> editService(
            @PathVariable Long id,
            @RequestBody HotelServiceDTO hotelServiceDTO) {

        try {
            HotelServiceDTO updatedServiceDTO = hotelService.editService(id, hotelServiceDTO);

            return ResponseEntity.ok(updatedServiceDTO);

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    @DeleteMapping("admin/services/{id}")
    public ResponseEntity<HotelServiceDTO> deleteService(@PathVariable Long id){
        HotelServiceDTO hotelServiceDTO = hotelService.deleteService(id);
        return  ResponseEntity.ok(hotelServiceDTO);
    }
}
