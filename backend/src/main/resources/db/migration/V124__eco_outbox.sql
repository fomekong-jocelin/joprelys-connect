-- ECO-20261107 S4 : outbox transactionnelle des webhooks de l'écosystème (enveloppe § 2.2).
-- Une ligne par événement et par abonné ; écrite dans la même transaction que le changement métier.
-- last_error ne contient qu'un statut technique, jamais le contenu de l'événement.

CREATE TABLE eco_outbox (
    id               UUID PRIMARY KEY,
    event_id         UUID         NOT NULL,
    event_type       VARCHAR(80)  NOT NULL,
    subscriber       VARCHAR(50)  NOT NULL,
    payload          TEXT         NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    attempts         INTEGER      NOT NULL,
    next_attempt_at  TIMESTAMP    NOT NULL,
    created_at       TIMESTAMP    NOT NULL,
    delivered_at     TIMESTAMP,
    last_error       VARCHAR(200),
    CONSTRAINT ck_eco_outbox_status CHECK (status IN ('PENDING', 'DELIVERED', 'DEAD')),
    CONSTRAINT ck_eco_outbox_attempts CHECK (attempts >= 0),
    CONSTRAINT uq_eco_outbox_event_subscriber UNIQUE (event_id, subscriber)
);

CREATE INDEX idx_eco_outbox_due ON eco_outbox (status, next_attempt_at);
