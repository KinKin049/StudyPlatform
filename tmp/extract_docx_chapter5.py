from __future__ import annotations

import sys
from pathlib import Path

from docx import Document


def iter_block_text(document: Document):
    for paragraph in document.paragraphs:
        text = paragraph.text.strip()
        if text:
            yield text


def main() -> None:
    path = Path(sys.argv[1])
    out_path = Path(sys.argv[2]) if len(sys.argv) > 2 else None
    document = Document(path)
    lines = []
    in_chapter = False
    for text in iter_block_text(document):
        normalized = text.replace("\u3000", " ").strip()
        if normalized.startswith("5 ") or normalized.startswith("5."):
            in_chapter = True
        if in_chapter and (normalized.startswith("6 ") or normalized.startswith("6.")):
            break
        if in_chapter:
            lines.append(text)
    if out_path:
        out_path.write_text("\n".join(lines), encoding="utf-8")
    else:
        print("\n".join(lines))


if __name__ == "__main__":
    main()
