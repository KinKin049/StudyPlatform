from pathlib import Path

import sys

from docx import Document


ROOT = Path(__file__).resolve().parents[1]
DOCX = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "CET46" / "报告-latest-第五部分流程图无遮挡修订.docx"


def xml_text(elem):
    return "".join(node.text or "" for node in elem.xpath(".//w:t")).strip()


def find_section_bounds(doc):
    children = list(doc.element.body)
    start = None
    end = None
    for idx, child in enumerate(children):
        text = xml_text(child)
        if start is None and text.startswith("6") and "数据库设计" in text:
            start = idx
            continue
        if start is not None and idx > start and text.startswith("7"):
            end = idx
            break
    if start is None:
        raise RuntimeError("未找到第六部分起点")
    return start, end or len(children)


doc = Document(DOCX)
start, end = find_section_bounds(doc)
print(f"DOCX={DOCX}")
print(f"section6_bounds={start},{end}, count={end - start}")
for idx, child in enumerate(list(doc.element.body)[start:end], start):
    tag = child.tag.split("}")[-1]
    text = xml_text(child).replace("\n", "")
    drawings = len(child.xpath(".//w:drawing"))
    pics = len(child.xpath(".//pic:pic"))
    if tag == "tbl":
        rows = len(child.xpath(".//w:tr"))
        cols = max((len(row.xpath("./w:tc")) for row in child.xpath(".//w:tr")), default=0)
        print(f"{idx:04d} TABLE rows={rows} cols={cols} drawings={drawings} pics={pics} text={text[:140]}")
    else:
        style = child.xpath("./w:pPr/w:pStyle/@w:val")
        style = style[0] if style else ""
        print(f"{idx:04d} PARA style={style} drawings={drawings} pics={pics} text={text[:180]}")
