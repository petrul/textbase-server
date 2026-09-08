"""Registry of available embedding backends for this experiment.

Every backend module exposes the same embed_batch(texts, prefix="")/
embed_one(text, prefix="") shape, so calling code can treat them
interchangeably. Three distinct real-world prefix conventions, all handled
by prefix_for() below so nothing else has to branch on embedder name:
  - nomic:  both sides prefixed differently in asymmetric mode
            ("search_query: " / "search_document: "), neither in symmetric.
  - bge-m3: never prefixed, either side, either mode (model card: no
            instructions needed at all).
  - qwen:   only the query side is ever prefixed, and only in asymmetric
            mode ("Instruct: ...\\nQuery:...") - documents never get one.
"""
from __future__ import annotations

import bge_embedder
import nomic_embedder
import qwen_embedder

EMBEDDERS = {
    "nomic": nomic_embedder,
    "bge-m3": bge_embedder,
    "qwen": qwen_embedder,
}


def prefix_for(embedder_name: str, mode: str, side: str) -> str:
    """side is 'query' or 'document'."""
    if mode != "asymmetric":
        return ""
    if embedder_name == "nomic":
        return nomic_embedder.QUERY_PREFIX if side == "query" else nomic_embedder.DOCUMENT_PREFIX
    if embedder_name == "qwen":
        return qwen_embedder.QUERY_PREFIX if side == "query" else ""
    return ""  # bge-m3: no prefix on either side, in either mode
