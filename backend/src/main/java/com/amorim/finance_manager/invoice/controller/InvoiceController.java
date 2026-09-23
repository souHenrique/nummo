package com.amorim.finance_manager.invoice.controller;

import com.amorim.finance_manager.config.openapi.OpenApiConfig;
import com.amorim.finance_manager.invoice.api.InvoiceApiDocs;
import com.amorim.finance_manager.invoice.dto.*;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.invoice.service.InvoiceClosingService;
import com.amorim.finance_manager.invoice.service.InvoicePaymentService;
import com.amorim.finance_manager.invoice.service.InvoiceQueryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(
        name = "Faturas",
        description = "Consulta de faturas dos cartões do usuário autenticado"
)
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@Validated
public class InvoiceController implements InvoiceApiDocs {

    private final InvoiceQueryService invoiceQueryService;
    private final InvoiceClosingService invoiceClosingService;
    private final InvoicePaymentService invoicePaymentService;

    @Override
    @GetMapping("/invoices")
    public ResponseEntity<InvoicePageResponse> list(
            @ModelAttribute InvoiceFilterRequest filters,
            @PageableDefault(
                    page = 0,
                    size = 20,
                    sort = {
                            "referenceYear",
                            "referenceMonth"
                    },
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                InvoicePageResponse.from(
                        invoiceQueryService.list(filters, pageable)
                )
        );
    }

    @Override
    @GetMapping("/invoices/{id}")
    public ResponseEntity<InvoiceDetailResponse> findById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                invoiceQueryService.findById(id)
        );
    }

    @Override
    @GetMapping("/credit-cards/{id}/invoices")
    public ResponseEntity<InvoicePageResponse> listByCreditCard(
            @PathVariable UUID id,
            @RequestParam(required = false) Integer referenceYear,
            @RequestParam(required = false) Integer referenceMonth,
            @RequestParam(required = false) InvoiceStatus status,
            @PageableDefault(
                    page = 0,
                    size = 20,
                    sort = {
                            "referenceYear",
                            "referenceMonth"
                    },
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                InvoicePageResponse.from(
                        invoiceQueryService.listByCreditCard(
                                id,
                                referenceYear,
                                referenceMonth,
                                status,
                                pageable
                        )
                )
        );
    }

    @Override
    @PostMapping("/invoices/{id}/close")
    public ResponseEntity<InvoiceSummaryResponse> close(
            @PathVariable UUID id,
            @Valid @RequestBody CloseInvoiceRequest request
    ) {
        return ResponseEntity.ok(
                invoiceClosingService.close(
                        id,
                        request.expectedVersion()
                )
        );
    }

    @Override
    @PostMapping("/invoices/{id}/reopen")
    public ResponseEntity<InvoiceSummaryResponse> reopen(
            @PathVariable UUID id,
            @Valid @RequestBody CloseInvoiceRequest request
    ) {
        return ResponseEntity.ok(invoiceClosingService.reopen(id, request.expectedVersion()));
    }

    @Override
    @PostMapping("/invoices/{id}/pay")
    public ResponseEntity<InvoicePaymentResponse> pay(
            @PathVariable UUID id,
            @Valid @RequestBody PayInvoiceRequest request
    ) {
        return ResponseEntity.ok(
                invoicePaymentService.pay(id, request)
        );
    }
}
