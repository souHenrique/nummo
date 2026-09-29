package com.amorim.finance_manager.report.api;

import com.amorim.finance_manager.report.dto.*;
import com.amorim.finance_manager.shared.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static com.amorim.finance_manager.config.openapi.OpenApiExamples.*;

public interface CashFlowReportApiDocs {

    @Operation(
            summary = "Consultar relatório de caixa diário",
            description = """
                    Retorna o caixa do usuário autenticado no dia informado.

                    Considera somente movimentações COMPLETED cuja effectiveDate
                    corresponda à data consultada.

                    INCOME compõe as entradas. EXPENSE e as faturas com
                    vencimento no período compõem as saídas. TRANSFER e
                    CREDIT_CARD_PAYMENT não compõem os totais.

                    invoiceOutflows já está incluído em outflows.
                    As categorias de despesa incluem somente EXPENSE,
                    sem atribuir pagamentos de fatura às categorias das compras.

                    net representa entradas menos saídas do dia,
                    não o saldo atual das contas.

                    Sem movimentações elegíveis, retorna HTTP 200,
                    valores zerados e listas de categorias vazias.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Relatório diário calculado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = DailyCashFlowResponse.class
                            ),
                            examples = @ExampleObject(
                                    name = "Relatório diário",
                                    value = DAILY_CASH_FLOW_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Data obrigatória ausente, formato de data inválido
                            ou período fora do intervalo permitido.
                            """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Data ausente",
                                            value = DAILY_CASH_DATE_REQUIRED_ERROR
                                    ),
                                    @ExampleObject(
                                            name = "Período inválido",
                                            value = DAILY_CASH_INVALID_PERIOD_ERROR
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Autenticação ausente ou token inválido",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Não autenticado",
                                    value = DAILY_CASH_UNAUTHORIZED_ERROR
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
                                    value = DAILY_CASH_INTERNAL_ERROR
                            )
                    )
            )
    })
    ResponseEntity<DailyCashFlowResponse> daily(@ParameterObject CashFlowReportRequest request);

    @Operation(
            summary = "Consultar relatório de caixa semanal",
            description = """
                    Retorna o caixa da semana que contém a data informada,
                    com comparação à semana imediatamente anterior.

                    Cada semana vai de segunda-feira a domingo, inclusive.
                    Para date=2026-09-03, a semana consultada vai de
                    2026-08-31 a 2026-09-06; a anterior vai de
                    2026-08-24 a 2026-08-30.

                    Considera somente movimentações COMPLETED do usuário
                    autenticado, selecionadas pela effectiveDate.

                    INCOME compõe as entradas. EXPENSE e as faturas com
                    vencimento no período compõem as saídas. TRANSFER e
                    CREDIT_CARD_PAYMENT não compõem os totais.

                    invoiceOutflows já está incluído em outflows.
                    As categorias de despesa incluem somente EXPENSE.

                    As diferenças representam semana consultada menos
                    semana anterior, em valores monetários, não percentuais.
                    net não representa o saldo atual das contas.

                    Sem movimentações elegíveis em uma semana, seu resumo
                    contém valores zerados e listas de categorias vazias.

                    Todo o intervalo calculado, incluindo a semana anterior,
                    deve estar entre 0001-01-01 e 9999-12-31.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Relatório semanal e comparação calculados",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = WeeklyCashFlowResponse.class
                            ),
                            examples = @ExampleObject(
                                    name = "Relatório semanal",
                                    value = WEEKLY_CASH_FLOW_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Data obrigatória ausente, formato de data inválido
                            ou intervalo calculado fora dos limites permitidos.
                            """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Data ausente",
                                            value = WEEKLY_CASH_DATE_REQUIRED_ERROR
                                    ),
                                    @ExampleObject(
                                            name = "Período inválido",
                                            value = WEEKLY_CASH_INVALID_PERIOD_ERROR
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Autenticação ausente ou token inválido",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Não autenticado",
                                    value = WEEKLY_CASH_UNAUTHORIZED_ERROR
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
                                    value = WEEKLY_CASH_INTERNAL_ERROR
                            )
                    )
            )
    })
    ResponseEntity<WeeklyCashFlowResponse> weekly(@ParameterObject CashFlowReportRequest request);

    @Operation(
            summary = "Consultar relatório de caixa mensal",
            description = """
                Retorna o fluxo de caixa do usuário autenticado para o mês e ano informados.

                Considera somente movimentações com status COMPLETED, selecionadas
                pela effectiveDate dentro do mês consultado.

                INCOME compõe as entradas. EXPENSE e as faturas com vencimento
                no período compõem as saídas. TRANSFER, CREDIT_CARD_PAYMENT,
                CREDIT_CARD_PURCHASE e ADJUSTMENT não compõem os totais.

                O valor de cada fatura é contado no mês de vencimento, mas não é
                incluído no agrupamento de despesas por categoria. O pagamento
                posterior não é contabilizado novamente, evitando duplicidade.

                net representa entradas menos saídas do mês e não corresponde
                ao saldo atual das contas.

                As receitas e despesas diretas são agrupadas por categoria.
                Sem movimentações elegíveis, retorna HTTP 200, valores zerados
                e listas de categorias vazias.

                Os parâmetros year e month são obrigatórios. O ano deve estar
                entre 1 e 9999 e o mês entre 1 e 12.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Relatório mensal calculado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = MonthlyCashFlowResponse.class
                            ),
                            examples = @ExampleObject(
                                    name = "Relatório mensal",
                                    value = MONTHLY_CASH_FLOW_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                        Ano ou mês ausente, malformado ou fora
                        do intervalo permitido.
                        """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Período mensal inválido",
                                    value = MONTHLY_CASH_VALIDATION_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Autenticação ausente ou token inválido",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Não autenticado",
                                    value = MONTHLY_CASH_UNAUTHORIZED_ERROR
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
                                    value = MONTHLY_CASH_INTERNAL_ERROR
                            )
                    )
            )
    })
    ResponseEntity<MonthlyCashFlowResponse> monthly(@ParameterObject MonthlyCashFlowReportRequest request);

    @Operation(
            summary = "Consultar evolução anual do fluxo de caixa",
            description = """
                Retorna a evolução mensal do fluxo de caixa do usuário autenticado
                para o ano informado.

                O resultado contém obrigatoriamente os 12 meses do ano, ordenados
                de janeiro a dezembro. Meses sem movimentações elegíveis são
                retornados com entradas, saídas e resultado iguais a zero.

                Considera somente movimentações com status COMPLETED, selecionadas
                pela effectiveDate entre o primeiro e o último dia do ano.

                INCOME compõe as entradas. EXPENSE e as faturas com vencimento
                no período compõem as saídas. TRANSFER, CREDIT_CARD_PAYMENT,
                CREDIT_CARD_PURCHASE e ADJUSTMENT não compõem os totais.

                O valor de cada fatura é contado no mês de vencimento. O pagamento
                posterior não é contabilizado novamente, evitando duplicidade.

                Para cada mês, net representa entradas menos saídas. Os valores
                não correspondem ao saldo atual das contas.

                O parâmetro year é obrigatório e deve estar entre 1 e 9999.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Evolução anual calculada com os 12 meses",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = AnnualCashFlowResponse.class
                            ),
                            examples = @ExampleObject(
                                    name = "Evolução anual",
                                    value = ANNUAL_CASH_FLOW_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                        Ano ausente, malformado ou fora
                        do intervalo permitido.
                        """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Ano inválido",
                                    value = ANNUAL_CASH_VALIDATION_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Autenticação ausente ou token inválido",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Não autenticado",
                                    value = ANNUAL_CASH_UNAUTHORIZED_ERROR
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
                                    value = ANNUAL_CASH_INTERNAL_ERROR
                            )
                    )
            )
    })
    ResponseEntity<AnnualCashFlowResponse> annual(@ParameterObject AnnualCashFlowReportRequest request);
}
