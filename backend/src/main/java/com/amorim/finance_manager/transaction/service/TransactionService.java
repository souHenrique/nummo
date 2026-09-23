package com.amorim.finance_manager.transaction.service;

import com.amorim.finance_manager.account.entity.Account;
import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.account.service.AccountBalanceService;
import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.entity.CategoryStatus;
import com.amorim.finance_manager.category.entity.CategoryType;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.shared.exception.*;
import com.amorim.finance_manager.transaction.dto.CreateTransactionRequest;
import com.amorim.finance_manager.transaction.dto.TransactionFilterRequest;
import com.amorim.finance_manager.transaction.dto.TransactionInstallmentDetailsResponse;
import com.amorim.finance_manager.transaction.dto.TransactionListItemResponse;
import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import com.amorim.finance_manager.transaction.dto.UpdateTransactionRequest;
import com.amorim.finance_manager.transaction.entity.PaymentMethod;
import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.transaction.mapper.TransactionMapper;
import com.amorim.finance_manager.transaction.repository.TransactionRepository;
import com.amorim.finance_manager.transaction.projection.InstallmentGroupTotal;
import com.amorim.finance_manager.transaction.specification.TransactionSpecifications;
import com.amorim.finance_manager.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final AccountBalanceService accountBalanceService;
    private final CurrentUserService currentUserService;
    private final TransactionImpactService transactionImpactService;

    private static final Sort EXPORT_SORT =
            Sort.by(Sort.Direction.DESC, "competenceDate")
                    .and(Sort.by(Sort.Direction.ASC, "id"));

    @Transactional
    public TransactionResponse create(CreateTransactionRequest request) {
        UUID userId = currentUserService.getCurrentUserId();

        validateRequest(request);

        Category category = findOwnedCategory(request.categoryId(), userId);

        validateCategory(category, request.type());

        UUID accountId = resolveAccountId(request);

        if (request.status() == TransactionStatus.PENDING) {
            validateOwnedActiveAccount(accountId, userId);
        } else {
            applyBalance(request, userId, accountId);
        }

        Transaction transaction = transactionMapper.toEntity(request);

        transaction.setUserId(userId);

        Transaction saved = transactionRepository.saveAndFlush(transaction);

        log.info(
                "event=transaction.created transactionId={} userId={} type={} status={}",
                saved.getId(),
                saved.getUserId(),
                saved.getType(),
                saved.getStatus()
        );

        return transactionMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(UUID transactionId) {
        UUID userId = currentUserService.getCurrentUserId();

        Transaction transaction = transactionRepository
                .findByIdAndUserId(transactionId, userId)
                .orElseThrow(TransactionNotFoundException::new);

        return transactionMapper.toResponse(transaction);
    }

    @Transactional
    public TransactionResponse update(UUID transactionId, UpdateTransactionRequest request) {
        UUID userId = currentUserService.getCurrentUserId();

        Transaction transaction = transactionRepository
                .findByIdAndUserId(transactionId, userId)
                .orElseThrow(TransactionNotFoundException::new);

        if (transaction.getStatus() == TransactionStatus.CANCELLED) {
            throw new InvalidTransactionStatusException("Transação cancelada não pode ser editada");
        }

        if (request.status() == TransactionStatus.CANCELLED) {
            throw new InvalidTransactionStatusException("Utilize o endpoint de cancelamento");
        }

        if (isCardManagedTransaction(transaction)) {
            return updateCardManagedTransactionMetadata(transaction, request, userId);
        }

        TransactionStatus previousStatus = transaction.getStatus();

        transactionImpactService.reverse(userId, transaction);

        transactionMapper.updateEntity(request, transaction);

        if (request.status() == TransactionStatus.PENDING) {
            transaction.setEffectiveDate(null);
        }

        validateUpdatedTransaction(transaction, userId);

        transactionImpactService.apply(userId, transaction);

        Transaction saved = transactionRepository.saveAndFlush(transaction);

        log.info(
                "event=transaction.updated transactionId={} userId={} previousStatus={} currentStatus={}",
                saved.getId(),
                saved.getUserId(),
                previousStatus,
                saved.getStatus()
        );

        return transactionMapper.toResponse(saved);
    }

    private TransactionResponse updateCardManagedTransactionMetadata(
            Transaction transaction,
            UpdateTransactionRequest request,
            UUID userId
    ) {
        validateCardManagedTransactionUpdate(transaction, request);

        if (transaction.getType() == TransactionType.CREDIT_CARD_PURCHASE
                && request.categoryId() != null) {
            Category category = findOwnedCategory(request.categoryId(), userId);
            validateCategory(category, TransactionType.EXPENSE);
        }

        List<Transaction> relatedTransactions = relatedCardTransactions(transaction, userId);

        for (Transaction relatedTransaction : relatedTransactions) {
            if (request.description() != null) {
                relatedTransaction.setDescription(request.description());
            }

            if (request.categoryId() != null) {
                relatedTransaction.setCategoryId(request.categoryId());
            }
        }

        transactionRepository.saveAllAndFlush(relatedTransactions);

        log.info(
                "event=transaction.metadata_updated transactionId={} userId={} type={}",
                transaction.getId(),
                userId,
                transaction.getType()
        );

        return transactionMapper.toResponse(transaction);
    }

    private List<Transaction> relatedCardTransactions(Transaction transaction, UUID userId) {
        if (transaction.getType() == TransactionType.CREDIT_CARD_PURCHASE
                && transaction.getInstallmentGroupId() != null
                && transaction.getCreditCardId() != null) {
            return transactionRepository
                    .findAllByInstallmentGroupIdAndCreditCardIdAndUserIdOrderByInstallmentNumberAsc(
                            transaction.getInstallmentGroupId(),
                            transaction.getCreditCardId(),
                            userId
                    );
        }

        return List.of(transaction);
    }

    private void validateCardManagedTransactionUpdate(
            Transaction transaction,
            UpdateTransactionRequest request
    ) {
        if (request.amount() != null
                || request.competenceDate() != null
                || request.effectiveDate() != null
                || request.status() != null
                || request.paymentMethod() != null
                || request.sourceAccountId() != null
                || request.destinationAccountId() != null) {
            throw new InvalidTransactionException(
                    "Nesta transação, somente as informações descritivas podem ser alteradas"
            );
        }

        if (request.description() != null && request.description().isBlank()) {
            throw new InvalidTransactionException("Descrição da transação é obrigatória");
        }

        if (transaction.getType() == TransactionType.CREDIT_CARD_PAYMENT
                && request.categoryId() != null) {
            throw new InvalidTransactionException(
                    "A categoria de um pagamento de fatura não pode ser alterada"
            );
        }
    }

    private boolean isCardManagedTransaction(Transaction transaction) {
        return transaction.getType() == TransactionType.CREDIT_CARD_PURCHASE
                || transaction.getType() == TransactionType.CREDIT_CARD_PAYMENT;
    }

    @Transactional
    public TransactionResponse cancel(UUID transactionId) {
        UUID userId = currentUserService.getCurrentUserId();

        Transaction transaction = transactionRepository
                .findByIdAndUserId(transactionId, userId)
                .orElseThrow(TransactionNotFoundException::new);

        validateGenericMutation(transaction);

        if (transaction.getStatus() == TransactionStatus.CANCELLED) {
            throw new TransactionAlreadyCancelledException();
        }

        transactionImpactService.reverse(userId, transaction);
        transaction.setStatus(TransactionStatus.CANCELLED);

        Transaction saved = transactionRepository.saveAndFlush(transaction);

        log.info(
                "event=transaction.cancelled transactionId={} userId={} type={} status={}",
                saved.getId(),
                userId,
                saved.getType(),
                saved.getStatus()
        );

        return transactionMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<TransactionListItemResponse> list(TransactionFilterRequest filters, Pageable pageable) {
        UUID userId = currentUserService.getCurrentUserId();
        Specification<Transaction> specification = ownedSpecification(filters)
                .and(TransactionSpecifications.firstInstallmentOrStandalone());

        Pageable safePageable = normalizePageable(pageable);

        Page<Transaction> transactions = transactionRepository.findAll(specification, safePageable);

        Map<UUID, BigDecimal> totalsByGroupId = findInstallmentTotals(userId, transactions.getContent());

        return transactions.map(transaction -> {
            TransactionResponse response = transactionMapper.toResponse(transaction);
            BigDecimal displayAmount = transaction.getInstallmentGroupId() == null
                    ? transaction.getAmount()
                    : totalsByGroupId.getOrDefault(
                            transaction.getInstallmentGroupId(),
                            transaction.getAmount()
                    );

            return TransactionListItemResponse.from(response, displayAmount);
        });
    }

    @Transactional(readOnly = true)
    public TransactionInstallmentDetailsResponse findInstallmentDetails(UUID transactionId) {
        UUID userId = currentUserService.getCurrentUserId();

        Transaction transaction = transactionRepository
                .findByIdAndUserId(transactionId, userId)
                .orElseThrow(TransactionNotFoundException::new);

        List<Transaction> installments = transaction.getInstallmentGroupId() == null
                ? List.of(transaction)
                : transactionRepository
                        .findAllByInstallmentGroupIdAndCreditCardIdAndUserIdOrderByInstallmentNumberAsc(
                                transaction.getInstallmentGroupId(),
                                transaction.getCreditCardId(),
                                userId
                        );

        BigDecimal totalAmount = installments.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TransactionInstallmentDetailsResponse(
                totalAmount,
                installments.stream().map(transactionMapper::toResponse).toList()
        );
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> listForExport(TransactionFilterRequest filters) {
        return transactionRepository
                .findAll(ownedSpecification(filters), EXPORT_SORT)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    private Map<UUID, BigDecimal> findInstallmentTotals(
            UUID userId,
            List<Transaction> transactions
    ) {
        List<UUID> installmentGroupIds = transactions.stream()
                .map(Transaction::getInstallmentGroupId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        if (installmentGroupIds.isEmpty()) {
            return Map.of();
        }

        return transactionRepository
                .sumByInstallmentGroupIds(userId, installmentGroupIds)
                .stream()
                .collect(Collectors.toMap(
                        InstallmentGroupTotal::installmentGroupId,
                        InstallmentGroupTotal::totalAmount
                ));
    }

    private void applyBalance(CreateTransactionRequest request, UUID userId, UUID accountId) {
        if (request.type() == TransactionType.INCOME) {
            accountBalanceService.credit(userId, accountId, request.amount());
            return;
        }

        accountBalanceService.debit(userId, accountId, request.amount());
    }

    private Category findOwnedCategory(UUID categoryId, UUID userId) {
        return categoryRepository
                .findByIdAndUserId(categoryId, userId)
                .orElseThrow(CategoryNotFoundException::new);
    }

    private void validateCategory(Category category, TransactionType transactionType) {
        CategoryType expectedType =
                transactionType == TransactionType.INCOME ? CategoryType.INCOME : CategoryType.EXPENSE;

        if (category.getType() != expectedType) {
            throw new IncompatibleCategoryTypeException();
        }

        if (category.getStatus() != CategoryStatus.ACTIVE) {
            throw new InvalidTransactionException("Categoria inativa não pode receber novas transações");
        }
    }

    private void validateOwnedActiveAccount(UUID accountId, UUID userId) {
        Account account = accountRepository
                .findByIdAndUserId(accountId, userId)
                .orElseThrow(AccountNotFoundException::new);

        account.ensureActive();
    }

    private UUID resolveAccountId(CreateTransactionRequest request) {
        if (request.type() == TransactionType.INCOME) {
            return request.destinationAccountId();
        }

        return request.sourceAccountId();
    }

    private void validateRequest(CreateTransactionRequest request) {
        if (request.type() != TransactionType.INCOME && request.type() != TransactionType.EXPENSE) {
            throw new InvalidTransactionException("Apenas receitas e despensas são suportadas nesta operação");
        }

        if (request.paymentMethod() == PaymentMethod.CREDIT_CARD) {
            throw new InvalidTransactionException("Transações de cartão de crédito ainda não são suportadas");
        }

        if (request.status() == TransactionStatus.CANCELLED) {
            throw new InvalidTransactionException("Uma transação não pode ser criada como cancelada");
        }

        validateDates(request);
        validateAccounts(request);
        validateFutureFields(request);
    }

    private void validateDates(CreateTransactionRequest request) {
        if (request.status() == TransactionStatus.COMPLETED && request.effectiveDate() == null) {
            throw new InvalidTransactionException("Transação concluída deve possuir data efetiva");
        }

        if (request.status() == TransactionStatus.PENDING && request.effectiveDate() != null) {
            throw new InvalidTransactionException("Transação pendente não deve possuir data efetiva");
        }
    }

    private void validateAccounts(CreateTransactionRequest request) {
        if (request.type() == TransactionType.INCOME) {
            if (request.destinationAccountId() == null) {
                throw new InvalidTransactionException("Receita deve possuir conta de destino");
            }

            if (request.sourceAccountId() != null) {
                throw new InvalidTransactionException("Receita não deve possuir conta de origem");
            }

            return;
        }

        if (request.sourceAccountId() == null) {
            throw new InvalidTransactionException("Despesa deve possuir conta de origem");
        }

        if (request.destinationAccountId() != null) {
            throw new InvalidTransactionException("Despesa não deve possuir conta de destino");
        }
    }

    private void validateFutureFields(CreateTransactionRequest request) {
        if (request.creditCardId() != null
                || request.invoiceId() != null
                || request.installmentGroupId() != null
                || request.installmentNumber() != null
                || request.installmentCount() != null) {
            throw new InvalidTransactionException("Cartão, fatura e parcelamento ainda não suportados");
        }
    }

    private void validateUpdatedTransaction(
            Transaction transaction,
            UUID userId
    ) {
        if (transaction.getDescription() == null
                || transaction.getDescription().isBlank()) {
            throw new InvalidTransactionException("Descrição da transação é obrigatória");
        }

        if (transaction.getAmount() == null
                || transaction.getAmount().signum() <= 0) {
            throw new InvalidTransactionException("O valor da transação deve ser maior que zero");
        }

        if (transaction.getCompetenceDate() == null) {
            throw new InvalidTransactionException("Data de competência é obrigatória");
        }

        if (transaction.getType() == null
                || transaction.getStatus() == null
                || transaction.getPaymentMethod() == null) {
            throw new InvalidTransactionException("Tipo, status e método de pagamento são obrigatórios");
        }

        if (transaction.getStatus() == TransactionStatus.CANCELLED) {
            throw new InvalidTransactionException("Utilize o endpoint de cancelamento");
        }

        if (transaction.getStatus() == TransactionStatus.COMPLETED
                && transaction.getEffectiveDate() == null) {
            throw new InvalidTransactionException("Transação concluída deve possuir data efetiva");
        }

        if (transaction.getStatus() == TransactionStatus.PENDING
                && transaction.getEffectiveDate() != null) {
            throw new InvalidTransactionException("Transação pendente não deve possuir data efetiva");
        }

        switch (transaction.getType()) {
            case INCOME -> validateUpdatedIncome(transaction, userId);
            case EXPENSE -> validateUpdatedExpense(transaction, userId);
            case TRANSFER -> validateUpdatedTransfer(transaction, userId);
            default -> throw new InvalidTransactionException("Tipo de transação não suportado para edição");
        }
    }

    private void validateUpdatedIncome(
            Transaction transaction,
            UUID userId
    ) {
        if (transaction.getDestinationAccountId() == null) {
            throw new InvalidTransactionException("Receita deve possuir conta de destino");
        }

        if (transaction.getSourceAccountId() != null) {
            throw new InvalidTransactionException("Receita não deve possuir conta de origem");
        }

        if (transaction.getCategoryId() == null) {
            throw new InvalidTransactionException("Receita deve possuir categoria");
        }

        if (transaction.getPaymentMethod() == PaymentMethod.CREDIT_CARD) {
            throw new InvalidTransactionException("Transações de cartão de crédito ainda não são suportadas");
        }

        Category category = findOwnedCategory(transaction.getCategoryId(), userId);

        validateCategory(category, TransactionType.INCOME);
        validateOwnedActiveAccount(transaction.getDestinationAccountId(), userId);
    }

    private void validateUpdatedExpense(Transaction transaction, UUID userId) {
        if (transaction.getSourceAccountId() == null) {
            throw new InvalidTransactionException("Despesa deve possuir conta de origem");
        }

        if (transaction.getDestinationAccountId() != null) {
            throw new InvalidTransactionException("Despesa não deve possuir conta de destino");
        }

        if (transaction.getCategoryId() == null) {
            throw new InvalidTransactionException("Despesa deve possuir categoria");
        }

        if (transaction.getPaymentMethod() == PaymentMethod.CREDIT_CARD) {
            throw new InvalidTransactionException("Transações de cartão de crédito ainda não são suportadas");
        }

        Category category = findOwnedCategory(transaction.getCategoryId(), userId);

        validateCategory(category, TransactionType.EXPENSE);
        validateOwnedActiveAccount(transaction.getSourceAccountId(), userId);
    }

    private void validateUpdatedTransfer(Transaction transaction, UUID userId) {
        if (transaction.getSourceAccountId() == null
                || transaction.getDestinationAccountId() == null) {
            throw new InvalidTransactionException("Transferência deve possuir contas de origem e destino");
        }

        if (transaction.getSourceAccountId()
                .equals(transaction.getDestinationAccountId())) {
            throw new InvalidTransactionException("As contas de origem e destino devem ser diferentes");
        }

        if (transaction.getCategoryId() != null) {
            throw new InvalidTransactionException("Transferência não deve possuir categoria");
        }

        if (transaction.getPaymentMethod() != PaymentMethod.TRANSFER) {
            throw new InvalidTransactionException("Transferência deve utilizar o método TRANSFER");
        }

        validateOwnedActiveAccount(transaction.getSourceAccountId(), userId);

        validateOwnedActiveAccount(transaction.getDestinationAccountId(), userId);
    }

    private void validateFilters(TransactionFilterRequest filters) {
        if (filters.startDate() != null
                && filters.endDate() != null
                && filters.startDate().isAfter(filters.endDate())) {

            throw new InvalidTransactionException(
                    "A data inicial não pode ser posterior à data final"
            );
        }

        if (filters.minAmount() != null
                && filters.maxAmount() != null
                && filters.minAmount().compareTo(filters.maxAmount()) > 0) {

            throw new InvalidTransactionException(
                    "O valor mínimo não pode ser maior que o valor máximo"
            );
        }
    }

    private Pageable normalizePageable(Pageable pageable) {
        if (pageable.isUnpaged()) {
            throw new InvalidTransactionException(
                    "A consulta de transações deve ser paginada"
            );
        }

        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "competenceDate");

        for (Sort.Order order : sort) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new InvalidTransactionException(
                        "Campo de ordenação inválido: " + order.getProperty()
                );
            }

            if (order.isIgnoreCase()
                    && !"description".equals(order.getProperty())) {
                throw new InvalidTransactionException(
                        "Ordenação ignorecase é permitida somente para description"
                );
            }
        }

        if (sort.getOrderFor("id") == null) {
            sort = sort.and(Sort.by(Sort.Direction.ASC, "id"));
        }

        return PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 100),
                sort
        );
    }

    private void validateGenericMutation(Transaction transaction) {
        if (transaction.getType() == TransactionType.CREDIT_CARD_PURCHASE) {
            throw new InvalidTransactionException(
                    "Utilize o endpoint de estorno de compra no cartão"
            );
        }

        if (transaction.getType() == TransactionType.CREDIT_CARD_PAYMENT) {
            throw new InvalidTransactionException(
                    "Pagamentos de fatura não podem ser alterados "
                            + "pelo fluxo genérico de transações"
            );
        }
    }

    private Specification<Transaction> ownedSpecification(TransactionFilterRequest filters) {
        UUID userId = currentUserService.getCurrentUserId();

        validateFilters(filters);

        return TransactionSpecifications.withFilters(userId, filters);
    }

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "description",
            "amount",
            "competenceDate",
            "effectiveDate",
            "dueDate",
            "type",
            "status",
            "createdAt",
            "updatedAt"
    );
}
