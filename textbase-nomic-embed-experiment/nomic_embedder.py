"""Thin client for Ollama's nomic-embed-text:v1.5, served on zmeu.

Confirmed empirically (2026-09-03) that this model coexists fine with the
qwen3-vl OCR job also running on that Ollama instance once warm (~0.1s/call);
a cold start (model not yet resident) can take tens of seconds, so the first
batch in any run of these scripts may be slow - that's expected, not a bug.

nomic-embed-text was trained with task-prefixed inputs for *asymmetric*
retrieval (short query vs long passage) - "search_query: " on the query side,
"search_document: " on the corpus side - as opposed to the *symmetric*
doc-vs-doc similarity used with no prefix at all. The two are different
embedding spaces: a collection built with one convention must be queried
with the matching convention, never mixed - see milvus_common.collection_name
which keeps them in separate Milvus collections for exactly this reason.
"""
from __future__ import annotations

import requests

OLLAMA_URL = "http://zmeu.local:11434"
MODEL = "nomic-embed-text:v1.5"
DIM = 768

QUERY_PREFIX = "search_query: "
DOCUMENT_PREFIX = "search_document: "


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
