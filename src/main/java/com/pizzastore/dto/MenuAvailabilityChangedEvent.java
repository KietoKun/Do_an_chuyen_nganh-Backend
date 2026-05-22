package com.pizzastore.dto;

public class MenuAvailabilityChangedEvent {
    private final String event = "MENU_AVAILABILITY_CHANGED";
    private Long branchId;

    public MenuAvailabilityChangedEvent(Long branchId) {
        this.branchId = branchId;
    }

    public String getEvent() {
        return event;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }
}
