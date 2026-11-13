package com.Vishleshan.Api.dto;

public class EmployeeStatusRequest {

    private boolean active;

    public EmployeeStatusRequest() {
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}