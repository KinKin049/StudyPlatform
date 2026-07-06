from __future__ import annotations

import sys
from pathlib import Path

from docx import Document


def main() -> None:
    path = Path(sys.argv[1])
    chapter_prefix = sys.argv[2]
    next_prefix = sys.argv[3]
    out_path = Path(sys.argv[4])
    document = Document(path)
    lines: list[str] = []
    in_chapter = False
    for paragraph in document.paragraphs:
        text = paragraph.text.strip()
        if not text:
            continue
        normalized = text.replace("\u3000", " ").strip()
        if normalized.startswith(chapter_prefix):
            in_chapter = True
        if in_chapter and normalized.startswith(next_prefix):
            break
        if in_chapter:
            lines.append(text)
    out_path.write_text("\n".join(lines), encoding="utf-8")


if __name__ == "__main__":
    main()
