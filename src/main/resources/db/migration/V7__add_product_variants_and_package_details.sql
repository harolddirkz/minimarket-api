ALTER TABLE products
    ADD COLUMN variant VARCHAR(60),
    ADD COLUMN units_per_package INTEGER,
    ADD COLUMN unit_content_quantity NUMERIC(12, 3),
    ADD COLUMN unit_content_unit VARCHAR(10);

ALTER TABLE products
    ADD CONSTRAINT ck_products_variant_not_blank CHECK (variant IS NULL OR BTRIM(variant) <> ''),
    ADD CONSTRAINT ck_products_units_per_package_positive CHECK (units_per_package IS NULL OR units_per_package > 0),
    ADD CONSTRAINT ck_products_unit_content_positive CHECK (unit_content_quantity IS NULL OR unit_content_quantity > 0),
    ADD CONSTRAINT ck_products_unit_content_pair CHECK ((unit_content_quantity IS NULL) = (unit_content_unit IS NULL)),
    ADD CONSTRAINT ck_products_unit_content_unit_not_blank CHECK (unit_content_unit IS NULL OR BTRIM(unit_content_unit) <> '');

ALTER TABLE sale_items
    ADD COLUMN product_variant VARCHAR(60),
    ADD COLUMN product_units_per_package INTEGER,
    ADD COLUMN product_unit_content_quantity NUMERIC(12, 3),
    ADD COLUMN product_unit_content_unit VARCHAR(10);

ALTER TABLE sale_items
    ADD CONSTRAINT ck_sale_items_units_per_package_positive CHECK (product_units_per_package IS NULL OR product_units_per_package > 0),
    ADD CONSTRAINT ck_sale_items_unit_content_positive CHECK (product_unit_content_quantity IS NULL OR product_unit_content_quantity > 0),
    ADD CONSTRAINT ck_sale_items_unit_content_pair CHECK ((product_unit_content_quantity IS NULL) = (product_unit_content_unit IS NULL));
