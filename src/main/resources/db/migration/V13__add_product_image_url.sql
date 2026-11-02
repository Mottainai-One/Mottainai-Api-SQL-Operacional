ALTER TABLE mottainai.product
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(2048);

COMMENT ON COLUMN mottainai.product.image_url IS
    'HTTPS image URL uploaded by the mobile client to Cloudinary';
