package com.amorim.finance_manager.config.openapi;

public final class OpenApiExamples {

    private OpenApiExamples() {
    }

    public static final String REGISTER_REQUEST = """
            {
              "name": "Henrique Amorim",
              "email": "henrique@example.com",
              "password": "SenhaSegura123!"
            }
            """;

    public static final String USER_RESPONSE = """
            {
              "id": "2a1fbc5b-cbb9-4879-b0c5-42f034d64261",
              "name": "Henrique Amorim",
              "email": "henrique@example.com",
              "createdAt": "2026-09-02T12:00:00Z",
              "updatedAt": "2026-09-02T12:00:00Z"
            }
            """;

    public static final String UPDATED_USER_RESPONSE = """
            {
              "id": "2a1fbc5b-cbb9-4879-b0c5-42f034d64261",
              "name": "Henrique Amorim Silva",
              "email": "henrique.silva@example.com",
              "createdAt": "2026-09-02T12:00:00Z",
              "updatedAt": "2026-09-02T12:30:00Z"
            }
            """;

    public static final String LOGIN_REQUEST = """
            {
              "email": "henrique@example.com",
              "password": "SenhaSegura123!"
            }
            """;

    public static final String AUTH_RESPONSE = """
            {
              "expiresIn": 3600
            }
            """;

    public static final String CSRF_TOKEN_RESPONSE = """
            {
              "token": "550e8400-e29b-41d4-a716-446655440000",
              "headerName": "X-XSRF-TOKEN"
            }
            """;

    public static final String CREATE_ACCOUNT_REQUEST = """
            {
              "name": "Conta principal",
              "type": "CHECKING",
              "institution": "Banco Exemplo",
              "initialBalance": 1500.00
            }
            """;

    public static final String ACCOUNT_RESPONSE = """
            {
              "id": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
              "name": "Conta principal",
              "type": "CHECKING",
              "institution": "Banco Exemplo",
              "initialBalance": 1500.00,
              "currentBalance": 1500.00,
              "status": "ACTIVE",
              "version": 0,
              "createdAt": "2026-09-02T12:00:00Z",
              "updatedAt": "2026-09-02T12:00:00Z"
            }
            """;

    public static final String ACCOUNT_LIST_RESPONSE = """
            [
              {
                "id": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
                "name": "Conta principal",
                "type": "CHECKING",
                "institution": "Banco Exemplo",
                "initialBalance": 1500.00,
                "currentBalance": 1500.00,
                "status": "ACTIVE",
                "version": 0,
                "createdAt": "2026-09-02T12:00:00Z",
                "updatedAt": "2026-09-02T12:00:00Z"
              },
              {
                "id": "9ba25043-024c-4ba3-a48a-bf62a2c30ef0",
                "name": "Reserva de emergência",
                "type": "SAVINGS",
                "institution": "Banco Exemplo",
                "initialBalance": 500.00,
                "currentBalance": 750.00,
                "status": "ACTIVE",
                "version": 1,
                "createdAt": "2026-09-02T12:00:00Z",
                "updatedAt": "2026-09-02T12:30:00Z"
              }
            ]
            """;

    public static final String UPDATED_ACCOUNT_RESPONSE = """
            {
              "id": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
              "name": "Conta principal atualizada",
              "type": "CHECKING",
              "institution": "Novo Banco",
              "initialBalance": 1500.00,
              "currentBalance": 1500.00,
              "status": "ACTIVE",
              "version": 1,
              "createdAt": "2026-09-02T12:00:00Z",
              "updatedAt": "2026-09-02T12:30:00Z"
            }
            """;

    public static final String INACTIVE_ACCOUNT_RESPONSE = """
            {
              "id": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
              "name": "Conta principal",
              "type": "CHECKING",
              "institution": "Banco Exemplo",
              "initialBalance": 1500.00,
              "currentBalance": 1500.00,
              "status": "INACTIVE",
              "version": 1,
              "createdAt": "2026-09-02T12:00:00Z",
              "updatedAt": "2026-09-02T12:30:00Z"
            }
            """;

    public static final String VALIDATION_ERROR = """
            {
              "timestamp": "2026-09-02T12:00:00Z",
              "status": 400,
              "code": "VALIDATION_ERROR",
              "message": "Dados de entrada inválidos",
              "path": "/api/v1/accounts",
              "fieldErrors": [
                {
                  "field": "name",
                  "message": "Nome é obrigatório"
                }
              ]
            }
            """;

    public static final String UNAUTHORIZED_ERROR = """
            {
              "timestamp": "2026-09-02T12:00:00Z",
              "status": 401,
              "code": "UNAUTHORIZED",
              "message": "Autenticação necessária ou token inválido",
              "path": "/api/v1/accounts",
              "fieldErrors": []
            }
            """;

    public static final String ACCOUNT_NOT_FOUND = """
            {
              "timestamp": "2026-09-02T12:00:00Z",
              "status": 404,
              "code": "ACCOUNT_NOT_FOUND",
              "message": "Conta não encontrada",
              "path": "/api/v1/accounts/0f6d7313-77f8-4b48-a63d-5338dd95461e",
              "fieldErrors": []
            }
            """;

    public static final String INTERNAL_SERVER_ERROR = """
            {
              "timestamp": "2026-09-02T12:00:00Z",
              "status": 500,
              "code": "INTERNAL_SERVER_ERROR",
              "message": "Ocorreu um erro interno inesperado",
              "path": "/api/v1/accounts",
              "fieldErrors": []
            }
            """;

    public static final String UPDATE_PROFILE_REQUEST = """
            {
              "name": "Henrique Amorim Silva",
              "email": "henrique.silva@example.com",
              "currentPassword": "SenhaSegura123!"
            }
            """;

    public static final String CONFIRM_CURRENT_PASSWORD_REQUEST = """
            {
              "currentPassword": "SenhaSegura123!"
            }
            """;

