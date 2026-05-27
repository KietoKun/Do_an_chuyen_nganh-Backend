package com.pizzastore.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Thong tin tao/cap nhat chi nhanh")
public class BranchRequest {
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private Double maxServiceRadiusKm;
    private Integer maxPendingCookOrders;
    private Boolean active;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getMaxServiceRadiusKm() {
        return maxServiceRadiusKm;
    }

    public void setMaxServiceRadiusKm(Double maxServiceRadiusKm) {
        this.maxServiceRadiusKm = maxServiceRadiusKm;
    }

    public Integer getMaxPendingCookOrders() {
        return maxPendingCookOrders;
    }

    public void setMaxPendingCookOrders(Integer maxPendingCookOrders) {
        this.maxPendingCookOrders = maxPendingCookOrders;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
