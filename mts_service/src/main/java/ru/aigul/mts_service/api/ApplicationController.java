package ru.aigul.mts_service.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.aigul.mts_service.dto.application.*;
import ru.aigul.mts_service.model.ApplicationStatus;
import ru.aigul.mts_service.service.ApplicationService;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class
ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationDto create(@RequestHeader("X-User-Id") Long userId,
                                 @Valid @RequestBody ApplicationCreateDto dto) {
        return applicationService.create(userId, dto);
    }

    @GetMapping
    public ApplicationPageDto list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) Long after,
            @RequestParam(defaultValue = "20") int limit) {
        return applicationService.list(userId, status, after, limit);
    }

    @GetMapping("/{applicationId}")
    public ApplicationDetailDto getDetail(@PathVariable Long applicationId) {
        return applicationService.getDetail(applicationId);
    }

    @PostMapping("/{applicationId}/approve")
    public ResponseEntity<ApplicationDto> approve(@RequestHeader("X-User-Id") Long userId,
                                                   @PathVariable Long applicationId) {
        return ResponseEntity.ok(applicationService.approve(userId, applicationId));
    }

    @PostMapping("/{applicationId}/reject")
    public ResponseEntity<ApplicationDto> reject(@RequestHeader("X-User-Id") Long userId,
                                                  @PathVariable Long applicationId,
                                                  @Valid @RequestBody ApplicationRejectDto dto) {
        return ResponseEntity.ok(applicationService.reject(userId, applicationId, dto));
    }
}