    public static final String CHANGE_PASSWORD_REQUEST = """
        {
          "currentPassword": "SenhaSegura123!",
          "newPassword": "NovaSenhaSegura456!"
        }
        """;

    public static final String UPDATE_ACCOUNT_REQUEST = """
        {
          "name": "Conta principal atualizada",
          "type": "CHECKING",
          "institution": "Novo Banco"
        }
        """;

    public static final String UPDATE_ACCOUNT_STATUS_REQUEST = """
        {
          "status": "INACTIVE"
        }
        """;

    public static final String PARTIAL_ACCOUNT_UPDATE_REQUEST = """
        {
          "name": "Reserva de emergência"
        }
        """;

    public static final String CREATE_CATEGORY_REQUEST = """
        {
          "name": "Alimentação",
          "icon": "FOOD",
          "type": "EXPENSE",
          "parentCategoryId": null
        }
        """;

    public static final String CREATE_SUBCATEGORY_REQUEST = """
        {
          "name": "Supermercado",
          "icon": "SHOPPING",
          "type": "EXPENSE",
          "parentCategoryId": "c487c4cf-d948-4ba8-a85f-e36bb798c928"
        }
        """;

    public static final String UPDATE_CATEGORY_REQUEST = """
        {
          "name": "Alimentação e mercado",
          "icon": "SHOPPING",
          "status": "ACTIVE"
        }
        """;

    public static final String CATEGORY_RESPONSE = """
        {
          "id": "c487c4cf-d948-4ba8-a85f-e36bb798c928",
          "name": "Alimentação",
          "icon": "FOOD",
          "type": "EXPENSE",
          "parentCategoryId": null,
          "status": "ACTIVE",
          "createdAt": "2026-09-02T12:00:00Z",
          "updatedAt": "2026-09-02T12:00:00Z"
        }
        """;

    public static final String SUBCATEGORY_RESPONSE = """
        {
          "id": "57b1879c-a98e-4718-b66d-47f970ab6709",
          "name": "Supermercado",
          "icon": "SHOPPING",
          "type": "EXPENSE",
          "parentCategoryId": "c487c4cf-d948-4ba8-a85f-e36bb798c928",
          "status": "ACTIVE",
          "createdAt": "2026-09-02T12:00:00Z",
          "updatedAt": "2026-09-02T12:00:00Z"
        }
        """;

    public static final String UPDATED_CATEGORY_RESPONSE = """
        {
          "id": "c487c4cf-d948-4ba8-a85f-e36bb798c928",
          "name": "Alimentação e mercado",
          "icon": "SHOPPING",
          "type": "EXPENSE",
          "parentCategoryId": null,
          "status": "ACTIVE",
          "createdAt": "2026-09-02T12:00:00Z",
          "updatedAt": "2026-09-02T12:30:00Z"
        }
        """;

    public static final String CATEGORY_LIST_RESPONSE = """
        [
          {
            "id": "c487c4cf-d948-4ba8-a85f-e36bb798c928",
            "name": "Alimentação",
            "icon": "FOOD",
            "type": "EXPENSE",
            "parentCategoryId": null,
            "status": "ACTIVE",
            "createdAt": "2026-09-02T12:00:00Z",
            "updatedAt": "2026-09-02T12:00:00Z"
          },
          {
            "id": "57b1879c-a98e-4718-b66d-47f970ab6709",
            "name": "Supermercado",
            "icon": "SHOPPING",
            "type": "EXPENSE",
            "parentCategoryId": "c487c4cf-d948-4ba8-a85f-e36bb798c928",
            "status": "ACTIVE",
            "createdAt": "2026-09-02T12:00:00Z",
            "updatedAt": "2026-09-02T12:00:00Z"
          }
        ]
        """;

    public static final String CREATE_EXPENSE_TRANSACTION_REQUEST = """
        {
          "description": "Compra no supermercado",
          "amount": 180.50,
          "competenceDate": "2026-09-02",
          "effectiveDate": "2026-09-02",
          "dueDate": "2026-09-02",
          "type": "EXPENSE",
          "status": "COMPLETED",
          "paymentMethod": "PIX",
          "sourceAccountId": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
          "destinationAccountId": null,
          "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
          "creditCardId": null,
          "invoiceId": null,
          "installmentGroupId": null,
          "installmentNumber": null,
          "installmentCount": null
        }
        """;

    public static final String CREATE_INCOME_TRANSACTION_REQUEST = """
        {
          "description": "Salário mensal",
          "amount": 5000.00,
          "competenceDate": "2026-09-01",
          "effectiveDate": "2026-09-01",
          "dueDate": null,
          "type": "INCOME",
          "status": "COMPLETED",
          "paymentMethod": "PIX",
          "sourceAccountId": null,
          "destinationAccountId": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
          "categoryId": "14e7b32a-e52f-4e04-9812-4c8067129684",
          "creditCardId": null,
          "invoiceId": null,
          "installmentGroupId": null,
          "installmentNumber": null,
          "installmentCount": null
        }
        """;

    public static final String CREATE_PENDING_TRANSACTION_REQUEST = """
        {
          "description": "Conta de energia",
          "amount": 240.75,
          "competenceDate": "2026-09-02",
          "effectiveDate": null,
          "dueDate": "2026-09-10",
          "type": "EXPENSE",
          "status": "PENDING",
          "paymentMethod": "OTHER",
          "sourceAccountId": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
          "destinationAccountId": null,
          "categoryId": "ae68eab0-5738-42b3-9898-07191e036e5b",
          "creditCardId": null,
          "invoiceId": null,
          "installmentGroupId": null,
          "installmentNumber": null,
          "installmentCount": null
        }
        """;

