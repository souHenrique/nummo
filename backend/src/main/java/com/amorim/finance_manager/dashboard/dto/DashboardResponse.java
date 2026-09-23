package com.amorim.finance_manager.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Indicadores consolidados do dashboard financeiro")
public record DashboardResponse(

        @Schema(description = "Data utilizada como referência para o dashboard", example = "2026-09-10")
        LocalDate referenceDate,

        @Schema(description = "Ano do período mensal consultado", example = "2026")
        int year,

        @Schema(description = "Mês do período consultado, de 1 a 12", example = "9")
        int month,

        @Schema(
                description = "Primeiro dia do mês considerado",
                example = "2026-09-01",
                format = "date"
        )
        LocalDate periodStart,

        @Schema(
                description = "Último dia do mês considerado",
                example = "2026-09-30",
                format = "date"
        )
        LocalDate periodEnd,

        @Schema(description = "Saldo mensal: entradas e saídas efetivas do mês, incluindo pagamentos de fatura quando realizados")
        DashboardIndicatorResponse monthlyBalance,

        @Schema(description = "Entradas efetivadas durante o mês")
        DashboardIndicatorResponse monthlyInflows,

        @Schema(description = "Soma histórica das saídas de caixa efetivadas")
        DashboardIndicatorResponse totalOutflows,

        @Schema(description = "Saídas de caixa efetivadas durante o mês")
        DashboardIndicatorResponse monthlyOutflows,

        @Schema(description = "Compras no cartão vinculadas às faturas de referência do mês")
        DashboardIndicatorResponse creditCardPurchaseOutflows,

        @Schema(description = "Despesas reconhecidas por competência durante o mês")
        DashboardIndicatorResponse competenceExpenses,

        @Schema(description = "Valor atual das faturas com status OPEN")
        DashboardIndicatorResponse openInvoices,

        @Schema(description = "Valor das faturas com status OPEN referentes ao mês exibido")
        DashboardIndicatorResponse monthlyOpenInvoices,

        @Schema(description = "Resumo dos orçamentos do mês atual")
        DashboardBudgetResponse budget,

        @Schema(description = "Soma dos saldos atuais das contas")
        DashboardIndicatorResponse consolidatedBalance
) {
}
