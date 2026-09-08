"""Thin client for Ollama's bge-m3, served on zmeu.

Unlike nomic-embed-text, BGE-M3's own model card FAQ says it "no longer
requires adding instructions to the queries" - no query/document prefix on
either side. The same dense embedding serves both symmetric (doc-vs-doc) and
asymmetric (query-vs-passage IR) search, so there's no separate "mode" for
this encoder - see milvus_common.py, which points both modes at one shared
collection for bge-m3. The `prefix` param is accepted for interface
compatibility with nomic_embedder but is always ignored.

Confirmed empirically (2026-09-03): coexists fine loaded alongside qwen3-vl
(OCR) and nomic-embed-text simultaneously - reported VRAM footprint ~74MB
despite a ~1.2GB on-disk size.
"""
from __future__ import annotations

import requests

OLLAMA_URL = "http://zmeu.local:11434"
MODEL = "bge-m3"
DIM = 1024

QUERY_PREFIX = ""
DOCUMENT_PREFIX = ""


def embed_batch(texts: list[str], prefix: str = "", timeout: int = 180) -> list[list[float]]:
    if not texts:
        return []
    resp = requests.post(
        f"{OLLAMA_URL}/api/embed",
        json={"model": MODEL, "input": texts},
        timeout=timeout,
    )
    resp.raise_for_status()
    data = resp.json()
    embeddings = data.get("embeddings")
    if not embeddings or len(embeddings) != len(texts):
        got = len(embeddings) if embeddings else 0
        raise RuntimeError(f"Ollama returned {got} embeddings for {len(texts)} inputs")
    return embeddings


def embed_one(text: str, prefix: str = "", timeout: int = 180) -> list[float]:
    return embed_batch([text], timeout=timeout)[0]
