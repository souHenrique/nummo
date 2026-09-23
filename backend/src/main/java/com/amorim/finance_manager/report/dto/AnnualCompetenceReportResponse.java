package com.amorim.finance_manager.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Evolução mensal das receitas e despesas reconhecidas por competência")
public record AnnualCompetenceReportResponse(

        @Schema(example = "2026")
        int year,

        @Schema(example = "2026-01-01", format = "date")
        LocalDate startDate,

        @Schema(example = "2026-12-31", format = "date")
        LocalDate endDate,

        List<AnnualCashFlowMonthResponse> evolution
) {
}
