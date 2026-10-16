ALTER TABLE publicaciones
    ADD COLUMN video_url VARCHAR(1000) NULL,
    ADD COLUMN video_public_id VARCHAR(255) NULL,
    ADD COLUMN video_thumbnail_url VARCHAR(1000) NULL;