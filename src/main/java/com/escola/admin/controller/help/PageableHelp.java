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

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PageableHelp {
    // Default values for pagination
    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;

    public Pageable getPageable(int pageNum, int pageSize, List<SortInput> sort) {

        List<Sort.Order> sortOrders = new ArrayList<>();
        if (sort != null && !sort.isEmpty()) {
            for (SortInput si : sort) {
                Sort.Direction direction = (si.direction() == SortOrder.asc) ? Sort.Direction.ASC : Sort.Direction.DESC;
                sortOrders.add(new Sort.Order(direction, si.property()));
            }
            return PageRequest.of(pageNum, pageSize, Sort.by(sortOrders));
        }
//        else {
//            // Apply default sort if no sort input is provided
//            // This mirrors @PageableDefault(sort = "nome")
//            sortOrders.add(new Sort.Order(Sort.Direction.ASC, ""));
//        }

        return PageRequest.of(pageNum, pageSize);
    }
}
