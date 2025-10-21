package com.luxestay.hotel.controller;

import com.luxestay.hotel.model.entity.BedLayout;
import com.luxestay.hotel.repository.BedLayoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bed-layouts")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://localhost:4173",
        "http://localhost:3000"
})
@RequiredArgsConstructor
public class BedLayoutController {

    private final BedLayoutRepository bedLayoutRepository;

    /**
     * Get all bed layouts
     * GET /api/bed-layouts
     */
    @GetMapping
    public List<BedLayout> getAllBedLayouts() {
        return bedLayoutRepository.findAll();
    }

    /**
     * Get bed layout by ID
     * GET /api/bed-layouts/{id}
     */
    @GetMapping("/{id}")
    public BedLayout getBedLayoutById(@PathVariable Integer id) {
        return bedLayoutRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bed layout not found"));
    }
}


