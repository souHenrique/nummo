package com.amorim.finance_manager.bill.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record BillPageResponse(
        List<BillResponse> content, int number, int size, long totalElements, int totalPages
) {
    public static BillPageResponse from(Page<BillResponse> page) {
        return new BillPageResponse(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