    public static final String UPDATE_TRANSACTION_REQUEST = """
        {
          "description": "Compra mensal no supermercado",
          "amount": 210.90,
          "competenceDate": "2026-09-02",
          "effectiveDate": "2026-09-02",
          "status": "COMPLETED",
          "paymentMethod": "DEBIT",
          "sourceAccountId": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
          "destinationAccountId": null,
          "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709"
        }
        """;

    public static final String UPDATE_TRANSACTION_AMOUNT_REQUEST = """
        {
          "amount": 210.90
        }
        """;

    public static final String TRANSACTION_RESPONSE = """
        {
          "id": "2cb0ba91-bfc4-43be-89ec-336ca64a6231",
          "description": "Compra no supermercado",
          "amount": 180.50,
          "competenceDate": "2026-09-02",
          "effectiveDate": "2026-09-02",
          "dueDate": "2026-09-02",
          "type": "EXPENSE",
          "status": "COMPLETED",
          "paymentMethod": "PIX",
          "sourceAccountId": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
          "destinationAccountId": null,
          "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
          "creditCardId": null,
          "invoiceId": null,
          "installmentGroupId": null,
          "installmentNumber": null,
          "installmentCount": null,
          "createdAt": "2026-09-02T12:00:00Z",
          "updatedAt": "2026-09-02T12:00:00Z"
        }
        """;

    public static final String UPDATED_TRANSACTION_RESPONSE = """
        {
          "id": "2cb0ba91-bfc4-43be-89ec-336ca64a6231",
          "description": "Compra mensal no supermercado",
          "amount": 210.90,
          "competenceDate": "2026-09-02",
          "effectiveDate": "2026-09-02",
          "dueDate": "2026-09-02",
          "type": "EXPENSE",
          "status": "COMPLETED",
          "paymentMethod": "DEBIT",
          "sourceAccountId": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
          "destinationAccountId": null,
          "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
          "creditCardId": null,
          "invoiceId": null,
          "installmentGroupId": null,
          "installmentNumber": null,
          "installmentCount": null,
          "createdAt": "2026-09-02T12:00:00Z",
          "updatedAt": "2026-09-02T12:30:00Z"
        }
        """;

    public static final String CANCELLED_TRANSACTION_RESPONSE = """
        {
          "id": "2cb0ba91-bfc4-43be-89ec-336ca64a6231",
          "description": "Compra no supermercado",
          "amount": 180.50,
          "competenceDate": "2026-09-02",
          "effectiveDate": "2026-09-02",
          "dueDate": "2026-09-02",
          "type": "EXPENSE",
          "status": "CANCELLED",
          "paymentMethod": "PIX",
          "sourceAccountId": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
          "destinationAccountId": null,
          "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
          "creditCardId": null,
          "invoiceId": null,
          "installmentGroupId": null,
          "installmentNumber": null,
          "installmentCount": null,
          "createdAt": "2026-09-02T12:00:00Z",
          "updatedAt": "2026-09-02T12:30:00Z"
        }
        """;

    public static final String CREATE_TRANSFER_REQUEST = """
        {
          "sourceAccountId": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
          "destinationAccountId": "9ba25043-024c-4ba3-a48a-bf62a2c30ef0",
          "amount": 250.00,
          "date": "2026-09-02",
          "description": "Transferência para reserva"
        }
        """;

    public static final String TRANSFER_RESPONSE = """
        {
          "id": "3707f594-1649-4670-959c-1702e03af86d",
          "description": "Transferência para reserva",
          "amount": 250.00,
          "competenceDate": "2026-09-02",
          "effectiveDate": "2026-09-02",
          "dueDate": null,
          "type": "TRANSFER",
          "status": "COMPLETED",
          "paymentMethod": "TRANSFER",
          "sourceAccountId": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
          "destinationAccountId": "9ba25043-024c-4ba3-a48a-bf62a2c30ef0",
          "categoryId": null,
          "creditCardId": null,
          "invoiceId": null,
          "installmentGroupId": null,
          "installmentNumber": null,
          "installmentCount": null,
          "createdAt": "2026-09-02T12:00:00Z",
          "updatedAt": "2026-09-02T12:00:00Z"
        }
        """;

    public static final String CATEGORY_NOT_FOUND = """
        {
          "timestamp": "2026-09-02T12:00:00Z",
          "status": 404,
          "code": "CATEGORY_NOT_FOUND",
          "message": "Categoria não encontrada",
          "path": "/api/v1/categories/c487c4cf-d948-4ba8-a85f-e36bb798c928",
          "fieldErrors": []
        }
        """;

    public static final String TRANSACTION_NOT_FOUND = """
        {
          "timestamp": "2026-09-02T12:00:00Z",
          "status": 404,
          "code": "TRANSACTION_NOT_FOUND",
          "message": "Transação não encontrada",
          "path": "/api/v1/transactions/2cb0ba91-bfc4-43be-89ec-336ca64a6231",
          "fieldErrors": []
        }
        """;

    public static final String EMAIL_ALREADY_EXISTS = """
        {
          "timestamp": "2026-09-02T12:00:00Z",
          "status": 409,
          "code": "EMAIL_ALREADY_EXISTS",
          "message": "E-mail já cadastrado",
          "path": "/api/v1/auth/register",
          "fieldErrors": []
        }
        """;

    public static final String PROFILE_EMAIL_ALREADY_EXISTS = """
        {
          "timestamp": "2026-09-02T12:00:00Z",
          "status": 409,
          "code": "EMAIL_ALREADY_EXISTS",
          "message": "E-mail já cadastrado",
          "path": "/api/v1/users/me",
          "fieldErrors": []
        }
        """;

    public static final String INVALID_TRANSACTION_STATUS = """
        {
          "timestamp": "2026-09-02T12:00:00Z",
          "status": 409,
          "code": "INVALID_TRANSACTION_STATUS",
          "message": "Transação cancelada não pode ser editada",
          "path": "/api/v1/transactions/2cb0ba91-bfc4-43be-89ec-336ca64a6231",
          "fieldErrors": []
        }
        """;

