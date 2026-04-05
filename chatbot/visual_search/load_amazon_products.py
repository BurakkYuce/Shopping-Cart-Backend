"""
Loads Amazon product CSV into the products table with CLIP embeddings.
Run once: python visual_search/load_amazon_products.py
"""
import csv
import io
import json
import os
import sys
import uuid
from concurrent.futures import ThreadPoolExecutor, as_completed

import requests
from PIL import Image
from sentence_transformers import SentenceTransformer
from sqlalchemy import text

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from db.engine import get_engine

csv.field_size_limit(10**7)

CSV_PATH = os.environ.get("CSV_PATH", "/Users/burak/Downloads/amazon-products.csv")
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


def parse_price(val: str) -> float | None:
    if not val or not val.strip():
        return None
    val = val.strip().strip('"').replace(",", "")
    try:
        return float(val)
    except ValueError:
        return None


def parse_category(categories_json: str) -> str | None:
    try:
        cats = json.loads(categories_json)
        return cats[0] if cats else None
    except Exception:
        return None


def load_products():
    with open(CSV_PATH, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        rows = [r for r in reader if r.get("image_url", "").strip()]

    print(f"Found {len(rows)} products with images.")

    engine = get_engine()

    # Check already loaded (by asin stored in sku)
    with engine.connect() as conn:
        existing = {r[0] for r in conn.execute(text("SELECT sku FROM products WHERE sku IS NOT NULL")).fetchall()}
    print(f"Already in DB: {len(existing)}")

    to_load = [r for r in rows if r["asin"] not in existing]
    print(f"To load: {len(to_load)}")

    if not to_load:
        print("Nothing to load.")
        return

    # Fetch embeddings in parallel
    print("Downloading images and generating embeddings...")
    embeddings = {}
    with ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
        futures = {executor.submit(fetch_and_embed, r["image_url"]): r["asin"] for r in to_load}
        done = 0
        for future in as_completed(futures):
            asin = futures[future]
            embeddings[asin] = future.result()
            done += 1
            if done % 50 == 0:
                print(f"  {done}/{len(to_load)} images processed")

    # Insert into DB in batches
    inserted = skipped = 0
    with engine.begin() as conn:
        for i in range(0, len(to_load), BATCH_SIZE):
            batch = to_load[i:i + BATCH_SIZE]
            for r in batch:
                asin = r["asin"]
                emb = embeddings.get(asin)
                if emb is None:
                    skipped += 1
                    continue

                conn.execute(text("""
                    INSERT INTO products
                        (id, sku, name, description, unit_price, retail_price,
                         image_url, brand, rating, store_id, category_id, embedding)
                    VALUES
                        (:id, :sku, :name, :description, :unit_price, :retail_price,
                         :image_url, :brand, :rating, :store_id, :category_id, :embedding)
                    ON CONFLICT (id) DO NOTHING
                """), {
                    "id": str(uuid.uuid4()),
                    "sku": asin,
                    "name": r["title"][:255],
                    "description": (r.get("description") or "")[:2000],
                    "unit_price": parse_price(r["final_price"]) or 0,
                    "retail_price": parse_price(r.get("initial_price")),
                    "image_url": r["image_url"],
                    "brand": (r.get("brand") or "")[:200],
                    "rating": parse_price(r.get("rating")),
                    "store_id": "amazon-marketplace",
                    "category_id": None,
                    "embedding": str(emb),
                })
                inserted += 1

            print(f"Batch {i // BATCH_SIZE + 1}: inserted {inserted}, skipped {skipped}")

    # Create index after bulk insert
    with engine.begin() as conn:
        conn.execute(text("SET ivfflat.probes = 10"))
        try:
            conn.execute(text("""
                CREATE INDEX IF NOT EXISTS products_embedding_idx
                ON products USING ivfflat (embedding vector_cosine_ops)
                WITH (lists = 20)
            """))
            print("Vector index created.")
        except Exception as e:
            print(f"Index creation skipped (may already exist): {e}")

    print(f"\nDone! Inserted: {inserted}, Skipped (no image): {skipped}")


if __name__ == "__main__":
    load_products()
