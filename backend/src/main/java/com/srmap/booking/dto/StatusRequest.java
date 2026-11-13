package com.srmap.booking.dto;

/** Request body for PATCH /api/appointments/{id}/status */
public class StatusRequest {
    private String status;

    public StatusRequest() {}

    public StatusRequest(String status) {
        this.status = status;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
