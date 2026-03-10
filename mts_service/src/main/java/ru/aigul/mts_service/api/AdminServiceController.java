package ru.aigul.mts_service.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aigul.mts_service.api.dto.ServiceCreateRequest;
import ru.aigul.mts_service.service.AdminService;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminServiceController {

    private final AdminService adminService;

    @PostMapping("/services")
    public ResponseEntity<?> createService(@RequestBody ServiceCreateRequest req) {
        if (req.getName() == null || req.getName().isBlank() || req.getPrice() == null) {
            return ResponseEntity.unprocessableEntity().body("name and price are required");
        }
        long id = adminService.createService(req.getName(), req.getPrice(), req.getDescription());
        return ResponseEntity.status(201).body(java.util.Map.of("id", id));
    }
}

