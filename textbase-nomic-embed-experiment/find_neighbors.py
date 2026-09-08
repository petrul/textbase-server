#!/usr/bin/env python3
"""Script 2: pick a random subchapter from textbase (via /util/random), embed
it with the same Ollama nomic-embed-text:v1.5 encoder used to build the
collection, and print the 20 closest stored chapters (url + text) from
Milvus's test_experiment_embeddings.

Two modes, matching embed_novels.py --mode: "symmetric" (default) queries the
doc-vs-doc collection with no task prefix; "asymmetric" queries the
query-vs-passage collection with nomic's "search_query: " prefix on this end
(the corpus side was stored with "search_document: " - see nomic_embedder.py).
Querying the wrong collection with the wrong prefix convention is meaningless,
so make sure --mode matches what embed_novels.py was actually run with.

Usage:
    uv run find_neighbors.py                      # one random query, preview text
    uv run find_neighbors.py --full                # print full chapter text, not a preview
    uv run find_neighbors.py --top-k 10
    uv run find_neighbors.py --lang en              # keep re-rolling /util/random until it lands on English
    uv run find_neighbors.py --mode asymmetric      # query the query-vs-passage IR collection instead
    uv run find_neighbors.py https://textbase.scriptorium.ro/yeats/the_green/a_drinking_song
                                                     # query with this exact subchapter instead of a random one
    uv run find_neighbors.py --mode asymmetric --text "What does the Bible say about wine and love?"
                                                     # query with arbitrary text that isn't in textbase at all -
                                                     # the whole point of asymmetric/IR mode
"""
from __future__ import annotations

import argparse
import sys

from embed_novels import detect_language
import embedders
import milvus_common
import textbase_client as tb

MAX_LANG_ATTEMPTS = 200


def preview(text: str, length: int = 280) -> str:
    text = " ".join(text.split())
    return text if len(text) <= length else text[:length].rstrip() + "…"


def pick_query(
    target_lang: str | None, given_url: str | None, free_text: str | None = None
) -> tuple[str | None, str | None, str]:
    """Returns (leaf_url, leaf_path, text). If free_text is set, it's used
    verbatim as the query - it does not need to come from textbase at all
    (leaf_url/leaf_path come back as None, since there's nothing to link to
    or self-match against). Otherwise, if given_url is set, that exact
    textbase subchapter is used as-is (no random sampling at all). Otherwise,
    if target_lang is set, keeps re-rolling /util/random (like
    embed_novels.py's discovery pass) until a sample detects as that
    language."""
    if free_text is not None:
        return None, None, free_text

    if given_url is not None:
        leaf_path = tb.path_of(given_url) if "//" in given_url else given_url.strip("/")
        leaf_url = f"{tb.BASE_URL}/{leaf_path}"
        return leaf_url, leaf_path, tb.fetch_text(leaf_path)

    for attempt in range(1, MAX_LANG_ATTEMPTS + 1):
        leaf_url = tb.random_leaf_url()
        leaf_path = tb.path_of(leaf_url)
        text = tb.fetch_text(leaf_path)
        if target_lang is None:
            return leaf_url, leaf_path, text
        lang = detect_language(text)
        print(f"  [{attempt}] {leaf_path}  lang={lang}", file=sys.stderr)
        if lang == target_lang:
            return leaf_url, leaf_path, text
    raise SystemExit(f"Gave up after {MAX_LANG_ATTEMPTS} random draws without finding a '{target_lang}' subchapter")


def search(query_text: str, leaf_path: str | None, embedder_name: str, mode: str, top_k: int) -> list[dict]:
    """Embeds query_text (with the query-side prefix if applicable - see
    embedders.prefix_for) and returns up to top_k neighbour hits (each a
    Milvus search result dict with 'distance' and 'entity'), skipping a
    self-match on leaf_path."""
    embedder = embedders.EMBEDDERS[embedder_name]
    prefix = embedders.prefix_for(embedder_name, mode, "query")
    query_vector = embedder.embed_one(query_text, prefix=prefix)

    client = milvus_common.get_client()
    results = client.search(
        collection_name=milvus_common.collection_name(embedder_name, mode),
        data=[query_vector],
        limit=top_k + 1,  # +1 in case the query itself is in the collection
        output_fields=["url", "path", "author", "opus", "language", "head", "word_count", "text"],
    )[0]

    hits = [hit for hit in results if hit["entity"]["path"] != leaf_path]
    return hits[:top_k]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--top-k", type=int, default=20)
    parser.add_argument("--full", action="store_true", help="print full chapter text instead of a preview")
    parser.add_argument("--lang", default=None, help="keep sampling /util/random until it lands on this langdetect code (e.g. en, fr, ro)")
    parser.add_argument("--mode", choices=["symmetric", "asymmetric"], default="symmetric", help="must match the --mode embed_novels.py was run with")
    parser.add_argument("--embedder", choices=list(embedders.EMBEDDERS), default="nomic")
    parser.add_argument("--text", "-q", default=None, help="query with this arbitrary text instead of a textbase subchapter - does not need to be part of the corpus at all")
    parser.add_argument("url", nargs="?", default=None, help="query with this exact subchapter (full URL or bare path) instead of a random one - skips --lang")
    args = parser.parse_args()

    if args.text and (args.url or args.lang):
        print("[warn] --url/--lang are ignored when --text is given", file=sys.stderr)
    elif args.url and args.lang:
        print("[warn] --lang is ignored when a url is given", file=sys.stderr)

    leaf_url, leaf_path, query_text = pick_query(args.lang, args.url, args.text)

    print(f"Query: {leaf_url or '(free text, not from textbase)'}")
    print(f"Query text preview: {preview(query_text, 200)}\n")

    hits = search(query_text, leaf_path, args.embedder, args.mode, args.top_k)
    for rank, hit in enumerate(hits, start=1):
        entity = hit["entity"]
        print(f"#{rank}  score={hit['distance']:.4f}  [{entity['language']}]  {entity['author']} — {entity['opus']}")
        print(f"    {entity['url']}")
        print(f"    {entity['text'] if args.full else preview(entity['text'])}\n")


if __name__ == "__main__":
    main()
