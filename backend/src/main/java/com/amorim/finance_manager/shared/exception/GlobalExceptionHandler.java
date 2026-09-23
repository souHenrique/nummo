package com.amorim.finance_manager.shared.exception;

import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiError> handleDuplicateEmail(
            DuplicateEmailException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.CONFLICT,
                ApiErrorCode.EMAIL_ALREADY_EXISTS,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<FieldErrorResponse> fieldErrors = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldErrorResponse(
                        error.getField(),
                        Optional.ofNullable(error.getDefaultMessage())
                                .orElse("Valor inválido")
                ))
                .sorted(
                        Comparator.comparing(FieldErrorResponse::field)
                        .thenComparing(FieldErrorResponse::message)
                )
                .toList();
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.VALIDATION_ERROR,
                "Dados de entrada inválidos",
                request,
                fieldErrors
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request
    ) {
        List<FieldErrorResponse> fieldErrors = exception
                .getParameterValidationResults()
                .stream()
                .flatMap(result -> result
                        .getResolvableErrors()
                        .stream()
                        .map(error -> new FieldErrorResponse(
                                Optional.ofNullable(
                                        result.getMethodParameter().getParameterName()
                                ).orElse("request"),
                                Optional.ofNullable(error.getDefaultMessage())
                                        .orElse("Valor inválido")
                        ))
                )
                .toList();
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.VALIDATION_ERROR,
                "Dados de entrada inválidos",
                request,
                fieldErrors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<FieldErrorResponse> fieldErrors = exception
                .getConstraintViolations()
                .stream()
                .map(violation -> new FieldErrorResponse(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.VALIDATION_ERROR,
                "Dados de entrada inválidos",
                request,
                fieldErrors
        );
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiError> handleInvalidRequest(Exception exception, HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_REQUEST,
                "Requisição inválida ou malformada",
                request
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.UNAUTHORIZED,
                ApiErrorCode.INVALID_CREDENTIALS,
                "Credenciais inválidas",
                request
        );
    }

    @ExceptionHandler(UnauthenticatedUserException.class)
    public ResponseEntity<ApiError> handleUnauthenticatedUser(
            UnauthenticatedUserException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.UNAUTHORIZED,
                ApiErrorCode.UNAUTHORIZED,
                "Usuário não autenticado",
                request
        );
    }

    @ExceptionHandler(InvalidProfileUpdateException.class)
    public ResponseEntity<ApiError> handleInvalidProfileUpdate(
            InvalidProfileUpdateException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_PROFILE_UPDATE,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidPasswordChangeException.class)
    public ResponseEntity<ApiError> handleInvalidPasswordChange(
            InvalidPasswordChangeException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_PASSWORD_CHANGE,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidCurrentPasswordException.class)
    public ResponseEntity<ApiError> handleInvalidCurrentPassword(
            InvalidCurrentPasswordException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_CURRENT_PASSWORD,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiError> handleAccountNotFound(
            AccountNotFoundException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.NOT_FOUND,
                ApiErrorCode.ACCOUNT_NOT_FOUND,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidAccountUpdateException.class)
    public ResponseEntity<ApiError> handleInvalidAccountUpdate(
            InvalidAccountUpdateException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_ACCOUNT_UPDATE,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InactiveAccountException.class)
    public ResponseEntity<ApiError> handleInactiveAccount(
            InactiveAccountException exception,
            HttpServletRequest request

    ) {
        return response(
                HttpStatus.CONFLICT,
                ApiErrorCode.INACTIVE_ACCOUNT,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ApiError> handleCategoryNotFound(
            CategoryNotFoundException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.NOT_FOUND,
                ApiErrorCode.CATEGORY_NOT_FOUND,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(IncompatibleCategoryTypeException.class)
    public ResponseEntity<ApiError> handleIncompatibleCategoryType(
            IncompatibleCategoryTypeException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.CATEGORY_TYPE_MISMATCH,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidCategoryUpdateException.class)
    public ResponseEntity<ApiError> handleInvalidCategoryUpdate(
            InvalidCategoryUpdateException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_CATEGORY_UPDATE,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidBalanceAmountException.class)
    public ResponseEntity<ApiError> handleInvalidBalanceAmount(
            InvalidBalanceAmountException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_BALANCE_AMOUNT,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ApiError> handleTransactionNotFound(
            TransactionNotFoundException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.NOT_FOUND,
                ApiErrorCode.TRANSACTION_NOT_FOUND,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ApiError> handleInvalidTransaction(
            InvalidTransactionException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_TRANSACTION,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ResponseEntity<ApiError> handleInvalidTransfer(
            InvalidTransferException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_TRANSFER,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(TransactionAlreadyCancelledException.class)
    public ResponseEntity<ApiError> handleTransactionAlreadyCancelled(
            TransactionAlreadyCancelledException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.CONFLICT,
                ApiErrorCode.TRANSACTION_ALREADY_CANCELLED,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidTransactionStatusException.class)
    public ResponseEntity<ApiError> handleInvalidTransactionStatus(
            InvalidTransactionStatusException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.CONFLICT,
                ApiErrorCode.INVALID_TRANSACTION_STATUS,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler({
            AccountBalanceConflictException.class,
            OptimisticLockingFailureException.class,
            OptimisticLockException.class
    })
    public ResponseEntity<ApiError> handleOptimisticLock(Exception exception, HttpServletRequest request) {
        return response(
                HttpStatus.CONFLICT,
                ApiErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                "O recurso foi alterado por outra operação. Atualize os dados e tente novamente.",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(Exception exception, HttpServletRequest request) {
        log.error(
                "event=api.unexpected_error method={} path={}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCode.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno inesperado",
                request
        );
    }

    @ExceptionHandler(InvalidReportPeriodException.class)
    public ResponseEntity<ApiError> handleInvalidReportPeriod(
            InvalidReportPeriodException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_REPORT_PERIOD,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(CreditCardNotFoundException.class)
    public ResponseEntity<ApiError> handleCreditCardNotFound(
            CreditCardNotFoundException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.NOT_FOUND,
                ApiErrorCode.CREDIT_CARD_NOT_FOUND,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidCreditCardUpdateException.class)
    public ResponseEntity<ApiError> handleInvalidCreditCardUpdate(
            InvalidCreditCardUpdateException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_CREDIT_CARD_UPDATE,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(CreditLimitConflictException.class)
    public ResponseEntity<ApiError> handleCreditLimitConflict(
            CreditLimitConflictException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.CONFLICT,
                ApiErrorCode.CREDIT_LIMIT_CONFLICT,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvoiceNotFoundException.class)
    public ResponseEntity<ApiError> handleInvoiceNotFound(
            InvoiceNotFoundException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.NOT_FOUND,
                ApiErrorCode.INVOICE_NOT_FOUND,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidCreditCardStatusException.class)
    public ResponseEntity<ApiError> handleInvalidCreditCardStatus(
            InvalidCreditCardStatusException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.CONFLICT,
                ApiErrorCode.INVALID_CREDIT_CARD_STATUS,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidInvoiceStatusException.class)
    public ResponseEntity<ApiError> handleInvalidInvoiceStatus(
            InvalidInvoiceStatusException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.CONFLICT,
                ApiErrorCode.INVALID_INVOICE_STATUS,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidCreditCardRefundException.class)
    public ResponseEntity<ApiError> handleInvalidCreditCardRefund(
            InvalidCreditCardRefundException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.CONFLICT,
                ApiErrorCode.INVALID_CREDIT_CARD_REFUND,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(CreditCardPurchaseAlreadyRefundedException.class)
    public ResponseEntity<ApiError> handlePurchaseAlreadyRefunded(
            CreditCardPurchaseAlreadyRefundedException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.CONFLICT,
                ApiErrorCode.CREDIT_CARD_PURCHASE_ALREADY_REFUNDED,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof
                    org.hibernate.exception.ConstraintViolationException violation) {

                String constraint = violation.getConstraintName();

                if ("uk_refund_items_original_transaction".equals(constraint)
                        || "uk_card_credits_refund_item".equals(constraint)) {

                    return response(
                            HttpStatus.CONFLICT,
                            ApiErrorCode.CREDIT_CARD_PURCHASE_ALREADY_REFUNDED,
                            "A compra já possui estorno registrado.",
                            request
                    );
                }
            }

            cause = cause.getCause();
        }

        return handleUnexpectedException(exception, request);
    }

    @ExceptionHandler(InvalidInvoicePaymentException.class)
    public ResponseEntity<ApiError> handleInvalidInvoicePayment(
            InvalidInvoicePaymentException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.CONFLICT,
                ApiErrorCode.INVALID_INVOICE_PAYMENT,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(BudgetNotFoundException.class)
    public ResponseEntity<ApiError> handleBudgetNotFound(
            BudgetNotFoundException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.NOT_FOUND,
                ApiErrorCode.BUDGET_NOT_FOUND,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(BudgetAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleBudgetAlreadyExists(
            BudgetAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.CONFLICT,
                ApiErrorCode.BUDGET_ALREADY_EXISTS,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidBudgetUpdateException.class)
    public ResponseEntity<ApiError> handleInvalidBudgetUpdate(
            InvalidBudgetUpdateException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_BUDGET_UPDATE,
                exception.getMessage(),
                request
        );
    }

    private ResponseEntity<ApiError> response(
            HttpStatus status,
            ApiErrorCode code,
            String message,
            HttpServletRequest request
    ) {
        if (status == HttpStatus.CONFLICT) {
            log.warn(
                    "event=api.conflict code={} method={} path={}",
                    code,
                    request.getMethod(),
                    request.getRequestURI()
            );
        }

        ApiError error = ApiError.of(
                status,
                code,
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(error);
    }

    private ResponseEntity<ApiError> response(
            HttpStatus status,
            ApiErrorCode code,
            String message,
            HttpServletRequest request,
            List<FieldErrorResponse> fieldErrors
    ) {
        ApiError error = ApiError.of(
                status,
                code,
                message,
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity.status(status).body(error);
    }
}
