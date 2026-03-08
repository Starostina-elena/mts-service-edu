package ru.aigul.mts_service.api.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogPageDto<T> {
    private List<T> items;
    private Long nextCursor;
}
