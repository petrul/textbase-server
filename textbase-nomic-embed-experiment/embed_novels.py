#!/usr/bin/env python3
"""Script 1: select ~20 works across different languages plus at least two
Bible translations (Romanian, French), walk each work's full chapter tree,
embed every leaf chapter with Ollama's nomic-embed-text:v1.5, and store the
vectors in the "int" Milvus's test_experiment_embeddings collection.

"Novel" here just means "some random work sampled from the corpus" - textbase's
live API doesn't expose genre metadata, so this is best-effort diversity of
language/author, not a genre filter.

Usage:
    uv run embed_novels.py --dry-run             # just discover + count leaves, no embedding
    uv run embed_novels.py                       # for real, symmetric (doc-vs-doc) mode
    uv run embed_novels.py --mode asymmetric --recreate
                                                  # asymmetric (query-vs-passage IR) mode, own collection
"""
from __future__ import annotations

import argparse
import sys
import time

from langdetect import DetectorFactory, LangDetectException, detect

import milvus_common
import embedders
import textbase_client as tb

DetectorFactory.seed = 0  # deterministic langdetect

TARGET_WORK_COUNT = 20
MAX_RANDOM_SAMPLES = 500
MIN_SAMPLE_CHARS = 60
BATCH_SIZE = 16
BATCH_PAUSE_S = 1.0  # be a reasonable neighbour to the OCR job sharing this Ollama instance


def detect_language(text: str) -> str | None:
    text = text.strip()
    if len(text) < MIN_SAMPLE_CHARS:
        return None
    try:
        return detect(text)
    except LangDetectException:
        return None


def sample_text_for_language(opus_path: str) -> str:
    try:
        text = tb.fetch_text(opus_path)
    except Exception:
        text = ""
    if len(text.strip()) >= MIN_SAMPLE_CHARS:
        return text
    try:
        html = tb.fetch_html(opus_path)
    except Exception:
        return text
    for child in tb.direct_child_paths(opus_path, html)[:5]:
        try:
            child_text = tb.fetch_text(child)
        except Exception:
            continue
        if len(child_text.strip()) >= MIN_SAMPLE_CHARS:
            return child_text
    return text


def truncate_utf8(text: str, max_bytes: int) -> str:
    """Milvus VARCHAR max_length is in UTF-8 *bytes*, not characters - a
    plain text[:n] slice can still overflow it for non-ASCII text (Cyrillic,
    diacritics, etc.), which is exactly what crashed the first real run on a
    Spanish-language chapter. Truncate by encoded bytes instead, dropping any
    partial trailing multi-byte character."""
    encoded = text.encode("utf-8")
    if len(encoded) <= max_bytes:
        return text
    return encoded[:max_bytes].decode("utf-8", errors="ignore")


def opus_title(opus_path: str) -> tuple[str, str]:
    """(author display, work title) parsed from the opus root page's <title>."""
    try:
        html = tb.fetch_html(opus_path)
        title = tb.page_title(html) or opus_path
    except Exception:
        return opus_path.split("/")[0], opus_path
    parts = [p.strip() for p in title.split("—")]  # em dash, as used by textbase's <title>
    author = parts[0] if parts else opus_path.split("/")[0]
    work = parts[1] if len(parts) > 1 else title
    return author, work


def discover_novels(target_count: int = TARGET_WORK_COUNT) -> list[dict]:
    """Repeatedly samples /util/random, dedupes by opus, detects language
    locally, and keeps distinct-language works (falling back to a 2nd work
    per language if the corpus doesn't have `target_count` distinct
    languages)."""
    seen_opus: set[str] = set()
    by_language: dict[str, list[dict]] = {}
    selected: list[dict] = []

    for i in range(MAX_RANDOM_SAMPLES):
        if len(selected) >= target_count:
            break
        try:
            leaf_url = tb.random_leaf_url()
        except Exception as exc:
            print(f"  [warn] /util/random failed: {exc}", file=sys.stderr)
            continue
        leaf_path = tb.path_of(leaf_url)
        opus_path = tb.opus_path_of(leaf_path)
        if opus_path in seen_opus:
            continue
        seen_opus.add(opus_path)

        try:
            sample = tb.fetch_text(leaf_path)
        except Exception:
            continue
        lang = detect_language(sample)
        if lang is None:
            continue

        author, work = opus_title(opus_path)
        entry = {"opus_path": opus_path, "language": lang, "author": author, "opus": work}
        is_new_language = lang not in by_language
        by_language.setdefault(lang, []).append(entry)
        print(f"  sampled #{i}: {opus_path}  lang={lang}  ({author} - {work})")

        # Prefer language diversity: only auto-accept the *first* work seen
        # per language on this pass.
        if is_new_language:
            selected.append(entry)

    # If the corpus doesn't have `target_count` distinct languages, top up
    # with additional works from languages we already have.
    if len(selected) < target_count:
        for entries in by_language.values():
            for entry in entries[1:]:
                if len(selected) >= target_count:
                    break
                selected.append(entry)
            if len(selected) >= target_count:
                break

    return selected[:target_count]


