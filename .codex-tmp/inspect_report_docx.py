from pathlib import Path
from docx import Document

ROOT = Path(__file__).resolve().parents[1]
latest = ROOT / "CET46" / "报告-latest.docx"
template = ROOT / "CET46" / "组号-项目名称-项目开发总结报告2026.docx"


def para_text(p):
    return "".join(run.text for run in p.runs).strip()


def print_section(doc_path, title_patterns, label):
    doc = Document(doc_path)
    print(f"\n===== {label}: {doc_path.name} =====")
    paras = [(i, p.style.name if p.style else "", para_text(p)) for i, p in enumerate(doc.paragraphs)]
    starts = [i for i, _, text in paras if any(pattern in text for pattern in title_patterns)]
    if not starts:
        print("No matching section")
        return
    start = starts[0]
    end = len(paras)
    for i, _, text in paras[start + 1 :]:
        if i > start and (text.startswith("第六") or text.startswith("6.") or text.startswith("六、")):
            end = i
            break
    for i, style, text in paras[start:end]:
        if text or "Heading" in style or "标题" in style:
            print(f"{i:04d} [{style}] {text[:240]}")


print_section(latest, ["第五", "5."], "LATEST SECTION 5")
print_section(template, ["第五", "5."], "TEMPLATE SECTION 5")
