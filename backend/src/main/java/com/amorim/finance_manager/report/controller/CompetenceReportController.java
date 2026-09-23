package com.amorim.finance_manager.report.controller;

import com.amorim.finance_manager.config.openapi.OpenApiConfig;
import com.amorim.finance_manager.report.api.CompetenceReportApiDocs;
import com.amorim.finance_manager.report.dto.CompetenceReportRequest;
import com.amorim.finance_manager.report.dto.CompetenceReportResponse;
import com.amorim.finance_manager.report.dto.AnnualCashFlowReportRequest;
import com.amorim.finance_manager.report.dto.AnnualCompetenceReportResponse;
import com.amorim.finance_manager.report.service.CompetenceReportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports/competence")
@Tag(name = "Relatórios por competência")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class CompetenceReportController implements CompetenceReportApiDocs {

    private final CompetenceReportService reportService;

    @Override
    @GetMapping
    public ResponseEntity<CompetenceReportResponse> generate(@Valid @ModelAttribute CompetenceReportRequest request) {
        return ResponseEntity.ok(
                reportService.generate(
                        request.startDate(),
                        request.endDate()
                )
        );
    }

    @Override
    @GetMapping("/annual")
    public ResponseEntity<AnnualCompetenceReportResponse> annual(
            @Valid @ModelAttribute AnnualCashFlowReportRequest request
    ) {
        return ResponseEntity.ok(reportService.annual(request.year()));
    }
}
