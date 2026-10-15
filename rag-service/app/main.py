"""FastAPI entry point of the RAG service.

Run from the rag-service folder:  uvicorn app.main:app --port 8000
"""
import logging
import os
import hmac
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from app.chunker import chunk_text, extract_title
from app.config import settings
from app.embedder import Embedder
from app.vector_store import VectorStore

logger = logging.getLogger("rag-service")
logging.basicConfig(level=logging.INFO)

SUPPORTED_SUFFIXES = {".md", ".txt"}


# ---------- request / response models ----------
class SearchRequest(BaseModel):
    query: str = Field(min_length=1, max_length=2000)
    top_k: int = Field(default=4, ge=1, le=20)
    min_score: float = Field(default=0.25, ge=-1.0, le=1.0)


class SearchHitModel(BaseModel):
    source: str
    title: str
    chunk_index: int
    score: float
    text: str


class SearchResponse(BaseModel):
    query: str
    hits: list[SearchHitModel]


class DocumentRequest(BaseModel):
    source: str = Field(min_length=1, max_length=200, pattern=r"^[\w.\- ]+$")
    content: str = Field(min_length=1)


class IngestResponse(BaseModel):
    documents: int
    chunks: int


# ---------- app wiring ----------
@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.embedder = Embedder(settings.embedding_model)
    app.state.store = VectorStore(settings.chroma_path, settings.collection_name)
    logger.info("Vector store ready with %d chunks", app.state.store.count())
    if os.getenv("INGEST_ON_STARTUP", "false").lower() == "true" and app.state.store.count() == 0:
        folder = Path(settings.knowledge_dir)
        for path in sorted(folder.iterdir()):
            if path.is_file() and path.suffix.lower() in SUPPORTED_SUFFIXES:
                _ingest_document(path.name, path.read_text(encoding="utf-8"))
        logger.info("Indexed bundled knowledge base: %d chunks", app.state.store.count())
    yield


app = FastAPI(title="Support Assistant RAG Service", version="0.1.0", lifespan=lifespan)


@app.middleware("http")
async def protect_service_endpoints(request, call_next):
    """Require the Java service token when configured; keep health checks public."""
    token = os.getenv("RAG_API_TOKEN", "")
    if token and request.url.path != "/health":
        supplied = request.headers.get("authorization", "")
        expected = f"Bearer {token}"
        if not hmac.compare_digest(supplied, expected):
            from fastapi.responses import JSONResponse

            return JSONResponse(status_code=401, content={"detail": "Unauthorized"})
    return await call_next(request)


def _ingest_document(source: str, content: str) -> int:
    chunks = chunk_text(content, settings.chunk_max_chars, settings.chunk_overlap)
    embeddings = app.state.embedder.embed(chunks)
    title = extract_title(content, fallback=source)
    return app.state.store.replace_source(source, title, chunks, embeddings)


# ---------- endpoints ----------
@app.get("/health")
def health():
    return {"status": "ok", "chunks": app.state.store.count(), "model": settings.embedding_model}


@app.post("/ingest", response_model=IngestResponse)
def ingest():
    """(Re)index every .md/.txt file in the configured knowledge directory."""
    folder = Path(settings.knowledge_dir)
    if not folder.is_dir():
        raise HTTPException(status_code=500, detail=f"Knowledge directory not found: {folder}")

    documents = chunks = 0
    for path in sorted(folder.iterdir()):
        if path.suffix.lower() not in SUPPORTED_SUFFIXES or not path.is_file():
            continue
        chunks += _ingest_document(path.name, path.read_text(encoding="utf-8"))
        documents += 1
    logger.info("Ingested %d documents into %d chunks", documents, chunks)
    return IngestResponse(documents=documents, chunks=chunks)


@app.post("/documents", response_model=IngestResponse)
def upsert_document(request: DocumentRequest):
    """Add or replace a single document without touching the knowledge folder."""
    return IngestResponse(documents=1, chunks=_ingest_document(request.source, request.content))


@app.post("/search", response_model=SearchResponse)
def search(request: SearchRequest):
    query_vector = app.state.embedder.embed([request.query])[0]
    hits = app.state.store.search(query_vector, request.top_k, request.min_score)
    return SearchResponse(
        query=request.query,
        hits=[SearchHitModel(source=h.source, title=h.title, chunk_index=h.chunk_index,
                             score=h.score, text=h.text) for h in hits],
    )
