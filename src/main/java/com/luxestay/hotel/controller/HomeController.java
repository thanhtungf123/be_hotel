package com.luxestay.hotel.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
    // Kiểm tra tính sống của server
    @GetMapping("/api/health")
    public String health() { return "OK"; }
}
