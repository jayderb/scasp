package com.scasp.model;
public record CampusService(String serviceId,String name,String description,double latitude,double longitude) {
    public CampusService {
        if(latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)
            throw new IllegalArgumentException("Invalid map coordinates");
    }
}
