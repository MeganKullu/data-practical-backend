package com.megan.dataproject.payload;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class PageResponse<T> {
    private List<T> content;
    private int number;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static <T> PageResponse<T> from(Page<T> page) {
        PageResponse<T> response = new PageResponse<>();
        response.setContent(page.getContent());
        response.setNumber(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setFirst(page.isFirst());
        response.setLast(page.isLast());
        return response;
    }
}
