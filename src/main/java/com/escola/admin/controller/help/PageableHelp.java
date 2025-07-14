package com.escola.admin.controller.help;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PageableHelp {
    // Default values for pagination
    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;

    public Pageable getPageable(Optional<Integer> page, Optional<Integer> size, Optional<List<SortInput>> sort) {
        int pageNum = page.orElse(DEFAULT_PAGE_NUMBER); // Use default 0 if not provided
        int pageSize = size.orElse(DEFAULT_PAGE_SIZE); // Use default 10 if not provided

        List<Sort.Order> sortOrders = new ArrayList<>();
        if (sort.isPresent()) {
            for (SortInput si : sort.get()) {
                Sort.Direction direction = (si.direction() == SortOrder.asc) ? Sort.Direction.ASC : Sort.Direction.DESC;
                sortOrders.add(new Sort.Order(direction, si.property()));
            }
        } else {
            // Apply default sort if no sort input is provided
            // This mirrors @PageableDefault(sort = "nome")
            sortOrders.add(new Sort.Order(Sort.Direction.ASC, "nome"));
        }

        return PageRequest.of(pageNum, pageSize, Sort.by(sortOrders));
    }
}
