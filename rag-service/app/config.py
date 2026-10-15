"""Runtime configuration, read from environment variables with sane defaults."""
import os
from dataclasses import dataclass
from pathlib import Path

_SERVICE_ROOT = Path(__file__).resolve().parent.parent


@dataclass(frozen=True)
class Settings:
    embedding_model: str = os.getenv("EMBEDDING_MODEL", "sentence-transformers/all-MiniLM-L6-v2")
    # Where ChromaDB persists its files.
    chroma_path: str = os.getenv("CHROMA_PATH", str(_SERVICE_ROOT / "data" / "chroma"))
    collection_name: str = os.getenv("COLLECTION_NAME", "support_kb")
    # Folder that POST /ingest reads (*.md and *.txt files).
    knowledge_dir: str = os.getenv("KNOWLEDGE_DIR", str(_SERVICE_ROOT.parent / "knowledge-base"))
    chunk_max_chars: int = int(os.getenv("CHUNK_MAX_CHARS", "800"))
    chunk_overlap: int = int(os.getenv("CHUNK_OVERLAP", "120"))


settings = Settings()