    public static final String OPTIMISTIC_LOCK_CONFLICT = """
        {
          "timestamp": "2026-09-02T12:00:00Z",
          "status": 409,
          "code": "OPTIMISTIC_LOCK_CONFLICT",
          "message": "O recurso foi alterado por outra operação. Atualize os dados e tente novamente.",
          "path": "/api/v1/accounts/0f6d7313-77f8-4b48-a63d-5338dd95461e",
          "fieldErrors": []
        }
        """;

    public static final String TRANSACTION_ALREADY_CANCELLED = """
        {
          "timestamp": "2026-09-02T12:00:00Z",
          "status": 409,
          "code": "TRANSACTION_ALREADY_CANCELLED",
          "message": "Transação já está cancelada",
          "path": "/api/v1/transactions/2cb0ba91-bfc4-43be-89ec-336ca64a6231/cancel",
          "fieldErrors": []
        }
        """;

    public static final String INVALID_CREDENTIALS_ERROR = """
        {
          "timestamp": "2026-09-02T12:00:00Z",
          "status": 401,
          "code": "INVALID_CREDENTIALS",
          "message": "Credenciais inválidas",
          "path": "/api/v1/auth/login",
          "fieldErrors": []
        }
        """;

    public static final String TRANSACTION_PAGE_RESPONSE = """
        {
          "content": [],
          "page": 0,
          "size": 20,
          "totalElements": 0,
          "totalPages": 0,
          "first": true,
          "last": true
        }
        """;

    public static final String TRANSACTION_INSTALLMENT_DETAILS_RESPONSE = """
        {
          "totalAmount": 1200.00,
          "installments": [
            {
              "id": "2cb0ba91-bfc4-43be-89ec-336ca64a6231",
              "description": "Notebook",
              "amount": 400.00,
              "competenceDate": "2026-09-11",
              "dueDate": "2026-10-17",
              "type": "CREDIT_CARD_PURCHASE",
              "status": "COMPLETED",
              "paymentMethod": "CREDIT_CARD",
              "installmentNumber": 1,
              "installmentCount": 3
            }
          ]
        }
        """;

    public static final String DAILY_CASH_FLOW_RESPONSE = """
        {
          "date": "2026-09-03",
          "summary": {
            "inflows": 5000.00,
            "outflows": 1500.00,
            "net": 3500.00,
            "invoicePayments": 1200.00,
            "incomeCategories": [
              {
                "categoryId": "14e7b32a-e52f-4e04-9812-4c8067129684",
                "name": "Salário",
                "amount": 5000.00
              }
            ],
            "expenseCategories": [
              {
                "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
                "name": "Supermercado",
                "amount": 300.00
              }
            ]
          }
        }
        """;

    public static final String WEEKLY_CASH_FLOW_RESPONSE = """
        {
          "currentWeek": {
            "startDate": "2026-08-31",
            "endDate": "2026-09-06",
            "summary": {
              "inflows": 5000.00,
              "outflows": 2000.00,
              "net": 3000.00,
              "invoicePayments": 1200.00,
              "incomeCategories": [
                {
                  "categoryId": "14e7b32a-e52f-4e04-9812-4c8067129684",
                  "name": "Salário",
                  "amount": 5000.00
                }
              ],
              "expenseCategories": [
                {
                  "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
                  "name": "Supermercado",
                  "amount": 800.00
                }
              ]
            }
          },
          "previousWeek": {
            "startDate": "2026-08-24",
            "endDate": "2026-08-30",
            "summary": {
              "inflows": 4500.00,
              "outflows": 1800.00,
              "net": 2700.00,
              "invoicePayments": 1000.00,
              "incomeCategories": [
                {
                  "categoryId": "14e7b32a-e52f-4e04-9812-4c8067129684",
                  "name": "Salário",
                  "amount": 4500.00
                }
              ],
              "expenseCategories": [
                {
                  "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
                  "name": "Supermercado",
                  "amount": 800.00
                }
              ]
            }
          },
          "comparison": {
            "inflowsDifference": 500.00,
            "outflowsDifference": 200.00,
            "netDifference": 300.00
          }
        }
        """;

    public static final String DAILY_CASH_DATE_REQUIRED_ERROR = """
        {
          "timestamp": "2026-09-03T12:00:00Z",
          "status": 400,
          "code": "VALIDATION_ERROR",
          "message": "Dados de entrada inválidos",
          "path": "/api/v1/reports/cash/daily",
          "fieldErrors": [
            {
              "field": "date",
              "message": "A data é obrigatória"
            }
          ]
        }
        """;

    public static final String WEEKLY_CASH_DATE_REQUIRED_ERROR = """
        {
          "timestamp": "2026-09-03T12:00:00Z",
          "status": 400,
          "code": "VALIDATION_ERROR",
          "message": "Dados de entrada inválidos",
          "path": "/api/v1/reports/cash/weekly",
          "fieldErrors": [
            {
              "field": "date",
              "message": "A data é obrigatória"
            }
          ]
        }
        """;

    public static final String DAILY_CASH_INVALID_PERIOD_ERROR = """
        {
          "timestamp": "2026-09-03T12:00:00Z",
          "status": 400,
          "code": "INVALID_REPORT_PERIOD",
          "message": "O período deve estar entre 0001-01-01 e 9999-12-31",
          "path": "/api/v1/reports/cash/daily",
          "fieldErrors": []
        }
        """;

    public static final String WEEKLY_CASH_INVALID_PERIOD_ERROR = """
        {
          "timestamp": "2026-09-03T12:00:00Z",
          "status": 400,
          "code": "INVALID_REPORT_PERIOD",
          "message": "O período deve estar entre 0001-01-01 e 9999-12-31",
          "path": "/api/v1/reports/cash/weekly",
          "fieldErrors": []
        }
        """;

