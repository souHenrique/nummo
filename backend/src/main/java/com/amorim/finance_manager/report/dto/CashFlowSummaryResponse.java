package com.amorim.finance_manager.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.util.List;

@Schema(
        description = """
                Resumo das movimentações COMPLETED no período,
                considerando a data de efetivação.
                Transferências e pagamentos de fatura não compõem os totais.
                Faturas com vencimento no período compõem as saídas uma única vez.
                """
)
public record CashFlowSummaryResponse(

        @Schema(
                description = "Total de entradas efetivas do tipo INCOME",
                example = "5000.00"
        )
        BigDecimal inflows,

        @Schema(
                description = """
                        Total de saídas efetivas.
                        Soma das despesas EXPENSE e dos valores das faturas
                        de cartão com vencimento no período.
                        """,
                example = "1500.00"
        )
        BigDecimal outflows,

        @Schema(
                description = """
                        Resultado líquido: inflows menos outflows.
                        Pode ser negativo. Não representa o saldo atual das contas.
                        """,
                example = "3500.00"
        )
        BigDecimal net,

        @Schema(
                description = """
                        Total das faturas de cartão com vencimento no período.
                        Este valor já está incluído em outflows;
                        não deve ser somado novamente.
                        """,
                example = "1200.00"
        )
        BigDecimal invoiceOutflows,

        @Schema(
                description = """
                        Receitas agrupadas por categoria, em ordem decrescente de valor.
                        Subcategorias permanecem separadas das categorias pai.
                        Retorna lista vazia quando não houver receitas.
                        """
        )
        List<CategoryCashFlowResponse> incomeCategories,

        @Schema(
                description = """
                        Despesas EXPENSE agrupadas por categoria,
                        em ordem decrescente de valor.
                        Não inclui o valor consolidado das faturas, exibido
                        separadamente em invoiceOutflows.
                        Subcategorias permanecem separadas das categorias pai.
                        Retorna lista vazia quando não houver despesas diretas.
                        """
        )
        List<CategoryCashFlowResponse> expenseCategories
) {
    /** @deprecated Use {@link #invoiceOutflows()} instead. */
    @Deprecated
    @JsonIgnore
    public BigDecimal invoicePayments() {
        return invoiceOutflows;
    }
}
