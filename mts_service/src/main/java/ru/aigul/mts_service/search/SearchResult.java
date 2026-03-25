package ru.aigul.mts_service.search;

import ru.aigul.mts_service.dto.CursorPage;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchResult<T> {
    private CursorPage<T> page;
    private String source; // "es" or "jpa"
}

