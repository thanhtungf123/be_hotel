package com.luxestay.hotel.service.impl;


import com.luxestay.hotel.model.HotelServiceDTO;
import com.luxestay.hotel.model.entity.HotelService;
import com.luxestay.hotel.repository.HotelServiceRepository;
import com.luxestay.hotel.service.HotelServiceServ;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HotelServiceImplement implements HotelServiceServ {

    private final HotelServiceRepository hotelServiceRepository;

    @Autowired
    public HotelServiceImplement(HotelServiceRepository hotelServiceRepository) {
        this.hotelServiceRepository = hotelServiceRepository;
    }

    @Override
    public List<HotelServiceDTO> getAllServices() {
        List<HotelService> serviceEntities = hotelServiceRepository.findAll();

        return serviceEntities.stream()
                .map(HotelServiceDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public HotelServiceDTO createService(HotelServiceDTO hotelServiceDTO) {
        HotelService hotelService = new HotelService(hotelServiceDTO);
        HotelService savedHotelService = hotelServiceRepository.save(hotelService);
        return new HotelServiceDTO(savedHotelService);
    }

    @Override
    public HotelServiceDTO editService(Long id, HotelServiceDTO hotelServiceDTO) {
        HotelService existingService = hotelServiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));
        if (hotelServiceDTO.getServiceName() != null && !hotelServiceDTO.getServiceName().isEmpty()) {
            existingService.setServiceName(hotelServiceDTO.getServiceName());
        }
        if (hotelServiceDTO.getDescription() != null) {
            existingService.setDescription(hotelServiceDTO.getDescription());
        }
        if (hotelServiceDTO.getPrice() != null) {
            existingService.setPrice(hotelServiceDTO.getPrice());
        }
        if (hotelServiceDTO.getCategory() != null && !hotelServiceDTO.getCategory().isEmpty()) {
            existingService.setCategory(hotelServiceDTO.getCategory());
        }
        if (hotelServiceDTO.getIsAvailable() != null) {
            existingService.setIsAvailable(hotelServiceDTO.getIsAvailable());
        }
        HotelService updatedService = hotelServiceRepository.save(existingService);
        return new HotelServiceDTO(updatedService);
    }

    @Override
    public HotelServiceDTO deleteService(Long id) {
        HotelService existingService = hotelServiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));
        hotelServiceRepository.delete(existingService);
        return new HotelServiceDTO(existingService);
    }


}