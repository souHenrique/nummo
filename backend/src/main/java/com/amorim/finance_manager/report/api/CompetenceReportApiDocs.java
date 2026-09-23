package com.amorim.finance_manager.report.api;

import com.amorim.finance_manager.report.dto.CompetenceReportRequest;
import com.amorim.finance_manager.report.dto.CompetenceReportResponse;
import com.amorim.finance_manager.report.dto.AnnualCashFlowReportRequest;
import com.amorim.finance_manager.report.dto.AnnualCompetenceReportResponse;
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

public interface CompetenceReportApiDocs {

    @Operation(
            summary = "Consultar relatório por competência",
            description = """
                    Retorna o relatório financeiro do usuário autenticado
                    dentro do período informado.

                    O período é selecionado exclusivamente por competenceDate,
                    com as datas inicial e final incluídas no cálculo.

                    São consideradas somente transações com status COMPLETED.

                    INCOME compõe o total de receitas.

                    EXPENSE e CREDIT_CARD_PURCHASE compõem o total de despesas.
                    Uma compra no cartão é reconhecida como despesa na sua
                    data de competência, independentemente da data em que
                    a respectiva fatura será paga.

                    CREDIT_CARD_PAYMENT não é contabilizado como nova despesa,
                    pois representa apenas a quitação financeira de compras
                    que já foram reconhecidas por competência. Essa exclusão
                    evita a duplicação das despesas com cartão.

                    TRANSFER e ADJUSTMENT também não compõem os totais.

                    As receitas e despesas são agrupadas por categoria.
                    Categorias diferentes permanecem separadas mesmo quando
                    possuem o mesmo nome.

                    result representa totalIncome menos totalExpenses.
                    O resultado pode ser negativo e não representa o saldo
                    atual das contas.

                    Quando não houver movimentações elegíveis no período,
                    retorna HTTP 200, valores zerados e listas de categorias
                    vazias.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Relatório por competência calculado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = CompetenceReportResponse.class
                            ),
                            examples = @ExampleObject(
                                    name = "Relatório por competência",
                                    value = COMPETENCE_REPORT_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Datas obrigatórias ausentes, formato inválido
                            ou data inicial posterior à data final.
                            """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Datas obrigatórias ausentes",
                                            value = COMPETENCE_REPORT_VALIDATION_ERROR
                                    ),
                                    @ExampleObject(
                                            name = "Período inválido",
                                            value = COMPETENCE_REPORT_INVALID_PERIOD_ERROR
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
                                    value = COMPETENCE_REPORT_UNAUTHORIZED_ERROR
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
                                    value = COMPETENCE_REPORT_INTERNAL_ERROR
                            )
                    )
            )
    })
    ResponseEntity<CompetenceReportResponse> generate(@ParameterObject CompetenceReportRequest request);

    @Operation(
            summary = "Consultar evolução anual por competência",
            description = """
                    Retorna os totais mensais por data da despesa. Compras no cartão
                    entram no mês de cada compra ou parcela; o pagamento da fatura
                    não é incluído como uma nova saída.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Evolução anual por competência calculada",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AnnualCompetenceReportResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Ano inválido"),
            @ApiResponse(responseCode = "401", description = "Autenticação ausente ou token inválido")
    })
    ResponseEntity<AnnualCompetenceReportResponse> annual(
            @ParameterObject AnnualCashFlowReportRequest request
    );
}
