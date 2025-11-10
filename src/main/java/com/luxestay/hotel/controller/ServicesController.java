package com.luxestay.hotel.controller;

import com.luxestay.hotel.model.Services;
import com.luxestay.hotel.service.ServicesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://localhost:4173",
        "http://localhost:3000"
})
@RequiredArgsConstructor
public class ServicesController {
    private final ServicesService servicesService;

    /**
     * Get all available services (public endpoint for booking page)
     */
    @GetMapping
    public List<Services> getAllServices() {
        return servicesService.getAll();
    }

    /**
     * Get service by ID (public endpoint)
     */
    @GetMapping("/{id}")
    public Services getService(@PathVariable Integer id) {
        return servicesService.findById(id);
    }
}


