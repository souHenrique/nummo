package com.amorim.finance_manager.shared.exception;

public class InvalidCurrentPasswordException extends RuntimeException {

    public InvalidCurrentPasswordException() {
        super("A senha atual está incorreta.");
    }
}
