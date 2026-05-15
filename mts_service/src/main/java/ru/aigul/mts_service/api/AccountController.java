package ru.aigul.mts_service.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aigul.mts_service.dto.ApplicationResponse;
import ru.aigul.mts_service.dto.BalanceResponse;
import ru.aigul.mts_service.dto.TopUpRequest;
import ru.aigul.mts_service.mapper.AccountApplicationMapper;
import ru.aigul.mts_service.model.Application;
import ru.aigul.mts_service.service.ApplicationService;
import ru.aigul.mts_service.service.BalanceService;

import java.util.List;
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
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(Authentication auth) {
        String email = auth != null ? auth.getName() : null;
        List<Application> apps = applicationService.getApplicationsForUserEmail(email);
        List<ApplicationResponse> dto = apps.stream().map(applicationMapper::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/balance")
    public ResponseEntity<BalanceResponse> getMyBalance(Authentication auth) {
        BalanceResponse resp = balanceService.getBalanceResponseForAuthentication(auth);
        return ResponseEntity.ok(resp);
    }

    @PostMapping(path = "/balance/top-up", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BalanceResponse> topUpBalance(Authentication auth, @Valid @RequestBody TopUpRequest req) {
        boolean ok = balanceService.applyTopUpForAuthentication(auth, req.getAmount());

        if (!ok) {
            return ResponseEntity.badRequest().build();
        }

        BalanceResponse resp = balanceService.getBalanceResponseForAuthentication(auth);
        return ResponseEntity.ok(resp);
    }
}
