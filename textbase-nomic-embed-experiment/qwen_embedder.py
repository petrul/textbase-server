"""Thin client for Ollama's qwen3-embedding:4b, served on zmeu.

Per Qwen3-Embedding-4B's own model card, retrieval queries benefit from an
instruction prefix but documents get none at all ("No need to add
instruction for retrieval documents") - a third convention, distinct from
both nomic (both sides prefixed differently) and bge-m3 (neither side
prefixed): "Instruct: {task}\\nQuery:{query}" on the query side only. Since
the document side is identical in both modes, symmetric and asymmetric
share one Milvus collection here too (see milvus_common.py), same as bge-m3.

CAUTION - confirmed empirically (2026-09-03) that this model does NOT
coexist well with the qwen3-vl OCR job on the same Ollama instance: cold
loads took 80-180s+ and sometimes never completed within a 120s window,
apparently thrashing for GPU memory against the much larger already-resident
OCR model (unlike nomic and bge-m3, both of which coexist fine). Expect
slow, inconsistent latency for a full reindex - avoid running large batches
of this while OCR is actively running on zmeu, if avoidable.
"""
from __future__ import annotations

import requests

OLLAMA_URL = "http://zmeu.local:11434"
MODEL = "qwen3-embedding:4b"
DIM = 2560

DEFAULT_TASK = "Given a web search query, retrieve relevant passages that answer the query"
QUERY_PREFIX = f"Instruct: {DEFAULT_TASK}\nQuery:"
DOCUMENT_PREFIX = ""  # documents never get a prefix, per the model card


def embed_batch(texts: list[str], prefix: str = "", timeout: int = 180) -> list[list[float]]:
    if not texts:
        return []
    inputs = [prefix + t for t in texts] if prefix else texts
    resp = requests.post(
        f"{OLLAMA_URL}/api/embed",
        json={"model": MODEL, "input": inputs},
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
    return embed_batch([text], prefix=prefix, timeout=timeout)[0]
