package com.amorim.finance_manager.bill.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record BillVersionRequest(@NotNull @PositiveOrZero Long expectedVersion) {}
