package com.amorim.finance_manager.bill.controller;

import com.amorim.finance_manager.bill.dto.*;
import com.amorim.finance_manager.bill.entity.BillStatus;
import com.amorim.finance_manager.bill.service.BillService;
import com.amorim.finance_manager.config.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bills")
@Tag(name = "Boletos")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class BillController {
    private final BillService billService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar boleto único ou parcelas mensais",
            description = "O valor informado é o de cada parcela. Não afeta saldo ou fatura até o pagamento.")
    public List<BillResponse> create(@Valid @RequestBody CreateBillRequest request) {
        return billService.create(request);
    }

    @GetMapping
    @Operation(summary = "Listar boletos do usuário por mês de vencimento")
    public BillPageResponse list(@RequestParam int year, @RequestParam int month,
                                 @RequestParam(required = false) BillStatus status,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        return BillPageResponse.from(billService.list(year, month, status, page, size));
    }

    @GetMapping("/{id}")
    public BillResponse findById(@PathVariable UUID id) { return billService.findById(id); }

    @PatchMapping("/{id}")
    @Operation(summary = "Editar somente este boleto pendente")
    public BillResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateBillRequest request) {
        return billService.update(id, request);
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Registrar pagamento integral do boleto",
            description = "Cria uma despesa comum BOLETO e debita a conta na data informada. Não gera fatura.")
    public BillResponse pay(@PathVariable UUID id, @Valid @RequestBody PayBillRequest request) {
        return billService.pay(id, request);
    }

    @PostMapping("/{id}/cancel")
    public BillResponse cancel(@PathVariable UUID id, @Valid @RequestBody BillVersionRequest request) {
        return billService.cancel(id, request);
    }
}
