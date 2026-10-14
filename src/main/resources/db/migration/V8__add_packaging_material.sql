ALTER TABLE products
    ADD COLUMN packaging_material VARCHAR(30);

ALTER TABLE products
    ADD CONSTRAINT ck_products_packaging_material_not_blank
        CHECK (packaging_material IS NULL OR BTRIM(packaging_material) <> '');

ALTER TABLE sale_items
    ADD COLUMN product_packaging_material VARCHAR(30);
