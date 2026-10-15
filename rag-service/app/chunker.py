"""Split documents into overlapping chunks that respect paragraph boundaries.

Pure functions only (no third-party imports) so they are trivial to unit test.
"""
import re

_SENTENCE_SPLIT = re.compile(r"(?<=[.!?])\s+")
_PARAGRAPH_SPLIT = re.compile(r"\n\s*\n")


def _split_long(text: str, max_chars: int) -> list[str]:
    """Break an oversized paragraph on sentence boundaries, hard-slicing as a last resort."""
    if len(text) <= max_chars:
        return [text]

    pieces: list[str] = []
    current = ""
    for sentence in _SENTENCE_SPLIT.split(text):
        if len(sentence) > max_chars:  # a single monster sentence
            if current:
                pieces.append(current)
                current = ""
            pieces.extend(sentence[i:i + max_chars] for i in range(0, len(sentence), max_chars))
        elif not current:
            current = sentence
        elif len(current) + 1 + len(sentence) <= max_chars:
            current += " " + sentence
        else:
            pieces.append(current)
            current = sentence
    if current:
        pieces.append(current)
    return pieces


def _overlap_tail(text: str, overlap: int) -> str:
    """Last `overlap` characters of text, trimmed forward to a word boundary."""
    if overlap <= 0:
        return ""
    tail = text[-overlap:]
    space = tail.find(" ")
    return tail[space + 1:] if space != -1 else tail


def chunk_text(text: str, max_chars: int = 800, overlap: int = 120) -> list[str]:
    """Return chunks of roughly `max_chars` characters.

    A chunk may exceed `max_chars` by up to `overlap` characters because the tail of the
    previous chunk is prepended to keep context across boundaries.
    """
    if max_chars <= 0:
        raise ValueError("max_chars must be positive")

    paragraphs = [p.strip() for p in _PARAGRAPH_SPLIT.split(text) if p.strip()]
    chunks: list[str] = []
    current = ""

    for paragraph in paragraphs:
        for piece in _split_long(paragraph, max_chars):
            if not current:
                current = piece
            elif len(current) + 2 + len(piece) <= max_chars:
                current += "\n\n" + piece
            else:
                chunks.append(current)
                tail = _overlap_tail(current, overlap)
                current = f"{tail}\n\n{piece}" if tail else piece

    if current:
        chunks.append(current)
    return chunks


def extract_title(text: str, fallback: str) -> str:
    """Use the first Markdown '# heading' as the document title."""
    for line in text.splitlines():
        line = line.strip()
        if line.startswith("# "):
            return line[2:].strip()
    return fallback
