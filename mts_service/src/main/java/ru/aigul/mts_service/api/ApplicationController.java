package ru.aigul.mts_service.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.aigul.mts_service.dto.CursorPage;
import ru.aigul.mts_service.dto.application.*;
import ru.aigul.mts_service.model.ApplicationStatus;
import ru.aigul.mts_service.service.ApplicationService;
import ru.aigul.mts_service.service.UserService;
import ru.aigul.mts_service.exception.UserNotFoundException;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final UserService userService;

    @Value("${app.pagination.default-limit}")
    private int defaultLimit;

    @Value("${app.auth.user-id-header}")
    private String userIdHeader;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationDto create(@RequestHeader(value = "${app.auth.user-id-header}", required = false) Long userId,
                                 @RequestHeader(value = "X-Auth-Email", required = false) String authEmail,
                                 @RequestHeader(value = "X-Mock-User", required = false) String mockUser,
                                 @Valid @RequestBody ApplicationCreateDto dto) {
        Long effectiveUserId = resolveUserId(userId, authEmail, mockUser);
        return applicationService.create(effectiveUserId, dto);
    }

    @GetMapping
    public CursorPage<ApplicationDto> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit) {
        return applicationService.list(userId, status, after, limit != null ? limit : defaultLimit);
    }

    @GetMapping("/{applicationId}")
    public ApplicationDetailDto getDetail(@PathVariable Long applicationId) {
        return applicationService.getDetail(applicationId);
    }

    @PostMapping("/{applicationId}/approve")
    public ResponseEntity<ApplicationDto> approve(@RequestHeader(value = "${app.auth.user-id-header}", required = false) Long userId,
                                                   @RequestHeader(value = "X-Auth-Email", required = false) String authEmail,
                                                   @RequestHeader(value = "X-Mock-User", required = false) String mockUser,
                                                   @PathVariable Long applicationId) {
        Long effectiveUserId = resolveUserId(userId, authEmail, mockUser);
        return ResponseEntity.ok(applicationService.approve(effectiveUserId, applicationId));
    }

    @PostMapping("/{applicationId}/reject")
    public ResponseEntity<ApplicationDto> reject(@RequestHeader(value = "${app.auth.user-id-header}", required = false) Long userId,
                                                  @RequestHeader(value = "X-Auth-Email", required = false) String authEmail,
                                                  @RequestHeader(value = "X-Mock-User", required = false) String mockUser,
                                                  @PathVariable Long applicationId,
                                                  @Valid @RequestBody ApplicationRejectDto dto) {
        Long effectiveUserId = resolveUserId(userId, authEmail, mockUser);
        return ResponseEntity.ok(applicationService.reject(effectiveUserId, applicationId, dto));
    }

    private Long resolveUserId(Long headerUserId, String authEmail, String mockUser) {
        if (headerUserId != null) return headerUserId;
        String identifier = authEmail != null && !authEmail.isBlank() ? authEmail : mockUser;
        if (identifier != null && !identifier.isBlank()) {
            return userService.findByEmail(identifier)
                    .orElseThrow(() -> new UserNotFoundException(identifier))
                    .getId();
        }
        throw new UserNotFoundException("Unknown user");
    }
}