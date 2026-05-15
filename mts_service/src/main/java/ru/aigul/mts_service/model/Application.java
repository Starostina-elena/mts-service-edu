package ru.aigul.mts_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "applications")
@Data
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tariff_id", nullable = false)
    private Tariff tariff;

    @NotNull
    @Column(nullable = false, length = 500)
    private String address;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "passport_verified", nullable = false)
    private Boolean passportVerified = false;

    @Column(name = "technical_feasibility", nullable = false)
    private Boolean technicalFeasibility = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "application_services", joinColumns = @JoinColumn(name = "application_id"), inverseJoinColumns = @JoinColumn(name = "service_id"))
    private Set<Service> additionalServices = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private LocalDateTime updatedAt;
}
