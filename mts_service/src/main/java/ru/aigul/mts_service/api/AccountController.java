package ru.aigul.mts_service.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aigul.mts_service.api.dto.ApplicationResponse;
import ru.aigul.mts_service.api.dto.BalanceResponse;
import ru.aigul.mts_service.api.dto.CheckFeasibilityRequest;
import ru.aigul.mts_service.api.dto.CheckFeasibilityResponse;
import ru.aigul.mts_service.api.dto.PaymentResponse;
import ru.aigul.mts_service.api.dto.TopUpRequest;
import ru.aigul.mts_service.api.mapper.ApplicationMapper;
import ru.aigul.mts_service.model.Balance;
import ru.aigul.mts_service.model.Application;
import ru.aigul.mts_service.service.ApplicationService;
import ru.aigul.mts_service.service.BalanceService;
import ru.aigul.mts_service.service.TechnicalFeasibilityService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/account", produces = MediaType.APPLICATION_JSON_VALUE)
public class AccountController {

    private final ApplicationService applicationService;
    private final BalanceService balanceService;
    private final TechnicalFeasibilityService technicalFeasibilityService;
    private final ApplicationMapper applicationMapper;

    @GetMapping("/applications")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(Authentication authentication) {
        String email = extractEmailFromPrincipal(authentication.getPrincipal());
        List<Application> apps = applicationService.getApplicationsForUserEmail(email);
        List<ApplicationResponse> dto = apps.stream().map(applicationMapper::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/balance")
    public ResponseEntity<BalanceResponse> getMyBalance(Authentication authentication) {
        String email = extractEmailFromPrincipal(authentication.getPrincipal());
        Optional<Balance> balanceOpt = balanceService.findBalanceForUserEmail(email);

        if (balanceOpt.isEmpty()) {
            BalanceResponse resp = new BalanceResponse(BigDecimal.ZERO, "RUB", OffsetDateTime.now(ZoneOffset.UTC));
            return ResponseEntity.ok(resp);
        }

        Balance b = balanceOpt.get();
        OffsetDateTime updated = b.getUpdatedAt() != null ? b.getUpdatedAt().atOffset(ZoneOffset.UTC) : OffsetDateTime.now(ZoneOffset.UTC);
        BigDecimal amount = b.getAmount() != null ? b.getAmount() : BigDecimal.ZERO;
        BalanceResponse resp = new BalanceResponse(amount, "RUB", updated);
        return ResponseEntity.ok(resp);
    }

    @PostMapping(path = "/balance/top-up", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentResponse> topUpBalance(Authentication authentication, @Valid @RequestBody TopUpRequest req) {
        String email = extractEmailFromPrincipal(authentication.getPrincipal());
        Optional<String> paymentUrlOpt = balanceService.createTopUpPayment(email, req.getAmount());

        if (paymentUrlOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        PaymentResponse resp = new PaymentResponse(paymentUrlOpt.get());
        return ResponseEntity.ok(resp);
    }

    @PostMapping(path = "/check-technical-feasibility", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CheckFeasibilityResponse> checkTechnicalFeasibility(Authentication authentication, @Valid @RequestBody CheckFeasibilityRequest req) {
        TechnicalFeasibilityService.Result result = technicalFeasibilityService.check(req.getAddress(), req.getTariff_id());
        CheckFeasibilityResponse resp = new CheckFeasibilityResponse(result.feasible(), result.reason());
        if (!result.feasible()) {
            return ResponseEntity.unprocessableEntity().body(resp);
        }
        return ResponseEntity.ok(resp);
    }

    private String extractEmailFromPrincipal(Object principal) {
        if (principal instanceof String) {
            return (String) principal;
        }
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return String.valueOf(principal);
    }
}
