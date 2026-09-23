package com.amorim.finance_manager.creditcard.controller;

import com.amorim.finance_manager.config.openapi.OpenApiConfig;
import com.amorim.finance_manager.creditcard.api.CreditCardApiDocs;
import com.amorim.finance_manager.creditcard.dto.*;
import com.amorim.finance_manager.creditcard.service.CreditCardPurchaseRefundService;
import com.amorim.finance_manager.creditcard.service.CreditCardPurchaseService;
import com.amorim.finance_manager.creditcard.service.CreditCardService;
import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/credit-cards")
@Tag(
        name = "Cartões de crédito",
        description = "Gerenciamento dos cartões de crédito do usuário autenticado"
)
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class CreditCardController implements CreditCardApiDocs {

    private final CreditCardService creditCardService;
    private final CreditCardPurchaseService creditCardPurchaseService;
    private final CreditCardPurchaseRefundService creditCardPurchaseRefundService;

    @Override
    @PostMapping
    public ResponseEntity<CreditCardResponse> create(@Valid @RequestBody CreateCreditCardRequest request) {
        CreditCardResponse response = creditCardService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Override
    @GetMapping
    public ResponseEntity<List<CreditCardResponse>> findAll() {
        return ResponseEntity.ok(creditCardService.findAll());
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<CreditCardResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(creditCardService.findById(id));
    }

    @Override
    @PatchMapping("/{id}")
    public ResponseEntity<CreditCardResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCreditCardRequest request
    ) {
        return ResponseEntity.ok(
                creditCardService.update(id, request)
        );
    }

    @Override
    @PostMapping("/{id}/purchases")
    public ResponseEntity<List<TransactionResponse>> createPurchase(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCreditCardPurchaseRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(creditCardPurchaseService.create(id, request));
    }

    @PatchMapping("/{id}/purchases/{transactionId}")
    public ResponseEntity<List<TransactionResponse>> updatePurchase(
            @PathVariable UUID id,
            @PathVariable UUID transactionId,
            @Valid @RequestBody UpdateCreditCardPurchaseRequest request
    ) {
        return ResponseEntity.ok(creditCardPurchaseService.update(id, transactionId, request));
    }

    @Override
    @PostMapping("/{creditCardId}/purchase/{transactionId}/refund")
    public ResponseEntity<CreditCardRefundResponse> refundPurchase(
            @PathVariable("creditCardId") UUID creditCardId,
            @PathVariable("transactionId") UUID transactionId,
            @Valid @RequestBody CreditCardRefundRequest request
    ) {
        return ResponseEntity.ok(
                creditCardPurchaseRefundService.refundPurchase(
                        creditCardId,
                        transactionId,
                        request
                )
        );
    }
}
