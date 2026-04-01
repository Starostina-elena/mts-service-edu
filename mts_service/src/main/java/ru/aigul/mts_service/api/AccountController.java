package ru.aigul.mts_service.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aigul.mts_service.dto.ApplicationResponse;
import ru.aigul.mts_service.dto.BalanceResponse;
import ru.aigul.mts_service.dto.PaymentResponse;
import ru.aigul.mts_service.dto.TopUpRequest;
import ru.aigul.mts_service.mapper.AccountApplicationMapper;
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
    private final AccountApplicationMapper applicationMapper;

    @GetMapping("/applications")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(@AuthenticationPrincipal String email) {
        List<Application> apps = applicationService.getApplicationsForUserEmail(email);
        List<ApplicationResponse> dto = apps.stream().map(applicationMapper::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/balance")
    public ResponseEntity<BalanceResponse> getMyBalance(@AuthenticationPrincipal String email) {
        BalanceResponse resp = balanceService.getBalanceResponseForUserEmail(email);
        return ResponseEntity.ok(resp);
    }

    @PostMapping(path = "/balance/top-up", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentResponse> topUpBalance(@AuthenticationPrincipal String email, @Valid @RequestBody TopUpRequest req) {
        Optional<String> paymentUrlOpt = balanceService.createTopUpPayment(email, req.getAmount());

        if (paymentUrlOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        PaymentResponse resp = new PaymentResponse(paymentUrlOpt.get());
        return ResponseEntity.ok(resp);
    }
}