    public static final String DAILY_CASH_UNAUTHORIZED_ERROR = """
        {
          "timestamp": "2026-09-03T12:00:00Z",
          "status": 401,
          "code": "UNAUTHORIZED",
          "message": "Autenticação necessária ou token inválido",
          "path": "/api/v1/reports/cash/daily",
          "fieldErrors": []
        }
        """;

    public static final String WEEKLY_CASH_UNAUTHORIZED_ERROR = """
        {
          "timestamp": "2026-09-03T12:00:00Z",
          "status": 401,
          "code": "UNAUTHORIZED",
          "message": "Autenticação necessária ou token inválido",
          "path": "/api/v1/reports/cash/weekly",
          "fieldErrors": []
        }
        """;

    public static final String DAILY_CASH_INTERNAL_ERROR = """
        {
          "timestamp": "2026-09-03T12:00:00Z",
          "status": 500,
          "code": "INTERNAL_SERVER_ERROR",
          "message": "Ocorreu um erro interno inesperado",
          "path": "/api/v1/reports/cash/daily",
          "fieldErrors": []
        }
        """;

    public static final String WEEKLY_CASH_INTERNAL_ERROR = """
        {
          "timestamp": "2026-09-03T12:00:00Z",
          "status": 500,
          "code": "INTERNAL_SERVER_ERROR",
          "message": "Ocorreu um erro interno inesperado",
          "path": "/api/v1/reports/cash/weekly",
          "fieldErrors": []
        }
        """;

    public static final String MONTHLY_CASH_FLOW_RESPONSE = """
    {
      "year": 2026,
      "month": 9,
      "startDate": "2026-09-01",
      "endDate": "2026-09-30",
      "summary": {
        "inflows": 5000.00,
        "outflows": 1500.00,
        "net": 3500.00,
        "invoicePayments": 1200.00,
        "incomeCategories": [
          {
            "categoryId": "14e7b32a-e52f-4e04-9812-4c8067129684",
            "name": "Salário",
            "amount": 5000.00
          }
        ],
        "expenseCategories": [
          {
            "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
            "name": "Supermercado",
            "amount": 300.00
          }
        ]
      }
    }
    """;

    public static final String ANNUAL_CASH_FLOW_RESPONSE = """
    {
      "year": 2026,
      "startDate": "2026-01-01",
      "endDate": "2026-12-31",
      "evolution": [
        {
          "month": 1,
          "totals": {
            "inflows": 5000.00,
            "outflows": 1500.00,
            "net": 3500.00
          }
        },
        {
          "month": 2,
          "totals": {
            "inflows": 0.00,
            "outflows": 0.00,
            "net": 0.00
          }
        },
        {
          "month": 3,
          "totals": {
            "inflows": 0.00,
            "outflows": 0.00,
            "net": 0.00
          }
        },
        {
          "month": 4,
          "totals": {
            "inflows": 0.00,
            "outflows": 0.00,
            "net": 0.00
          }
        },
        {
          "month": 5,
          "totals": {
            "inflows": 0.00,
            "outflows": 0.00,
            "net": 0.00
          }
        },
        {
          "month": 6,
          "totals": {
            "inflows": 0.00,
            "outflows": 0.00,
            "net": 0.00
          }
        },
        {
          "month": 7,
          "totals": {
            "inflows": 0.00,
            "outflows": 0.00,
            "net": 0.00
          }
        },
        {
          "month": 8,
          "totals": {
            "inflows": 0.00,
            "outflows": 0.00,
            "net": 0.00
          }
        },
        {
          "month": 9,
          "totals": {
            "inflows": 5000.00,
            "outflows": 1200.00,
            "net": 3800.00
          }
        },
        {
          "month": 10,
          "totals": {
            "inflows": 0.00,
            "outflows": 0.00,
            "net": 0.00
          }
        },
        {
          "month": 11,
          "totals": {
            "inflows": 0.00,
            "outflows": 0.00,
            "net": 0.00
          }
        },
        {
          "month": 12,
          "totals": {
            "inflows": 0.00,
            "outflows": 0.00,
            "net": 0.00
          }
        }
      ]
    }
    """;

    public static final String MONTHLY_CASH_VALIDATION_ERROR = """
    {
      "timestamp": "2026-09-03T12:00:00Z",
      "status": 400,
      "code": "VALIDATION_ERROR",
      "message": "Dados de entrada inválidos",
      "path": "/api/v1/reports/cash/monthly",
      "fieldErrors": [
        {
          "field": "month",
          "message": "O mês é obrigatório"
        },
        {
          "field": "year",
          "message": "O ano é obrigatório"
        }
      ]
    }
    """;

    public static final String ANNUAL_CASH_VALIDATION_ERROR = """
    {
      "timestamp": "2026-09-03T12:00:00Z",
      "status": 400,
      "code": "VALIDATION_ERROR",
      "message": "Dados de entrada inválidos",
      "path": "/api/v1/reports/cash/annual",
      "fieldErrors": [
        {
          "field": "year",
          "message": "O ano é obrigatório"
        }
      ]
    }
    """;

    public static final String MONTHLY_CASH_UNAUTHORIZED_ERROR = """
    {
      "timestamp": "2026-09-03T12:00:00Z",
      "status": 401,
      "code": "UNAUTHORIZED",
      "message": "Autenticação necessária ou token inválido",
      "path": "/api/v1/reports/cash/monthly",
      "fieldErrors": []
    }
    """;

    public static final String ANNUAL_CASH_UNAUTHORIZED_ERROR = """
    {
      "timestamp": "2026-09-03T12:00:00Z",
      "status": 401,
      "code": "UNAUTHORIZED",
      "message": "Autenticação necessária ou token inválido",
      "path": "/api/v1/reports/cash/annual",
      "fieldErrors": []
    }
    """;

