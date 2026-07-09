package com.joprelys.backend.common.api;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * DTO de réponse paginée stable, indépendant de la sérialisation interne de Spring Data PageImpl.
 * Préserve le contrat { content, totalElements, totalPages, size, number } utilisé par le front.
 */
public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int size,
        int number
) {

    public static <T> PageResponse<T> fromPage(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getSize(),
                page.getNumber()
        );
    }
}
