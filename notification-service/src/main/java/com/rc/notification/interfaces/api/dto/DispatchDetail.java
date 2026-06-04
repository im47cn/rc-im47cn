package com.rc.notification.interfaces.api.dto;

public class DispatchDetail {

    private String subscriberCode;
    private String status;  // QUEUED / IDEMPOTENT_HIT / DEAD_LETTERED

    public DispatchDetail() {
    }

    public DispatchDetail(String subscriberCode, String status) {
        this.subscriberCode = subscriberCode;
        this.status = status;
    }

    // --- Getters & Setters ---

    public String getSubscriberCode() { return subscriberCode; }
    public void setSubscriberCode(String subscriberCode) { this.subscriberCode = subscriberCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
