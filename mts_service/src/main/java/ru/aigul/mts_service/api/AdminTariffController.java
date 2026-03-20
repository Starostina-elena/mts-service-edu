package ru.aigul.mts_service.api;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aigul.mts_service.api.dto.TariffCreateRequest;
import ru.aigul.mts_service.model.Tariff;
import ru.aigul.mts_service.service.AdminTariff;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminTariffController {

    private final AdminTariff adminTariffService;

    @PostMapping("/tariffs")
    public ResponseEntity<?> createTariff(@Valid @RequestBody TariffCreateRequest req) {
        Tariff saved = adminTariffService.createTariff(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(java.util.Map.of("id", saved.getId()));
    }

    @PostMapping("/tariffs/{tariffId}/archive")
    public ResponseEntity<?> archiveTariff(@PathVariable("tariffId") Long tariffId) {
        adminTariffService.archiveTariff(tariffId);
        return ResponseEntity.ok(java.util.Map.of("id", tariffId, "status", "ARCHIVED"));
    }
}
