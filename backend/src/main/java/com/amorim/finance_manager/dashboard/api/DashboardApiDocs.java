package com.amorim.finance_manager.dashboard.api;

import com.amorim.finance_manager.dashboard.dto.DashboardResponse;
import com.amorim.finance_manager.report.dto.AnnualCashFlowReportRequest;
import com.amorim.finance_manager.report.dto.AnnualCompetenceReportResponse;
import com.amorim.finance_manager.report.dto.CompetenceReportResponse;
import com.amorim.finance_manager.report.dto.MonthlyCashFlowReportRequest;
import com.amorim.finance_manager.shared.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springdoc.core.annotations.ParameterObject;

import static com.amorim.finance_manager.config.openapi.OpenApiExamples.*;

public interface DashboardApiDocs {

    @Operation(
            summary = "Consultar o dashboard financeiro",
            description = """
                    Retorna os indicadores financeiros do usuário autenticado.

                    referenceDate utiliza o fuso horário configurado pela aplicação.
                    Saldo, entradas e saídas mensais, despesas por competência e orçamento
                    representam o mês que contém referenceDate.

                    monthlyBalance corresponde às entradas mensais menos as
                    saídas mensais e menos as compras no cartão vinculadas às
                    faturas de referência do mês. Seu basis é CASH_AND_INVOICE,
                    pois combina movimentos efetivados e compromissos da fatura.

                    monthlyInflows e monthlyOutflows utilizam o regime CASH e
                    selecionam transações pela effectiveDate entre o primeiro
                    e o último dia do mês. Assim, monthlyInflows é acumulado
                    durante todo o mês e só é reiniciado na mudança de período.
                    Pagamentos de fatura compõem monthlyOutflows; compras no
                    cartão não.

                    totalOutflows soma todas as saídas de caixa efetivadas do
                    usuário, também sem incluir compras no cartão.

                    creditCardPurchaseOutflows utiliza o regime COMPETENCE e
                    soma as compras e parcelas vinculadas às faturas de
                    referência do mês, independentemente da competenceDate.

                    competenceExpenses utiliza o regime COMPETENCE e seleciona
                    transações pela competenceDate. Compras no cartão compõem
                    esse indicador; pagamentos de fatura não.

                    openInvoices soma exclusivamente todas as faturas com status OPEN.
                    monthlyOpenInvoices aplica a mesma regra, limitada ao mês e
                    ano de referência do dashboard.

                    budget considera os orçamentos do mês atual e seu consumo
                    por competenceDate.

                    consolidatedBalance soma os saldos atuais das contas.

                    Cada indicador informa explicitamente seu regime por meio
                    do campo basis.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Dashboard calculado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = DashboardResponse.class
                            ),
                            examples = @ExampleObject(
                                    name = "Dashboard financeiro",
                                    value = DASHBOARD_RESPONSE
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
                                    value = DASHBOARD_UNAUTHORIZED_ERROR
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
                                    value = DASHBOARD_INTERNAL_ERROR
                            )
                    )
            )
    })
    ResponseEntity<DashboardResponse> get();

    @Operation(
            summary = "Consultar gastos do gráfico mensal do dashboard",
            description = """
                    Retorna entradas e saídas para o mês informado. Lançamentos
                    diretos usam a data efetiva; compras no cartão usam a data
                    de vencimento da fatura. Pagamentos de fatura não entram
                    novamente, evitando duplicidade.
                    """
    )
    ResponseEntity<CompetenceReportResponse> monthlyChart(
            @ParameterObject MonthlyCashFlowReportRequest request
    );

    @Operation(
            summary = "Consultar evolução anual do gráfico do dashboard",
            description = """
                    Retorna os doze meses do ano. Compras no cartão são exibidas
                    no mês de vencimento de cada fatura, e não no mês da compra.
                    """
    )
    ResponseEntity<AnnualCompetenceReportResponse> annualChart(
            @ParameterObject AnnualCashFlowReportRequest request
    );
}
