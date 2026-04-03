package ru.aigul.mts_service.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.aigul.mts_service.dto.CursorPage;
import ru.aigul.mts_service.dto.application.*;
import ru.aigul.mts_service.model.ApplicationStatus;
import ru.aigul.mts_service.service.ApplicationService;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @Value("${app.pagination.default-limit}")
    private int defaultLimit;

    @Value("${app.auth.user-id-header}")
    private String userIdHeader;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('APPLICATION_CREATE')")
    public ApplicationDto create(@RequestHeader("${app.auth.user-id-header}") Long userId,
                                 @Valid @RequestBody ApplicationCreateDto dto) {
        return applicationService.create(userId, dto);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('APPLICATION_READ_OWN') or hasAuthority('APPLICATION_READ_ALL')")
    public CursorPage<ApplicationDto> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit) {
        return applicationService.list(userId, status, after, limit != null ? limit : defaultLimit);
    }

    @GetMapping("/{applicationId}")
    @PreAuthorize("hasAuthority('APPLICATION_READ_OWN') or hasAuthority('APPLICATION_READ_ALL')")
    public ApplicationDetailDto getDetail(@PathVariable Long applicationId) {
        return applicationService.getDetail(applicationId);
    }

    @PostMapping("/{applicationId}/approve")
    @PreAuthorize("hasAuthority('APPLICATION_APPROVE')")
    public ResponseEntity<ApplicationDto> approve(@RequestHeader("${app.auth.user-id-header}") Long userId,
                                                   @PathVariable Long applicationId) {
        return ResponseEntity.ok(applicationService.approve(userId, applicationId));
    }

    @PostMapping("/{applicationId}/reject")
    @PreAuthorize("hasAuthority('APPLICATION_REJECT')")
    public ResponseEntity<ApplicationDto> reject(@RequestHeader("${app.auth.user-id-header}") Long userId,
                                                  @PathVariable Long applicationId,
                                                  @Valid @RequestBody ApplicationRejectDto dto) {
        return ResponseEntity.ok(applicationService.reject(userId, applicationId, dto));
    }
}