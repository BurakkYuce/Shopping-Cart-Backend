-- V2: Add vector embedding column for visual product search (CLIP ViT-B-32, 512-dim)
ALTER TABLE products ADD COLUMN IF NOT EXISTS embedding vector(512);
