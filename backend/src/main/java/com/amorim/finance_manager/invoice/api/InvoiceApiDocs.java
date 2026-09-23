package com.amorim.finance_manager.invoice.api;

import com.amorim.finance_manager.invoice.dto.*;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.shared.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static com.amorim.finance_manager.config.openapi.OpenApiExamples.*;

public interface InvoiceApiDocs {

    @Operation(
            summary = "Listar faturas",
            description = """
                    Lista as faturas pertencentes ao usuário autenticado.

                    É possível filtrar por cartão de crédito, ano, mês e status.
                    Todos os filtros informados são combinados por AND.

                    A listagem é paginada e não inclui as transações completas
                    de cada fatura. Para consultar as transações, utilize o
                    endpoint de detalhamento da fatura.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Faturas encontradas",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = InvoicePageResponse.class
                            ),
                            examples = @ExampleObject(
                                    name = "Lista paginada de faturas",
                                    value = INVOICE_PAGE_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Filtros ou parâmetros de paginação inválidos",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Erro de validação",
                                            value = VALIDATION_ERROR
                                    ),
                                    @ExampleObject(
                                            name = "Parâmetro inválido",
                                            value = INVALID_REQUEST_ERROR
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Não autenticado",
                                    value = UNAUTHORIZED_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno inesperado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Erro interno",
                                    value = INTERNAL_SERVER_ERROR
                            )
                    )
            )
    })
    ResponseEntity<InvoicePageResponse> list(
            @ParameterObject
            @Valid
            InvoiceFilterRequest filters,

            @ParameterObject
            Pageable pageable
    );

    @Operation(
            summary = "Consultar fatura por ID",
            description = """
                    Retorna os dados completos de uma fatura pertencente
                    ao usuário autenticado.

                    O detalhamento inclui as transações vinculadas à fatura,
                    quando aplicável.

                    Caso a fatura não exista ou pertença a outro usuário,
                    a API retorna HTTP 404.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Fatura encontrada",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = InvoiceDetailResponse.class
                            ),
                            examples = @ExampleObject(
                                    name = "Detalhamento de fatura",
                                    value = INVOICE_DETAIL_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "ID da fatura inválido",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "UUID inválido",
                                    value = INVALID_REQUEST_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Não autenticado",
                                    value = UNAUTHORIZED_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Fatura não encontrada",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Fatura não encontrada",
                                    value = INVOICE_NOT_FOUND
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno inesperado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Erro interno",
                                    value = INTERNAL_SERVER_ERROR
                            )
                    )
            )
    })
    ResponseEntity<InvoiceDetailResponse> findById(
            @Parameter(
                    name = "id",
                    description = "Identificador UUID da fatura",
                    required = true,
                    example = "72486234-ef50-4c7e-99a7-9193a28533a8",
                    schema = @Schema(
                            type = "string",
                            format = "uuid"
                    )
            )
            UUID id
    );

    @Operation(
            summary = "Listar faturas de um cartão",
            description = """
                    Lista as faturas de um cartão de crédito pertencente
                    ao usuário autenticado.

                    A consulta pode ser filtrada por ano, mês e status.
                    Os resultados são paginados.

                    Caso o cartão não exista ou pertença a outro usuário,
                    a API retorna HTTP 404.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Faturas do cartão encontradas",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = InvoicePageResponse.class
                            ),
                            examples = @ExampleObject(
                                    name = "Faturas do cartão",
                                    value = INVOICE_PAGE_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Filtros, UUID ou paginação inválidos",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Erro de validação",
                                            value = VALIDATION_ERROR
                                    ),
                                    @ExampleObject(
                                            name = "Parâmetro inválido",
                                            value = INVALID_REQUEST_ERROR
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Não autenticado",
                                    value = UNAUTHORIZED_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cartão de crédito não encontrado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Cartão não encontrado",
                                    value = CREDIT_CARD_NOT_FOUND
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno inesperado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Erro interno",
                                    value = INTERNAL_SERVER_ERROR
                            )
                    )
            )
    })
    ResponseEntity<InvoicePageResponse> listByCreditCard(
            @Parameter(
                    name = "id",
                    description = "Identificador UUID do cartão de crédito",
                    required = true,
                    example = "0c736743-8885-43d1-813b-c096a4899201",
                    schema = @Schema(
                            type = "string",
                            format = "uuid"
                    )
            )
            UUID id,

            @Parameter(
                    name = "referenceYear",
                    description = "Ano de referência da fatura",
                    example = "2026",
                    schema = @Schema(
                            type = "integer",
                            minimum = "1",
                            maximum = "9999"
                    )
            )
            @Min(value = 1, message = "Ano de referência deve ser positivo")
            @Max(value = 9999, message = "Ano de referência inválido")
            Integer referenceYear,

            @Parameter(
                    name = "referenceMonth",
                    description = "Mês de referência da fatura, de 1 a 12",
                    example = "9",
                    schema = @Schema(
                            type = "integer",
                            minimum = "1",
                            maximum = "12"
                    )
            )
            @Min(value = 1, message = "Mês de referência deve estar entre 1 e 12")
            @Max(value = 12, message = "Mês de referência deve estar entre 1 e 12")
            Integer referenceMonth,

            @Parameter(
                    name = "status",
                    description = "Status atual da fatura",
                    example = "OPEN",
                    schema = @Schema(
                            implementation = InvoiceStatus.class
                    )
            )
            InvoiceStatus status,

            @ParameterObject
            Pageable pageable
    );

    @Operation(
            summary = "Fechar fatura",
            description = """
                Fecha uma fatura pertencente ao usuário autenticado.

                O fechamento somente é permitido depois do término
                do dia indicado em closingDate, considerando o fuso
                configurado na aplicação.

                A operação altera o status de OPEN para CLOSED,
                sem modificar o total da fatura, as transações,
                o limite do cartão ou o saldo bancário.

                expectedVersion deve corresponder à versão atual
                da fatura, obtida no endpoint de consulta.

                Somente faturas OPEN podem ser fechadas.
                
                Faturas CLOSED, PAID ou CANCELLED retornam 409 Conflict.
                Uma nova tentativa de fechamento não altera a fatura.
                
                Após o fechamento, a fatura não aceita novas compras.
                O fechamento não realiza pagamento.
                """,
            requestBody = @RequestBody(
                    required = true,
                    description = "Versão da fatura consultada pelo usuário",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = CloseInvoiceRequest.class
                            ),
                            examples = @ExampleObject(
                                    name = "Fechar fatura",
                                    value = """
                                        {
                                          "expectedVersion": 4
                                        }
                                        """
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = """
                        Fatura alterada de OPEN para CLOSED.
                        Retorna o resumo e a nova versão da fatura.
                        """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = InvoiceSummaryResponse.class
                            ),
                            examples = @ExampleObject(
                                    name = "Fatura fechada",
                                    value = """
                                        {
                                          "id": "72486234-ef50-4c7e-99a7-9193a28533a8",
                                          "creditCardId": "0c736743-8885-43d1-813b-c096a4899201",
                                          "referenceMonth": 8,
                                          "referenceYear": 2026,
                                          "closingDate": "2026-08-20",
                                          "dueDate": "2026-08-28",
                                          "totalAmount": 150.00,
                                          "status": "CLOSED",
                                          "paidAt": null,
                                          "version": 5
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                        UUID inválido, corpo ausente ou malformado,
                        expectedVersion ausente ou negativo.
                        """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = """
                        Fatura inexistente ou pertencente a outro usuário.
                        Código: INVOICE_NOT_FOUND.
                        """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = """
                        Fatura já fechada, paga ou cancelada;
                        dia de fechamento ainda não encerrado;
                        dados inconsistentes ou conflito de versão.
                        
                        Códigos:
                        INVALID_INVOICE_STATUS;
                        OPTIMISTIC_LOCK_CONFLICT.
                        """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Fechamento ainda não permitido",
                                            value = """
                                                {
                                                  "timestamp": "2026-09-08T12:00:00Z",
                                                  "status": 409,
                                                  "code": "INVALID_INVOICE_STATUS",
                                                  "message": "O dia de fechamento da fatura ainda não terminou",
                                                  "path": "/api/v1/invoices/72486234-ef50-4c7e-99a7-9193a28533a8/close",
                                                  "fieldErrors": []
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "Versão desatualizada",
                                            value = """
                                                {
                                                  "timestamp": "2026-09-08T12:00:00Z",
                                                  "status": 409,
                                                  "code": "OPTIMISTIC_LOCK_CONFLICT",
                                                  "message": "O recurso foi alterado por outra operação. Atualize os dados e tente novamente.",
                                                  "path": "/api/v1/invoices/72486234-ef50-4c7e-99a7-9193a28533a8/close",
                                                  "fieldErrors": []
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "Fatura já fechada",
                                            value = """
                                                {
                                                  "timestamp": "2026-09-08T12:00:00Z",
                                                  "status": 409,
                                                  "code": "INVALID_INVOICE_STATUS",
                                                  "message": "Somente faturas abertas podem ser fechadas",
                                                  "path": "/api/v1/invoices/72486234-ef50-4c7e-99a7-9193a28533a8/close",
                                                  "fieldErrors": []
                                                }
                                                """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno inesperado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    })
    ResponseEntity<InvoiceSummaryResponse> close(
            @Parameter(
                    name = "id",
                    in = ParameterIn.PATH,
                    description = "Identificador da fatura que será fechada",
                    required = true,
                    example = "72486234-ef50-4c7e-99a7-9193a28533a8",
                    schema = @Schema(
                            type = "string",
                            format = "uuid"
                    )
            )
            UUID id,
            @Valid CloseInvoiceRequest request
    );

    @Operation(
            summary = "Reabrir fatura",
            description = """
                Reabre uma fatura CLOSED ou PAID pertencente ao usuário autenticado.

                Quando a fatura já estiver paga, o pagamento é revertido na mesma
                operação: o débito da conta é desfeito, créditos aplicados voltam a
                ficar disponíveis, o limite do cartão é recomposto e o registro de
                pagamento é mantido como cancelado no histórico.

                A operação não é permitida se compras posteriores já tiverem usado
                o limite que precisaria ser recomposto.
                """,
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CloseInvoiceRequest.class)
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Fatura reaberta e pronta para correções.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = InvoiceSummaryResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Status, versão ou dados financeiros incompatíveis.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    })
    ResponseEntity<InvoiceSummaryResponse> reopen(UUID id, @Valid CloseInvoiceRequest request);

    @Operation(
            summary = "Quitar fatura",
            description = """
                Quita integralmente uma fatura CLOSED pertencente
                ao usuário autenticado.

                O dia de fechamento deve estar encerrado.
                expectedVersion deve corresponder à versão atual
                da fatura, obtida no endpoint de consulta.

                Créditos disponíveis e elegíveis do mesmo usuário
                e cartão são aplicados primeiro, dos mais antigos
                para os mais recentes.

                São elegíveis créditos cuja fatura de origem possua
                referência anterior à fatura que será quitada.

                Somente a diferença não coberta por créditos é debitada
                da conta e registrada como CREDIT_CARD_PAYMENT.

                sourceAccountId pode ser omitido quando não houver
                valor restante a debitar. Se informado, deve identificar
                uma conta ativa do usuário autenticado.

                A fatura passa para PAID e paidAt registra o momento
                da quitação, inclusive quando ela ocorre só com créditos.

                O total da fatura é preservado.
                O limite é liberado pelo valor integral liquidado.
                Eventual crédito excedente permanece disponível.

                Não são criadas transações bancárias de valor zero.
                A operação não aceita pagamento parcial ou retroativo.

                Aplicação dos créditos, débito bancário, registro do
                pagamento, liberação do limite e quitação acontecem
                na mesma transação.
                """,
            requestBody = @RequestBody(
                    required = true,
                    description = """
                        Versão atual da fatura e conta de origem.

                        A conta é necessária quando os créditos
                        não cobrem integralmente o valor devido.
                        """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = PayInvoiceRequest.class
                            ),
                            examples = {
                                    @ExampleObject(
                                            name = "Pagamento com conta",
                                            value = """
                                                {
                                                  "sourceAccountId": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
                                                  "expectedVersion": 5
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "Quitação integral por créditos",
                                            value = """
                                                {
                                                  "expectedVersion": 5
                                                }
                                                """
                                    )
                            }
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = """
                        Fatura quitada.
                        Retorna o total, o crédito utilizado,
                        o valor debitado e a data da quitação.

                        paymentTransactionId é nulo quando não há
                        débito bancário.
                        """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = InvoicePaymentResponse.class
                            ),
                            examples = {
                                    @ExampleObject(
                                            name = "Crédito e débito bancário",
                                            value = """
                                                {
                                                  "invoiceId": "72486234-ef50-4c7e-99a7-9193a28533a8",
                                                  "totalAmount": 150.00,
                                                  "creditAppliedAmount": 100.00,
                                                  "cashPaidAmount": 50.00,
                                                  "paymentTransactionId": "75d4b4e7-8621-41fe-b71f-34c47e1b581b",
                                                  "paidAt": "2026-09-08T12:00:00Z"
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "Somente créditos",
                                            value = """
                                                {
                                                  "invoiceId": "72486234-ef50-4c7e-99a7-9193a28533a8",
                                                  "totalAmount": 150.00,
                                                  "creditAppliedAmount": 150.00,
                                                  "cashPaidAmount": 0.00,
                                                  "paymentTransactionId": null,
                                                  "paidAt": "2026-09-08T12:00:00Z"
                                                }
                                                """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                        UUID inválido, corpo ausente ou malformado,
                        ou expectedVersion ausente.
                        """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente, inválido ou expirado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = """
                        Fatura, cartão ou conta não encontrados.
                        Recursos de outro usuário também retornam 404.

                        Códigos:
                        INVOICE_NOT_FOUND;
                        CREDIT_CARD_NOT_FOUND;
                        ACCOUNT_NOT_FOUND.
                        """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = """
                        Fatura já quitada, não fechada ou com dados
                        inconsistentes; versão negativa; conta ausente
                        quando existe diferença a debitar; conta inativa;
                        inconsistência de limite ou conflito concorrente.

                        Códigos:
                        INVALID_INVOICE_PAYMENT;
                        INACTIVE_ACCOUNT;
                        OPTIMISTIC_LOCK_CONFLICT.
                        """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Fatura já quitada",
                                            value = """
                                                {
                                                  "timestamp": "2026-09-08T12:00:00Z",
                                                  "status": 409,
                                                  "code": "INVALID_INVOICE_PAYMENT",
                                                  "message": "A fatura já está quitada",
                                                  "path": "/api/v1/invoices/72486234-ef50-4c7e-99a7-9193a28533a8/pay",
                                                  "fieldErrors": []
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "Conta necessária",
                                            value = """
                                                {
                                                  "timestamp": "2026-09-08T12:00:00Z",
                                                  "status": 409,
                                                  "code": "INVALID_INVOICE_PAYMENT",
                                                  "message": "Informe uma conta para pagar o valor restante",
                                                  "path": "/api/v1/invoices/72486234-ef50-4c7e-99a7-9193a28533a8/pay",
                                                  "fieldErrors": []
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "Conflito de concorrência",
                                            value = """
                                                {
                                                  "timestamp": "2026-09-08T12:00:00Z",
                                                  "status": 409,
                                                  "code": "OPTIMISTIC_LOCK_CONFLICT",
                                                  "message": "O recurso foi alterado por outra operação. Atualize os dados e tente novamente.",
                                                  "path": "/api/v1/invoices/72486234-ef50-4c7e-99a7-9193a28533a8/pay",
                                                  "fieldErrors": []
                                                }
                                                """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno inesperado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    })
    ResponseEntity<InvoicePaymentResponse> pay(
            @Parameter(
                    name = "id",
                    in = ParameterIn.PATH,
                    description = "Identificador da fatura que será quitada",
                    required = true,
                    example = "72486234-ef50-4c7e-99a7-9193a28533a8",
                    schema = @Schema(
                            type = "string",
                            format = "uuid"
                    )
            )
            UUID id,
            @Valid PayInvoiceRequest request
    );
}