    public static final String MONTHLY_CASH_INTERNAL_ERROR = """
    {
      "timestamp": "2026-09-03T12:00:00Z",
      "status": 500,
      "code": "INTERNAL_SERVER_ERROR",
      "message": "Ocorreu um erro interno inesperado",
      "path": "/api/v1/reports/cash/monthly",
      "fieldErrors": []
    }
    """;

    public static final String ANNUAL_CASH_INTERNAL_ERROR = """
    {
      "timestamp": "2026-09-03T12:00:00Z",
      "status": 500,
      "code": "INTERNAL_SERVER_ERROR",
      "message": "Ocorreu um erro interno inesperado",
      "path": "/api/v1/reports/cash/annual",
      "fieldErrors": []
    }
    """;

    public static final String CREATE_CREDIT_CARD_REQUEST = """
        {
          "name": "Cartão principal",
          "creditLimit": 5000.00,
          "closingDay": 10,
          "dueDay": 17,
          "defaultAccountId": "0f6d7313-77f8-4b48-a63d-5338dd95461e"
        }
        """;

    public static final String UPDATE_CREDIT_CARD_REQUEST = """
        {
          "name": "Cartão viagens",
          "creditLimit": 6500.00,
          "closingDay": 12,
          "dueDay": 19,
          "status": "ACTIVE"
        }
        """;

    public static final String CREDIT_CARD_RESPONSE = """
        {
          "id": "d89835ee-3463-4a35-a2e9-38d96ab17418",
          "name": "Cartão principal",
          "creditLimit": 5000.00,
          "availableLimit": 5000.00,
          "closingDay": 10,
          "dueDay": 17,
          "defaultAccountId": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
          "status": "ACTIVE",
          "version": 0
        }
        """;

    public static final String UPDATED_CREDIT_CARD_RESPONSE = """
        {
          "id": "d89835ee-3463-4a35-a2e9-38d96ab17418",
          "name": "Cartão viagens",
          "creditLimit": 6500.00,
          "availableLimit": 6500.00,
          "closingDay": 12,
          "dueDay": 19,
          "defaultAccountId": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
          "status": "ACTIVE",
          "version": 1
        }
        """;

    public static final String CREDIT_CARD_LIST_RESPONSE = """
        [
          {
            "id": "d89835ee-3463-4a35-a2e9-38d96ab17418",
            "name": "Cartão principal",
            "creditLimit": 5000.00,
            "availableLimit": 5000.00,
            "closingDay": 10,
            "dueDay": 17,
            "defaultAccountId": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
            "status": "ACTIVE",
            "version": 0
          }
        ]
        """;

    public static final String CREDIT_CARD_NOT_FOUND = """
        {
          "timestamp": "2026-09-04T12:00:00Z",
          "status": 404,
          "code": "CREDIT_CARD_NOT_FOUND",
          "message": "Cartão de crédito não encontrado",
          "path": "/api/v1/credit-cards/d89835ee-3463-4a35-a2e9-38d96ab17418",
          "fieldErrors": []
        }
        """;

    public static final String INVALID_CREDIT_CARD_UPDATE = """
        {
          "timestamp": "2026-09-04T12:00:00Z",
          "status": 400,
          "code": "INVALID_CREDIT_CARD_UPDATE",
          "message": "Informe ao menos um campo para atualização",
          "path": "/api/v1/credit-cards/d89835ee-3463-4a35-a2e9-38d96ab17418",
          "fieldErrors": []
        }
        """;

    public static final String CREDIT_LIMIT_CONFLICT = """
        {
          "timestamp": "2026-09-04T12:00:00Z",
          "status": 409,
          "code": "CREDIT_LIMIT_CONFLICT",
          "message": "O novo limite não pode ser menor que o limite já comprometido",
          "path": "/api/v1/credit-cards/d89835ee-3463-4a35-a2e9-38d96ab17418",
          "fieldErrors": []
        }
        """;

    public static final String INVOICE_PAGE_RESPONSE = """
        {
          "content": [
            {
              "id": "72486234-ef50-4c7e-99a7-9193a28533a8",
              "creditCardId": "0c736743-8885-43d1-813b-c096a4899201",
              "referenceMonth": 9,
              "referenceYear": 2026,
              "closingDate": "2026-09-20",
              "dueDate": "2026-09-28",
              "totalAmount": 850.75,
              "status": "OPEN",
              "paidAt": null,
              "version": 0
            }
          ],
          "page": 0,
          "size": 20,
          "totalElements": 1,
          "totalPages": 1,
          "first": true,
          "last": true
        }
        """;

    public static final String INVOICE_DETAIL_RESPONSE = """
        {
          "id": "72486234-ef50-4c7e-99a7-9193a28533a8",
          "creditCardId": "0c736743-8885-43d1-813b-c096a4899201",
          "referenceMonth": 9,
          "referenceYear": 2026,
          "closingDate": "2026-09-20",
          "dueDate": "2026-09-28",
          "totalAmount": 850.75,
          "status": "OPEN",
          "paidAt": null,
          "version": 0,
          "transactions": [
            {
              "id": "75d4b4e7-8621-41fe-b71f-34c47e1b581b",
              "description": "Compra no supermercado",
              "amount": 350.75,
              "competenceDate": "2026-09-04",
              "effectiveDate": null,
              "dueDate": "2026-09-28",
              "type": "EXPENSE",
              "status": "COMPLETED",
              "paymentMethod": "CREDIT_CARD",
              "categoryId": "6d342909-a042-4dde-b57f-b4f35696f5db",
              "creditCardId": "0c736743-8885-43d1-813b-c096a4899201",
              "invoiceId": "72486234-ef50-4c7e-99a7-9193a28533a8",
              "installmentNumber": 1,
              "installmentCount": 1
            }
          ]
        }
        """;

