package ru.aigul.mts_service.dto.application;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationCreateDto {

    @NotNull
    private Long tariffId;

    @NotNull
    @Size(max = 500)
    private String address;

    private Long cityId;

    private List<Long> additionalServiceIds = List.of();
}
