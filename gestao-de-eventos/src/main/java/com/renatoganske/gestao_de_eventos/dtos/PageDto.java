package com.renatoganske.gestao_de_eventos.dtos;

import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

/**
 * One page of a list response (ADR-0029). Our own contract instead of serializing Spring's
 * {@code PageImpl}, whose JSON shape is not guaranteed to stay the same between versions.
 *
 * @param page zero-based index of this page
 */
public record PageDto<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) implements Serializable {

    public static <E, T> PageDto<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageDto<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
