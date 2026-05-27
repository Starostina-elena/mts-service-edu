package ru.aigul.mts_service.kafka;

public class ApplicationCreatedEvent {
    private Long applicationId;
    private Long userId;
    private Long tariffId;
    private String address;
    private String createdAt; // ISO-8601 string

    public ApplicationCreatedEvent() {
    }

    public ApplicationCreatedEvent(Long applicationId, Long userId, Long tariffId, String address, String createdAt) {
        this.applicationId = applicationId;
        this.userId = userId;
        this.tariffId = tariffId;
        this.address = address;
        this.createdAt = createdAt;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTariffId() {
        return tariffId;
    }

    public void setTariffId(Long tariffId) {
        this.tariffId = tariffId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
