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
import ru.aigul.mts_service.api.dto.PaymentResponse;
import ru.aigul.mts_service.api.dto.TopUpRequest;
import ru.aigul.mts_service.api.mapper.ApplicationMapper;
import ru.aigul.mts_service.model.Application;
import ru.aigul.mts_service.service.ApplicationService;
import ru.aigul.mts_service.service.BalanceService;

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
        BalanceResponse resp = balanceService.getBalanceResponseForUserEmail(email);
        return ResponseEntity.ok(resp);
    }

    @PostMapping(path = "/balance/top-up", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentResponse> topUpBalance(Authentication authentication, @Valid @RequestBody TopUpRequest req) {
        String email = extractEmailFromPrincipal(authentication.getPrincipal());
        Optional<String> paymentUrlOpt = balanceService.createTopUpPayment(email, req.getAmount());

        if (paymentUrlOpt.isEmpty()) {
            System.out.println("Payment url is empty");
            return ResponseEntity.badRequest().build();
        }

        PaymentResponse resp = new PaymentResponse(paymentUrlOpt.get());
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
