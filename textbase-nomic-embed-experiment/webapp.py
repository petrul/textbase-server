#!/usr/bin/env python3
"""Small local web UI for find_neighbors.py: fill in a textbase subchapter
URL, or hit "Random" (optionally with a language filter), pick symmetric
(doc-vs-doc) or asymmetric (query-vs-passage IR) mode, and see the nearest
stored chapters rendered as cards.

Usage:
    uv run webapp.py            # serves http://127.0.0.1:8765
"""
from __future__ import annotations

from flask import Flask, jsonify, render_template, request

import embedders
from find_neighbors import pick_query, preview, search

app = Flask(__name__)


@app.get("/")
def index():
    return render_template("index.html")


@app.get("/api/search")
def api_search():
    url = (request.args.get("url") or "").strip() or None
    lang = (request.args.get("lang") or "").strip() or None
    text = (request.args.get("text") or "").strip() or None
    mode = request.args.get("mode") or "symmetric"
    embedder_name = request.args.get("embedder") or "nomic"
    top_k = max(1, min(50, int(request.args.get("top_k", 20))))

    if mode not in ("symmetric", "asymmetric"):
        return jsonify({"error": f"invalid mode {mode!r}"}), 400
    if embedder_name not in embedders.EMBEDDERS:
        return jsonify({"error": f"invalid embedder {embedder_name!r}"}), 400

    try:
        leaf_url, leaf_path, query_text = pick_query(lang, url, text)
    except SystemExit as exc:
        return jsonify({"error": str(exc)}), 502
    except Exception as exc:
        return jsonify({"error": f"Could not fetch that subchapter: {exc}"}), 400

    try:
        hits = search(query_text, leaf_path, embedder_name, mode, top_k)
    except Exception as exc:
        return jsonify({"error": f"Search failed (is the collection built yet?): {exc}"}), 502

    neighbors = [
        {
            "score": round(float(hit["distance"]), 4),
            "language": hit["entity"]["language"],
            "author": hit["entity"]["author"],
            "opus": hit["entity"]["opus"],
            "head": hit["entity"]["head"],
            "url": hit["entity"]["url"],
            "word_count": hit["entity"]["word_count"],
            "preview": preview(hit["entity"]["text"], 500),
        }
        for hit in hits
    ]

    return jsonify(
        {
            "query": {"url": leaf_url, "path": leaf_path, "preview": preview(query_text, 600)},
            "neighbors": neighbors,
        }
    )


if __name__ == "__main__":
    # 0.0.0.0 so it's reachable from other machines on the LAN (e.g. as
    # http://yoga.local:8765) - still no auth, so don't expose this port
    # beyond the LAN.
    app.run(host="0.0.0.0", port=8765, debug=False)
