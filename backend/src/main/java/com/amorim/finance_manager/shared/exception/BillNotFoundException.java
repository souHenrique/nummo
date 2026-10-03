package com.amorim.finance_manager.shared.exception;

public class BillNotFoundException extends RuntimeException {
    public BillNotFoundException() { super("Boleto não encontrado"); }
}
