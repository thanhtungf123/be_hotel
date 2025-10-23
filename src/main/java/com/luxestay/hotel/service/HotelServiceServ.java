package com.luxestay.hotel.service;


import com.luxestay.hotel.model.HotelServiceDTO;

import java.util.List;

public interface HotelServiceServ {
    List<HotelServiceDTO> getAllServices();

    HotelServiceDTO createService(HotelServiceDTO hotelServiceDTO);

    HotelServiceDTO editService(Long id, HotelServiceDTO hotelServiceDTO);

    HotelServiceDTO deleteService(Long id);
}
