"""ChromaDB-backed vector store using cosine similarity."""
from dataclasses import dataclass

import chromadb


@dataclass(frozen=True)
class Hit:
    source: str
    title: str
    chunk_index: int
    score: float  # cosine similarity, 1.0 = identical
    text: str


class VectorStore:
    def __init__(self, path: str, collection_name: str):
        self._client = chromadb.PersistentClient(path=path)
        self._collection = self._client.get_or_create_collection(
            name=collection_name,
            metadata={"hnsw:space": "cosine"},
        )

    def replace_source(self, source: str, title: str, chunks: list[str],
                       embeddings: list[list[float]]) -> int:
        """Delete every chunk of `source`, then store the new ones (idempotent re-ingest)."""
        self._collection.delete(where={"source": source})
        if not chunks:
            return 0
        self._collection.upsert(
            ids=[f"{source}::{i}" for i in range(len(chunks))],
            documents=chunks,
            embeddings=embeddings,
            metadatas=[{"source": source, "title": title, "chunk_index": i}
                       for i in range(len(chunks))],
        )
        return len(chunks)

    def search(self, query_embedding: list[float], top_k: int, min_score: float) -> list[Hit]:
        total = self._collection.count()
        if total == 0:
            return []
        result = self._collection.query(
            query_embeddings=[query_embedding],
            n_results=min(top_k, total),
            include=["documents", "metadatas", "distances"],
        )
        hits: list[Hit] = []
        for text, meta, distance in zip(result["documents"][0],
                                        result["metadatas"][0],
                                        result["distances"][0]):
            score = 1.0 - float(distance)
            if score >= min_score:
                hits.append(Hit(meta["source"], meta["title"], int(meta["chunk_index"]),
                                round(score, 4), text))
        return hits

    def count(self) -> int:
        return self._collection.count()
