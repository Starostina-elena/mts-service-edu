package ru.aigul.mts_service.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aigul.mts_service.api.dto.ApplicationResponse;
import ru.aigul.mts_service.service.ApplicationService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/account", produces = MediaType.APPLICATION_JSON_VALUE)
public class AccountController {

    private final ApplicationService applicationService;

    @GetMapping("/applications")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(Authentication authentication) {
        String email = extractEmailFromPrincipal(authentication.getPrincipal());
        List<ApplicationResponse> apps = applicationService.getApplicationsForUserEmail(email);
        return ResponseEntity.ok(apps);
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

