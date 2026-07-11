from pathlib import Path

from docx import Document


ROOT = Path(__file__).resolve().parents[1]
DOCX = ROOT / "CET46" / "组号-项目名称-项目开发总结报告2026.docx"


def xml_text(elem):
    return "".join(node.text or "" for node in elem.xpath(".//*[local-name()='t']")).strip()


doc = Document(DOCX)
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

print(f"DOCX={DOCX}")
print(f"bounds={start},{end}")
if start is not None:
    for idx, child in enumerate(children[start : end or len(children)], start):
        tag = child.tag.split("}")[-1]
        text = xml_text(child).replace("\n", "")
        drawings = len(child.xpath(".//*[local-name()='drawing']"))
        if tag == "tbl":
            rows = len(child.xpath(".//*[local-name()='tr']"))
            cols = max((len(row.xpath("./*[local-name()='tc']")) for row in child.xpath(".//*[local-name()='tr']")), default=0)
            print(f"{idx:04d} TABLE rows={rows} cols={cols} drawings={drawings} text={text[:160]}")
        else:
            style = child.xpath("./*[local-name()='pPr']/*[local-name()='pStyle']/@*[local-name()='val']")
            style = style[0] if style else ""
            print(f"{idx:04d} PARA style={style} drawings={drawings} text={text[:180]}")