def find_bible(language: str, queries: list[str]) -> dict | None:
    """Looks for an opus-level (whole-work) divHeads hit whose own heading
    matches one of `queries` - i.e. the work itself is titled "Biblia .."/
    "Bible ..", not just some chapter that happens to mention the word."""
    for query in queries:
        try:
            hits = tb.search_div_heads(query, limit=20)
        except Exception as exc:
            print(f"  [warn] divHeads search for {query!r} failed: {exc}", file=sys.stderr)
            continue
        for hit in hits:
            data = hit.get("data", {})
            if not data.get("opus"):
                continue
            opus_path = data.get("path")
            if not opus_path:
                continue
            author, work = opus_title(opus_path)
            sample = sample_text_for_language(opus_path)
            detected = detect_language(sample) or language
            return {"opus_path": opus_path, "language": language if detected == language else detected, "author": author, "opus": work}
    return None


def embed_and_store(client, works: list[dict], embedder_name: str, mode: str) -> int:
    collection = milvus_common.collection_name(embedder_name, mode)
    embedder = embedders.EMBEDDERS[embedder_name]
    prefix = embedders.prefix_for(embedder_name, mode, "document")
    total_inserted = 0
    for work in works:
        opus_path = work["opus_path"]
        print(f"\n=== {work['author']} - {work['opus']}  [{work['language']}]  ({opus_path}) ===")
        leaves = tb.walk_leaves(opus_path)
        print(f"  {len(leaves)} leaf chapters found")

        batch_divs: list[tb.Div] = []
        batch_texts: list[str] = []

        def flush() -> None:
            nonlocal total_inserted
            if not batch_texts:
                return
            vectors = embedder.embed_batch(batch_texts, prefix=prefix)
            rows = [
                {
                    "vector": vectors[i],
                    "url": truncate_utf8(batch_divs[i].url, 1000),
                    "path": truncate_utf8(batch_divs[i].path, 500),
                    "author": truncate_utf8(work["author"], 200),
                    "opus": truncate_utf8(work["opus"], 300),
                    "language": truncate_utf8(work["language"], 10),
                    "head": truncate_utf8(batch_divs[i].head or "", 500),
                    "word_count": len(batch_texts[i].split()),
                    "text": truncate_utf8(batch_texts[i], 65535),
                }
                for i in range(len(batch_texts))
            ]
            client.insert(collection_name=collection, data=rows)
            total_inserted += len(rows)
            print(f"  embedded + stored {len(rows)} chapters (running total: {total_inserted})")
            time.sleep(BATCH_PAUSE_S)
            batch_divs.clear()
            batch_texts.clear()

        for div in leaves:
            try:
                text = tb.fetch_text(div.path)
            except Exception as exc:
                print(f"  [warn] failed to fetch {div.path}: {exc}", file=sys.stderr)
                continue
            if len(text.strip()) < 20:
                continue  # skip near-empty leaves (dividers, images-only, etc.)
            batch_divs.append(div)
            batch_texts.append(text)
            if len(batch_texts) >= BATCH_SIZE:
                flush()
        flush()
    return total_inserted


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true", help="discover + count leaves only, no embedding/storing")
    parser.add_argument("--target-count", type=int, default=TARGET_WORK_COUNT)
    parser.add_argument("--recreate", action="store_true", help="drop and recreate the collection before inserting")
    parser.add_argument(
        "--mode",
        choices=["symmetric", "asymmetric"],
        default="symmetric",
        help="symmetric: no task prefix, doc-vs-doc similarity (default, matches the existing "
        "test_experiment_embeddings collection). asymmetric: nomic's 'search_document: ' prefix "
        "for query-vs-passage IR, stored in a separate test_experiment_embeddings_asym collection. "
        "Ignored for --embedder bge-m3, which needs no prefix in either mode.",
    )
    parser.add_argument("--embedder", choices=list(embedders.EMBEDDERS), default="nomic")
    args = parser.parse_args()

    print(f"Discovering up to {args.target_count} works across distinct languages via /util/random ...")
    novels = discover_novels(args.target_count)
    print(f"\nSelected {len(novels)} works across {len({n['language'] for n in novels})} distinct languages:")
    for n in novels:
        print(f"  [{n['language']}] {n['author']} - {n['opus']}  ({n['opus_path']})")

    print("\nLooking for a Romanian Bible ...")
    bible_ro = find_bible("ro", ["Biblia"])
    print("Looking for a French Bible ...")
    bible_fr = find_bible("fr", ["Ancien Testament", "Nouveau Testament", "Bible"])

    extra = [b for b in (bible_ro, bible_fr) if b]
    for b in extra:
        print(f"  [{b['language']}] {b['author']} - {b['opus']}  ({b['opus_path']})")
    if not bible_ro:
        print("  [warn] no Romanian Bible found via search", file=sys.stderr)
    if not bible_fr:
        print("  [warn] no French Bible found via search", file=sys.stderr)

    existing_paths = {n["opus_path"] for n in novels}
    all_works = novels + [b for b in extra if b["opus_path"] not in existing_paths]

    if args.dry_run:
        print("\n--dry-run: counting leaves per work (no embedding) ...")
        total = 0
        for work in all_works:
            leaves = tb.walk_leaves(work["opus_path"])
            total += len(leaves)
            print(f"  {work['opus_path']}: {len(leaves)} leaf chapters")
        print(f"\nTotal leaf chapters across {len(all_works)} works: {total}")
        return

    collection = milvus_common.collection_name(args.embedder, args.mode)
    client = milvus_common.get_client()
    milvus_common.ensure_collection(client, args.embedder, args.mode, recreate=args.recreate)
    total = embed_and_store(client, all_works, args.embedder, args.mode)
    client.flush(collection_name=collection)
    print(f"\nDone. Inserted {total} chapter embeddings into '{collection}' ({args.embedder}, {args.mode}) at {milvus_common.MILVUS_URI}.")


if __name__ == "__main__":
    main()
