CREATE TABLE bills (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    description VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    due_date DATE NOT NULL,
    series_id UUID NOT NULL,
    installment_number INTEGER NOT NULL,
    installment_count INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_transaction_id UUID UNIQUE REFERENCES transactions(id) ON DELETE RESTRICT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_bills_installments CHECK (
        installment_count BETWEEN 1 AND 600
        AND installment_number BETWEEN 1 AND installment_count
    ),
    CONSTRAINT ck_bills_status CHECK (status IN ('PENDING', 'PAID', 'CANCELLED')),
    CONSTRAINT ck_bills_payment CHECK (
        (status = 'PAID' AND payment_transaction_id IS NOT NULL)
        OR (status <> 'PAID' AND payment_transaction_id IS NULL)
    ),
    CONSTRAINT uk_bills_series_installment UNIQUE (user_id, series_id, installment_number)
);

CREATE INDEX idx_bills_user_due_date ON bills(user_id, due_date, id);
ALTER TABLE bills ENABLE ROW LEVEL SECURITY;

CREATE TABLE bills_aud (
    rev INTEGER NOT NULL REFERENCES audit_revision(rev),
    revtype SMALLINT,
    id UUID NOT NULL,
    user_id UUID,
    category_id UUID,
    description VARCHAR(255),
    amount NUMERIC(19, 2),
    due_date DATE,
    series_id UUID,
    installment_number INTEGER,
    installment_count INTEGER,
    status VARCHAR(20),
    payment_transaction_id UUID,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (rev, id)
);
ALTER TABLE bills_aud ENABLE ROW LEVEL SECURITY;

ALTER TABLE transactions DROP CONSTRAINT ck_transactions_payment_method;
ALTER TABLE transactions ADD CONSTRAINT ck_transactions_payment_method CHECK (
    payment_method IS NULL OR payment_method IN (
        'DEBIT', 'PIX', 'CASH', 'TRANSFER', 'CREDIT_CARD', 'OTHER', 'BOLETO'
    )
);
