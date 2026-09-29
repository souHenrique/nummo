package com.amorim.finance_manager.report.projection;

import java.math.BigDecimal;

public record InvoiceDueMonthAggregate(Integer month, BigDecimal amount) {
}
