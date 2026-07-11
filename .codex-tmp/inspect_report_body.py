from pathlib import Path
from docx import Document
from docx.oxml.ns import qn

ROOT = Path(__file__).resolve().parents[1]
doc = Document(ROOT / "CET46" / "报告-latest.docx")

body = doc.element.body
items = []
for idx, child in enumerate(body):
    tag = child.tag.split("}")[-1]
    text = "".join(child.itertext()).strip().replace("\n", "")
    drawings = len(child.xpath(".//w:drawing"))
    pics = len(child.xpath(".//pic:pic"))
    if tag == "tbl":
        rows = len(child.xpath(".//w:tr"))
        items.append((idx, "TABLE", rows, drawings, pics, text[:120]))
    elif tag == "p":
        style = ""
        p_style = child.xpath("./w:pPr/w:pStyle/@w:val")
        if p_style:
            style = p_style[0]
        items.append((idx, "PARA", style, drawings, pics, text[:180]))

start = next((i for i, item in enumerate(items) if item[-1].startswith("5")), 0)
end = next((i for i, item in enumerate(items[start + 1 :], start + 1) if item[-1].startswith("6")), len(items))

for item in items[start:end]:
    print(item)
