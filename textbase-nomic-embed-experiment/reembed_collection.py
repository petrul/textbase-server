#!/usr/bin/env python3
"""Re-embed every row already stored in one collection using a different
encoder, writing the result to a new/different collection. Reuses the exact
same documents (url/path/author/opus/language/head/word_count/text) already
fetched from textbase - no re-crawling, no re-selecting works at random.

Usage:
    # re-embed the original nomic-symmetric corpus (test_experiment_embeddings,
    # 11127 rows as of writing) with bge-m3, into test_experiment_embeddings_bge_m3
    uv run reembed_collection.py --to-embedder bge-m3 --recreate
"""
from __future__ import annotations

import argparse
import time

import embedders
import milvus_common

BATCH_SIZE = 16
BATCH_PAUSE_S = 1.0

FIELDS = ["url", "path", "author", "opus", "language", "head", "word_count", "text"]


def iter_source_rows(client, collection: str):
    iterator = client.query_iterator(
        collection_name=collection,
        filter="id >= 0",
        output_fields=FIELDS,
        batch_size=1000,
    )
    while True:
        batch = iterator.next()
        if not batch:
            iterator.close()
            break
        yield from batch


def flush(client, embedder, prefix: str, target_collection: str, rows: list[dict]) -> int:
    texts = [r["text"] for r in rows]
    vectors = embedder.embed_batch(texts, prefix=prefix)
    data = [{"vector": vectors[i], **{f: rows[i][f] for f in FIELDS}} for i in range(len(rows))]
    client.insert(collection_name=target_collection, data=data)
    time.sleep(BATCH_PAUSE_S)
    return len(data)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--from-embedder", default="nomic", choices=list(embedders.EMBEDDERS))
    parser.add_argument("--from-mode", default="symmetric", choices=["symmetric", "asymmetric"])
    parser.add_argument("--to-embedder", required=True, choices=list(embedders.EMBEDDERS))
    parser.add_argument("--to-mode", default="symmetric", choices=["symmetric", "asymmetric"])
    parser.add_argument("--recreate", action="store_true")
    args = parser.parse_args()

    source_collection = milvus_common.collection_name(args.from_embedder, args.from_mode)
    target_collection = milvus_common.collection_name(args.to_embedder, args.to_mode)
    if source_collection == target_collection:
        raise SystemExit(f"source and target are both '{source_collection}' - refusing to overwrite in place")

    client = milvus_common.get_client()
    if not client.has_collection(source_collection):
        raise SystemExit(f"source collection '{source_collection}' does not exist")
    source_count = client.get_collection_stats(source_collection)["row_count"]
    print(f"Source: '{source_collection}' ({args.from_embedder}, {args.from_mode}) - {source_count} rows")
    print(f"Target: '{target_collection}' ({args.to_embedder}, {args.to_mode})")

    milvus_common.ensure_collection(client, args.to_embedder, args.to_mode, recreate=args.recreate)

    embedder = embedders.EMBEDDERS[args.to_embedder]
    doc_prefix = embedders.prefix_for(args.to_embedder, args.to_mode, "document")

    batch: list[dict] = []
    total = 0
    for row in iter_source_rows(client, source_collection):
        batch.append(row)
        if len(batch) >= BATCH_SIZE:
            total += flush(client, embedder, doc_prefix, target_collection, batch)
            print(f"  re-embedded {total}/{source_count}")
            batch.clear()
    if batch:
        total += flush(client, embedder, doc_prefix, target_collection, batch)
        print(f"  re-embedded {total}/{source_count}")

    client.flush(collection_name=target_collection)
    print(f"\nDone. Re-embedded {total} rows from '{source_collection}' into '{target_collection}' using {args.to_embedder}.")


if __name__ == "__main__":
    main()
