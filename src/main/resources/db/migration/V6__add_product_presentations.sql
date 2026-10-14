ALTER TABLE products
    ADD COLUMN presentation VARCHAR(30),
    ADD COLUMN content_quantity NUMERIC(12, 3),
    ADD COLUMN content_unit VARCHAR(10);

ALTER TABLE products
    ADD CONSTRAINT ck_products_content_quantity_positive
        CHECK (content_quantity IS NULL OR content_quantity > 0),
    ADD CONSTRAINT ck_products_content_definition_complete
        CHECK ((content_quantity IS NULL) = (content_unit IS NULL)),
    ADD CONSTRAINT ck_products_presentation_not_blank
        CHECK (presentation IS NULL OR BTRIM(presentation) <> ''),
    ADD CONSTRAINT ck_products_content_unit_not_blank
        CHECK (content_unit IS NULL OR BTRIM(content_unit) <> '');

ALTER TABLE sale_items
    ADD COLUMN product_presentation VARCHAR(30),
    ADD COLUMN product_content_quantity NUMERIC(12, 3),
    ADD COLUMN product_content_unit VARCHAR(10);

ALTER TABLE sale_items
    ADD CONSTRAINT ck_sale_items_product_content_quantity_positive
        CHECK (product_content_quantity IS NULL OR product_content_quantity > 0),
    ADD CONSTRAINT ck_sale_items_product_content_definition_complete
        CHECK ((product_content_quantity IS NULL) = (product_content_unit IS NULL)),
    ADD CONSTRAINT ck_sale_items_product_presentation_not_blank
        CHECK (product_presentation IS NULL OR BTRIM(product_presentation) <> ''),
    ADD CONSTRAINT ck_sale_items_product_content_unit_not_blank
        CHECK (product_content_unit IS NULL OR BTRIM(product_content_unit) <> '');
