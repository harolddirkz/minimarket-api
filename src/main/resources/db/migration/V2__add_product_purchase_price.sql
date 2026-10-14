ALTER TABLE products
    ADD COLUMN purchase_price NUMERIC(12, 2);

ALTER TABLE products
    ADD CONSTRAINT ck_products_purchase_price_positive
        CHECK (purchase_price IS NULL OR purchase_price > 0);
