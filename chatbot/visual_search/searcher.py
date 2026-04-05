"""
Visual similarity search using CLIP embeddings stored in pgvector.
"""
import io
import os

import requests
from PIL import Image
from sentence_transformers import SentenceTransformer
from sqlalchemy import text

from db.engine import get_engine

_model: SentenceTransformer | None = None
IMG_TIMEOUT = 10


def _get_model() -> SentenceTransformer:
    global _model
    if _model is None:
        _model = SentenceTransformer("clip-ViT-B-32")
    return _model


def embed_image_bytes(image_bytes: bytes) -> list[float]:
    img = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    return _get_model().encode(img).tolist()


def embed_text(text_query: str) -> list[float]:
    """CLIP also supports text queries for cross-modal search."""
    return _get_model().encode(text_query).tolist()


def search_by_embedding(embedding: list[float], top_k: int = 8) -> list[dict]:
    """Return top_k most similar products by cosine similarity."""
    engine = get_engine()
    vec_str = "[" + ",".join(str(x) for x in embedding) + "]"

    with engine.connect() as conn:
        rows = conn.execute(text("""
            SELECT
                id, name, brand, unit_price, retail_price,
                image_url, rating, sku,
                1 - (embedding <=> CAST(:vec AS vector)) AS similarity
            FROM products
            WHERE embedding IS NOT NULL
            ORDER BY embedding <=> CAST(:vec AS vector)
            LIMIT :k
        """), {"vec": vec_str, "k": top_k}).fetchall()

    return [
        {
            "id": r[0],
            "name": r[1],
            "brand": r[2],
            "price": float(r[3]) if r[3] else None,
            "retail_price": float(r[4]) if r[4] else None,
            "image_url": r[5],
            "rating": float(r[6]) if r[6] else None,
            "asin": r[7],
            "similarity": round(float(r[8]), 3),
        }
        for r in rows
    ]


def blend_embeddings(
    img_emb: list[float], txt_emb: list[float], image_weight: float = 0.7
) -> list[float]:
    """Weighted average of image and text CLIP embeddings (both are unit-norm)."""
    w_img = image_weight
    w_txt = 1.0 - image_weight
    blended = [w_img * a + w_txt * b for a, b in zip(img_emb, txt_emb)]
    # Re-normalise so cosine distance still makes sense
    norm = sum(x * x for x in blended) ** 0.5
    if norm > 0:
        blended = [x / norm for x in blended]
    return blended


def search_by_image_bytes(image_bytes: bytes, top_k: int = 8) -> list[dict]:
    embedding = embed_image_bytes(image_bytes)
    return search_by_embedding(embedding, top_k)


def search_by_image_and_text(
    image_bytes: bytes, text_query: str, top_k: int = 8, image_weight: float = 0.7
) -> list[dict]:
    """Search using a blended image+text CLIP embedding.

    image_weight=0.7 means the visual appearance dominates (70 %) while the
    text hint steers the results (30 %).  Works great for queries like
    'find shoes similar to this but in red'.
    """
    img_emb = embed_image_bytes(image_bytes)
    txt_emb = embed_text(text_query)
    combined = blend_embeddings(img_emb, txt_emb, image_weight)
    return search_by_embedding(combined, top_k)


def search_by_text(query: str, top_k: int = 8) -> list[dict]:
    embedding = embed_text(query)
    return search_by_embedding(embedding, top_k)
