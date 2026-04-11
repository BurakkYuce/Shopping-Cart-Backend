"""
Generates CLIP embeddings for products already in the database.
Reads products from PostgreSQL, downloads their images, encodes with CLIP,
and updates the embedding column.

Run: python visual_search/load_amazon_products.py
"""
import io
import os
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed

import requests
from PIL import Image
from sentence_transformers import SentenceTransformer
from sqlalchemy import text

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from db.engine import get_engine

BATCH_SIZE = 50
MAX_WORKERS = 8
IMG_TIMEOUT = 8

print("Loading CLIP model (clip-ViT-B-32)...")
model = SentenceTransformer("clip-ViT-B-32")
print("Model loaded.")


def fetch_and_embed(image_url: str):
    """Download image and return CLIP embedding, or None on failure."""
    try:
        resp = requests.get(image_url, timeout=IMG_TIMEOUT, stream=True)
        resp.raise_for_status()
        img = Image.open(io.BytesIO(resp.content)).convert("RGB")
        return model.encode(img).tolist()
    except Exception:
        return None


def text_embed(name: str, description: str):
    """Fallback: generate CLIP text embedding from product name + description."""
    try:
        text_input = f"{name} {description or ''}"[:200]
        return model.encode(text_input).tolist()
    except Exception:
        return None


def load_products():
    engine = get_engine()

    # Fetch products that have an image_url but no embedding yet
    with engine.connect() as conn:
        rows = conn.execute(text("""
            SELECT id, sku, name, description, image_url
            FROM products
            WHERE image_url IS NOT NULL
              AND image_url != ''
              AND (embedding IS NULL)
            ORDER BY id
        """)).fetchall()

    print(f"Found {len(rows)} products needing embeddings.")

    if not rows:
        print("Nothing to process.")
        return

    # Fetch image embeddings in parallel
    print("Downloading images and generating embeddings...")
    embeddings = {}
    with ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
        futures = {executor.submit(fetch_and_embed, r[4]): r[0] for r in rows}  # r[4]=image_url, r[0]=id
        done = 0
        for future in as_completed(futures):
            pid = futures[future]
            embeddings[pid] = future.result()
            done += 1
            if done % 100 == 0:
                print(f"  {done}/{len(rows)} images processed")

    # For failed image downloads, try text-based embedding
    text_fallback = 0
    for r in rows:
        pid = r[0]
        if embeddings.get(pid) is None:
            emb = text_embed(r[2], r[3])  # r[2]=name, r[3]=description
            if emb is not None:
                embeddings[pid] = emb
                text_fallback += 1

    print(f"Text fallback embeddings: {text_fallback}")

    # Update embeddings in DB
    updated = skipped = 0
    with engine.begin() as conn:
        for i in range(0, len(rows), BATCH_SIZE):
            batch = rows[i:i + BATCH_SIZE]
            for r in batch:
                pid = r[0]
                emb = embeddings.get(pid)
                if emb is None:
                    skipped += 1
                    continue

                conn.execute(text("""
                    UPDATE products SET embedding = :embedding WHERE id = :id
                """), {"id": pid, "embedding": str(emb)})
                updated += 1

            if (i // BATCH_SIZE + 1) % 10 == 0:
                print(f"  Batch {i // BATCH_SIZE + 1}: updated {updated}, skipped {skipped}")

    # Create/ensure vector index
    with engine.begin() as conn:
        conn.execute(text("SET ivfflat.probes = 10"))
        try:
            conn.execute(text("""
                CREATE INDEX IF NOT EXISTS products_embedding_idx
                ON products USING ivfflat (embedding vector_cosine_ops)
                WITH (lists = 20)
            """))
            print("Vector index created/verified.")
        except Exception as e:
            print(f"Index creation skipped (may already exist): {e}")

    print(f"\nDone! Updated: {updated}, Skipped (no embedding): {skipped}")


if __name__ == "__main__":
    load_products()
