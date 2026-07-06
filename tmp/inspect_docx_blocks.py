from __future__ import annotations

import sys
from pathlib import Path

from docx import Document
from docx.oxml.ns import qn


def has_drawing(paragraph) -> bool:
    return bool(paragraph._p.xpath(".//w:drawing"))


def main() -> None:
    path = Path(sys.argv[1])
    document = Document(path)
    body = document.element.body
    for index, child in enumerate(body.iterchildren()):
        tag = child.tag
        if tag == qn("w:p"):
            paragraph = None
            for p in document.paragraphs:
                if p._p is child:
                    paragraph = p
                    break
            text = paragraph.text.strip() if paragraph else ""
            style = paragraph.style.name if paragraph else ""
            drawing = " [IMAGE]" if paragraph and has_drawing(paragraph) else ""
            print(f"{index:03d} P {style}{drawing}: {text[:120]}")
        elif tag == qn("w:tbl"):
            rows = len(child.xpath(".//w:tr"))
            print(f"{index:03d} T rows={rows}")
        else:
            print(f"{index:03d} {tag}")


if __name__ == "__main__":
    main()