    public static final String INVOICE_NOT_FOUND = """
        {
          "timestamp": "2026-09-04T14:30:00Z",
          "status": 404,
          "code": "INVOICE_NOT_FOUND",
          "message": "Fatura não encontrada",
          "path": "/api/v1/invoices/72486234-ef50-4c7e-99a7-9193a28533a8",
          "fieldErrors": []
        }
        """;

    public static final String INVALID_REQUEST_ERROR = """
        {
          "timestamp": "2026-09-04T14:30:00Z",
          "status": 400,
          "code": "INVALID_REQUEST",
          "message": "Parâmetro de requisição inválido",
          "path": "/api/v1/invoices",
          "fieldErrors": []
        }
        """;

    public static final String CREATE_CREDIT_CARD_PURCHASE_REQUEST = """
    {
      "description": "Compra no supermercado",
      "amount": 100.00,
      "purchaseDate": "2026-09-11",
      "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
      "installmentCount": 3
    }
    """;

    public static final String CREDIT_CARD_PURCHASES_RESPONSE = """
    [
      {
        "id": "2cb0ba91-bfc4-43be-89ec-336ca64a6231",
        "description": "Compra no supermercado",
        "amount": 33.33,
        "competenceDate": "2026-09-11",
        "effectiveDate": null,
        "dueDate": "2026-10-17",
        "type": "CREDIT_CARD_PURCHASE",
        "status": "COMPLETED",
        "paymentMethod": "CREDIT_CARD",
        "sourceAccountId": null,
        "destinationAccountId": null,
        "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
        "creditCardId": "d89835ee-3463-4a35-a2e9-38d96ab17418",
        "invoiceId": "72486234-ef50-4c7e-99a7-9193a28533a8",
        "installmentGroupId": "3fdf8938-d2d2-4c20-8dce-1566b9c5194b",
        "installmentNumber": 1,
        "installmentCount": 3,
        "createdAt": "2026-09-11T12:00:00Z",
        "updatedAt": "2026-09-11T12:00:00Z"
      },
      {
        "id": "8e51f153-e149-4b37-873b-d851f10781be",
        "description": "Compra no supermercado",
        "amount": 33.33,
        "competenceDate": "2026-10-11",
        "effectiveDate": null,
        "dueDate": "2026-11-17",
        "type": "CREDIT_CARD_PURCHASE",
        "status": "COMPLETED",
        "paymentMethod": "CREDIT_CARD",
        "sourceAccountId": null,
        "destinationAccountId": null,
        "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
        "creditCardId": "d89835ee-3463-4a35-a2e9-38d96ab17418",
        "invoiceId": "923c54f5-07cb-48a5-8945-02d8d0bcb40c",
        "installmentGroupId": "3fdf8938-d2d2-4c20-8dce-1566b9c5194b",
        "installmentNumber": 2,
        "installmentCount": 3,
        "createdAt": "2026-09-11T12:00:00Z",
        "updatedAt": "2026-09-11T12:00:00Z"
      },
      {
        "id": "f5b05404-22f1-4d0d-bf0a-3ef260267c94",
        "description": "Compra no supermercado",
        "amount": 33.34,
        "competenceDate": "2026-11-11",
        "effectiveDate": null,
        "dueDate": "2026-12-17",
        "type": "CREDIT_CARD_PURCHASE",
        "status": "COMPLETED",
        "paymentMethod": "CREDIT_CARD",
        "sourceAccountId": null,
        "destinationAccountId": null,
        "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
        "creditCardId": "d89835ee-3463-4a35-a2e9-38d96ab17418",
        "invoiceId": "b425d89e-2cb7-4a3a-a956-33bb69d54a39",
        "installmentGroupId": "3fdf8938-d2d2-4c20-8dce-1566b9c5194b",
        "installmentNumber": 3,
        "installmentCount": 3,
        "createdAt": "2026-09-11T12:00:00Z",
        "updatedAt": "2026-09-11T12:00:00Z"
      }
    ]
    """;

    public static final String CREDIT_CARD_PURCHASE_LIMIT_CONFLICT = """
    {
      "timestamp": "2026-09-11T12:00:00Z",
      "status": 409,
      "code": "CREDIT_LIMIT_CONFLICT",
      "message": "Limite disponível insuficiente para realizar a compra",
      "path": "/api/v1/credit-cards/d89835ee-3463-4a35-a2e9-38d96ab17418/purchases",
      "fieldErrors": []
    }
    """;

    public static final String INVALID_CREDIT_CARD_STATUS_ERROR = """
    {
      "timestamp": "2026-09-11T12:00:00Z",
      "status": 409,
      "code": "INVALID_CREDIT_CARD_STATUS",
      "message": "Apenas cartões ativos podem receber novas compras",
      "path": "/api/v1/credit-cards/d89835ee-3463-4a35-a2e9-38d96ab17418/purchases",
      "fieldErrors": []
    }
    """;

    public static final String INVALID_INVOICE_STATUS_ERROR = """
    {
      "timestamp": "2026-09-11T12:00:00Z",
      "status": 409,
      "code": "INVALID_INVOICE_STATUS",
      "message": "Apenas faturas abertas podem receber novas compras",
      "path": "/api/v1/credit-cards/d89835ee-3463-4a35-a2e9-38d96ab17418/purchases",
      "fieldErrors": []
    }
    """;

    public static final String CREATE_BUDGET_REQUEST = """
        {
          "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
          "month": 9,
          "year": 2026,
          "amountLimit": 1500.00
        }
        """;

    public static final String UPDATE_BUDGET_REQUEST = """
        {
          "amountLimit": 1800.00
        }
        """;

    public static final String BUDGET_RESPONSE = """
        {
          "id": "c487c4cf-d948-4ba8-a85f-e36bb798c928",
          "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
          "month": 9,
          "year": 2026,
          "amountLimit": 1500.00,
          "spentAmount": 1200.00,
          "usagePercentage": 80.00,
          "alertStatus": "ALERT",
          "createdAt": "2026-09-08T14:00:00Z",
          "updatedAt": "2026-09-08T14:00:00Z"
        }
        """;

