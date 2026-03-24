package ru.aigul.mts_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Function;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursorPage<T> {
    private List<T> items;
    private Long nextCursor;

    public static <E, T> CursorPage<T> of(List<E> raw, int limit,
                                           Function<E, T> mapper,
                                           Function<E, Long> idExtractor) {
        boolean hasNext = raw.size() > limit;
        List<E> page = hasNext ? raw.subList(0, limit) : raw;
        Long next = hasNext ? idExtractor.apply(page.get(page.size() - 1)) : null;
        return new CursorPage<>(page.stream().map(mapper).toList(), next);
    }
}
