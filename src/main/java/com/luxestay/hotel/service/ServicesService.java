package com.luxestay.hotel.service;


import com.luxestay.hotel.model.Services;
import com.luxestay.hotel.repository.ServicesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicesService {
    @Autowired
    private ServicesRepository servicesRepository;

    public void addService(Services service) {
        servicesRepository.save(service);
    }

    public void editService(int id,Services newService) {
        Services s = servicesRepository.getReferenceById(id);
        if(s!=null) {
            if(newService.getNameService()!=null) {s.setNameService(newService.getNameService());}
            if(newService.getPrice() >= 0) {s.setPrice(newService.getPrice());}
            } else {
            System.out.println("Service not found");
        }
        servicesRepository.save(s);
    }

    public void deleteService(int id) {
        servicesRepository.deleteById(id);
    }

    public List<Services> getAll() {
        return servicesRepository.findAll();
    }

    public Services findById(int id) {
        return servicesRepository.getReferenceById(id);
    }
}