    public static final String BUDGET_LIST_RESPONSE = """
        [
          {
            "id": "c487c4cf-d948-4ba8-a85f-e36bb798c928",
            "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
            "month": 9,
            "year": 2026,
            "amountLimit": 1500.00,
            "spentAmount": 1200.00,
            "usagePercentage": 80.00,
            "alertStatus": "ALERT",
            "createdAt": "2026-09-08T14:00:00Z",
            "updatedAt": "2026-09-08T14:00:00Z"
          },
          {
            "id": "853430b9-48c9-4ee6-9b83-b41188405eeb",
            "categoryId": "95d2e27e-7533-42cb-85ab-4ad45ee568fc",
            "month": 8,
            "year": 2026,
            "amountLimit": 1500.00,
            "spentAmount": 1200.00,
            "usagePercentage": 80.00,
            "alertStatus": "ALERT",
            "createdAt": "2026-08-01T12:00:00Z",
            "updatedAt": "2026-08-01T12:00:00Z"
          }
        ]
        """;

    public static final String COMPETENCE_REPORT_RESPONSE = """
        {
          "startDate": "2026-09-01",
          "endDate": "2026-09-30",
          "totalIncome": 0.00,
          "totalExpenses": 1000.00,
          "result": -1000.00,
          "incomeCategories": [],
          "expenseCategories": [
            {
              "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
              "name": "Compras",
              "amount": 1000.00
            }
          ]
        }
        """;

    public static final String COMPETENCE_REPORT_VALIDATION_ERROR = """
        {
          "timestamp": "2026-09-09T14:00:00Z",
          "status": 400,
          "code": "VALIDATION_ERROR",
          "message": "Erro de validação",
          "path": "/api/v1/reports/competence",
          "fieldErrors": [
            {
              "field": "startDate",
              "message": "A data inicial é obrigatória"
            }
          ]
        }
        """;

    public static final String COMPETENCE_REPORT_INVALID_PERIOD_ERROR = """
        {
          "timestamp": "2026-09-09T14:00:00Z",
          "status": 400,
          "code": "INVALID_REPORT_PERIOD",
          "message": "A data inicial não pode ser posterior à data final",
          "path": "/api/v1/reports/competence",
          "fieldErrors": []
        }
        """;

    public static final String COMPETENCE_REPORT_UNAUTHORIZED_ERROR = """
        {
          "timestamp": "2026-09-09T14:00:00Z",
          "status": 401,
          "code": "UNAUTHORIZED",
          "message": "Autenticação necessária",
          "path": "/api/v1/reports/competence",
          "fieldErrors": []
        }
        """;

    public static final String COMPETENCE_REPORT_INTERNAL_ERROR = """
        {
          "timestamp": "2026-09-09T14:00:00Z",
          "status": 500,
          "code": "INTERNAL_SERVER_ERROR",
          "message": "Erro interno inesperado",
          "path": "/api/v1/reports/competence",
          "fieldErrors": []
        }
        """;

    public static final String DASHBOARD_RESPONSE = """
        {
          "referenceDate": "2026-09-10",
          "year": 2026,
          "month": 9,
          "periodStart": "2026-09-01",
          "periodEnd": "2026-09-30",
          "monthlyBalance": {
            "basis": "CASH_AND_INVOICE",
            "amount": 1200.00
          },
          "monthlyInflows": {
            "basis": "CASH",
            "amount": 3000.00
          },
          "totalOutflows": {
            "basis": "CASH",
            "amount": 5200.00
          },
          "monthlyOutflows": {
            "basis": "CASH",
            "amount": 1200.00
          },
          "creditCardPurchaseOutflows": {
            "basis": "COMPETENCE",
            "amount": 600.00
          },
          "competenceExpenses": {
            "basis": "COMPETENCE",
            "amount": 1800.00
          },
          "openInvoices": {
            "basis": "COMPETENCE",
            "amount": 400.00
          },
          "monthlyOpenInvoices": {
            "basis": "COMPETENCE",
            "amount": 300.00
          },
          "budget": {
            "basis": "COMPETENCE",
            "totalLimit": 1500.00,
            "totalSpent": 1200.00,
            "usagePercentage": 80.00,
            "items": [
              {
                "budgetId": "c487c4cf-d948-4ba8-a85f-e36bb798c928",
                "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
                "amountLimit": 1500.00,
                "spentAmount": 1200.00,
                "usagePercentage": 80.00,
                "alertStatus": "ALERT"
              }
            ]
          },
          "consolidatedBalance": {
            "basis": "CASH",
            "amount": 5000.00
          }
        }
        """;

    public static final String DASHBOARD_UNAUTHORIZED_ERROR = """
        {
          "timestamp": "2026-09-10T12:00:00Z",
          "status": 401,
          "code": "UNAUTHORIZED",
          "message": "Autenticação necessária",
          "path": "/api/v1/dashboard",
          "fieldErrors": []
        }
        """;

    public static final String DASHBOARD_INTERNAL_ERROR = """
        {
          "timestamp": "2026-09-10T12:00:00Z",
          "status": 500,
          "code": "INTERNAL_SERVER_ERROR",
          "message": "Erro interno inesperado",
          "path": "/api/v1/dashboard",
          "fieldErrors": []
        }
        """;

    public static final String TRANSACTION_EXPORT_CSV = """
        id,description,type,status,amount,competenceDate,effectiveDate,category,account
        2cb0ba91-bfc4-43be-89ec-336ca64a6231,Compra no supermercado,EXPENSE,COMPLETED,180.50,2026-09-02,2026-09-02,Supermercado,Conta principal
        """;
}
