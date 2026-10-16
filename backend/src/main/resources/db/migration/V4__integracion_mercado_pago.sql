ALTER TABLE pagos
    ADD COLUMN checkout_url VARCHAR(1000) NULL AFTER mp_preference_id;

CREATE UNIQUE INDEX uk_pagos_mp_payment_id ON pagos (mp_payment_id);
