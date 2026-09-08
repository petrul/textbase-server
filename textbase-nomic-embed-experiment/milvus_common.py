"""Shared Milvus connection/schema for the test_experiment_embeddings collections.

Targets the "int" Milvus instance (scripts/docker/integration-deps/int-compose.yml,
"mini" profile), never prod - this is a throwaway experiment collection, not part
of the real textbase pipeline.

Collections are keyed by (embedder, mode):
  - nomic + symmetric   -> test_experiment_embeddings        (768-dim, no prefix)
  - nomic + asymmetric  -> test_experiment_embeddings_asym     (768-dim, search_query:/search_document: prefixes)
  - bge-m3 + either mode -> test_experiment_embeddings_bge_m3  (1024-dim, no prefix either way)
  - qwen + either mode   -> test_experiment_embeddings_qwen    (2560-dim, query-only instruction prefix)
  bge-m3 and qwen both embed documents identically regardless of mode (bge-m3 never prefixes
  anything; qwen only ever prefixes the query side - see embedders.py), so for both of them one
  collection covers both "modes" rather than duplicating identical document vectors under two
  names. Only nomic prefixes *both* sides differently per mode, so it alone needs two collections.
"""
from __future__ import annotations

from pymilvus import DataType, MilvusClient

MILVUS_URI = "http://mini.local:20112"

COLLECTIONS = {
    ("nomic", "symmetric"): {
        "name": "test_experiment_embeddings",
        "dim": 768,
        "description": (
            "Test experiment: chapter-leaf-div embeddings from textbase.scriptorium.ro, "
            "encoded with Ollama nomic-embed-text:v1.5 (768-dim, no task prefix - symmetric "
            "doc-vs-doc similarity) served at zmeu.local:11434. Built by scripts in "
            "~/work/textbase-nomic-embed-experiment - a one-off experiment, not part of the "
            "real textbase-nestjs vectorizer pipeline."
        ),
    },
    ("nomic", "asymmetric"): {
        "name": "test_experiment_embeddings_asym",
        "dim": 768,
        "description": (
            "Test experiment: chapter-leaf-div embeddings from textbase.scriptorium.ro, "
            "encoded with Ollama nomic-embed-text:v1.5 (768-dim) served at zmeu.local:11434, "
            "using nomic's 'search_document: ' task prefix for asymmetric (short query vs long "
            "passage) IR - query side must use 'search_query: ' (see nomic_embedder.py). Built "
            "by scripts in ~/work/textbase-nomic-embed-experiment - a one-off experiment, not "
            "part of the real textbase-nestjs vectorizer pipeline."
        ),
    },
    ("bge-m3", "symmetric"): {
        "name": "test_experiment_embeddings_bge_m3",
        "dim": 1024,
        "description": (
            "Test experiment: chapter-leaf-div embeddings from textbase.scriptorium.ro, "
            "encoded with Ollama bge-m3 (1024-dim, dense vector only) served at zmeu.local:11434. "
            "No task prefix on either side - unlike nomic, BGE-M3's own model card says it was "
            "fine-tuned not to need one, so the same vectors serve both symmetric doc-vs-doc "
            "similarity and asymmetric query-vs-passage IR; both modes share this one collection "
            "rather than duplicating identical vectors under two names. Re-embedded from the same "
            "documents as the nomic symmetric collection (test_experiment_embeddings), not a fresh "
            "textbase crawl. Built by scripts in ~/work/textbase-nomic-embed-experiment."
        ),
    },
    ("qwen", "symmetric"): {
        "name": "test_experiment_embeddings_qwen",
        "dim": 2560,
        "description": (
            "Test experiment: chapter-leaf-div embeddings from textbase.scriptorium.ro, "
            "encoded with Ollama qwen3-embedding:4b (2560-dim) served at zmeu.local:11434. "
            "Documents are stored with no instruction prefix, per the model's own card ('No need "
            "to add instruction for retrieval documents') - only the QUERY side gets an "
            "instruction prefix for asymmetric/IR search ('Instruct: <task>\\nQuery:<query>'), so "
            "one collection covers both symmetric and asymmetric modes here too (see "
            "qwen_embedder.py). Re-embedded from the same documents as the nomic symmetric "
            "collection (test_experiment_embeddings), not a fresh textbase crawl. NOTE: "
            "qwen3-embedding:4b was found to contend badly for GPU memory with the qwen3-vl OCR "
            "job also running on zmeu's Ollama instance - expect slow/inconsistent embedding "
            "latency; avoid running large batches while OCR is active. Built by scripts in "
            "~/work/textbase-nomic-embed-experiment."
        ),
    },
}


def _key(embedder: str, mode: str) -> tuple[str, str]:
    # bge-m3 and qwen have one collection regardless of mode - see module docstring.
    return (embedder, "symmetric") if embedder in ("bge-m3", "qwen") else (embedder, mode)


def collection_name(embedder: str, mode: str) -> str:
    return COLLECTIONS[_key(embedder, mode)]["name"]


def get_client() -> MilvusClient:
    return MilvusClient(uri=MILVUS_URI)


def ensure_collection(client: MilvusClient, embedder: str, mode: str, recreate: bool = False) -> None:
    spec = COLLECTIONS[_key(embedder, mode)]
    name = spec["name"]
    if client.has_collection(name):
        if not recreate:
            return
        client.drop_collection(name)

    schema = client.create_schema(auto_id=True, enable_dynamic_field=False, description=spec["description"])
    schema.add_field("id", DataType.INT64, is_primary=True, auto_id=True)
    schema.add_field("vector", DataType.FLOAT_VECTOR, dim=spec["dim"])
    schema.add_field("url", DataType.VARCHAR, max_length=1000)
    schema.add_field("path", DataType.VARCHAR, max_length=500)
    schema.add_field("author", DataType.VARCHAR, max_length=200)
    schema.add_field("opus", DataType.VARCHAR, max_length=300)
    schema.add_field("language", DataType.VARCHAR, max_length=10)
    schema.add_field("head", DataType.VARCHAR, max_length=500)
    schema.add_field("word_count", DataType.INT64)
    schema.add_field("text", DataType.VARCHAR, max_length=65535)

    index_params = client.prepare_index_params()
    index_params.add_index(
        field_name="vector",
        index_type="HNSW",
        metric_type="COSINE",
        params={"M": 16, "efConstruction": 200},
    )

    client.create_collection(
        collection_name=name,
        schema=schema,
        index_params=index_params,
    )
    client.load_collection(name)
