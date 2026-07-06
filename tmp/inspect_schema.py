from __future__ import annotations

import re
from pathlib import Path


MIGRATION_DIR = Path("StudyPlatform-back/src/main/resources/db/migration")


def strip_comments(sql: str) -> str:
    sql = re.sub(r"/\*.*?\*/", "", sql, flags=re.S)
    sql = re.sub(r"--[^\n]*", "", sql)
    return sql


def split_columns(body: str) -> list[str]:
    items: list[str] = []
    current = []
    depth = 0
    quote = None
    for char in body:
        if quote:
            current.append(char)
            if char == quote:
                quote = None
            continue
        if char in "'\"`":
            quote = char
            current.append(char)
            continue
        if char == "(":
            depth += 1
        elif char == ")":
            depth = max(0, depth - 1)
        if char == "," and depth == 0:
            item = "".join(current).strip()
            if item:
                items.append(item)
            current = []
        else:
            current.append(char)
    item = "".join(current).strip()
    if item:
        items.append(item)
    return items


def clean_name(name: str) -> str:
    return name.strip().strip("`").strip()


def parse_create_tables(sql: str) -> dict[str, dict]:
    tables: dict[str, dict] = {}
    pattern = re.compile(
        r"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?`?([A-Za-z0-9_]+)`?\s*\((.*?)\)\s*(?:ENGINE|DEFAULT|CHARSET|COLLATE|;)",
        re.I | re.S,
    )
    for match in pattern.finditer(sql):
        table = clean_name(match.group(1))
        body = match.group(2)
        info = tables.setdefault(table, {"columns": {}, "pk": [], "fk": []})
        for item in split_columns(body):
            upper = item.upper()
            if upper.startswith("PRIMARY KEY"):
                cols = re.findall(r"`?([A-Za-z0-9_]+)`?", item[item.find("(") + 1 : item.rfind(")")])
                info["pk"].extend([clean_name(col) for col in cols])
                continue
            if upper.startswith("CONSTRAINT") or upper.startswith("FOREIGN KEY"):
                fk = re.search(
                    r"FOREIGN\s+KEY\s*\((.*?)\)\s+REFERENCES\s+`?([A-Za-z0-9_]+)`?\s*\((.*?)\)",
                    item,
                    re.I | re.S,
                )
                if fk:
                    from_cols = [clean_name(col) for col in re.findall(r"`?([A-Za-z0-9_]+)`?", fk.group(1))]
                    to_table = clean_name(fk.group(2))
                    to_cols = [clean_name(col) for col in re.findall(r"`?([A-Za-z0-9_]+)`?", fk.group(3))]
                    info["fk"].append((from_cols, to_table, to_cols, "declared"))
                continue
            if upper.startswith(("KEY ", "INDEX ", "UNIQUE ", "FULLTEXT ", "CHECK ")):
                continue
            column_match = re.match(r"`?([A-Za-z0-9_]+)`?\s+(.+)", item, re.S)
            if column_match:
                column = clean_name(column_match.group(1))
                definition = " ".join(column_match.group(2).split())
                info["columns"][column] = definition
                if "PRIMARY KEY" in upper and column not in info["pk"]:
                    info["pk"].append(column)
    return tables


def apply_alters(sql: str, tables: dict[str, dict]) -> None:
    for match in re.finditer(r"ALTER\s+TABLE\s+`?([A-Za-z0-9_]+)`?\s+(.*?);", sql, re.I | re.S):
        table = clean_name(match.group(1))
        body = match.group(2)
        info = tables.setdefault(table, {"columns": {}, "pk": [], "fk": []})
        for add in re.finditer(r"ADD\s+(?:COLUMN\s+)?`?([A-Za-z0-9_]+)`?\s+([^,;]+)", body, re.I | re.S):
            column = clean_name(add.group(1))
            definition = " ".join(add.group(2).split())
            if column.upper() not in {"CONSTRAINT", "KEY", "INDEX", "PRIMARY", "FOREIGN"}:
                info["columns"].setdefault(column, definition)
        fk = re.search(
            r"FOREIGN\s+KEY\s*\((.*?)\)\s+REFERENCES\s+`?([A-Za-z0-9_]+)`?\s*\((.*?)\)",
            body,
            re.I | re.S,
        )
        if fk:
            from_cols = [clean_name(col) for col in re.findall(r"`?([A-Za-z0-9_]+)`?", fk.group(1))]
            to_table = clean_name(fk.group(2))
            to_cols = [clean_name(col) for col in re.findall(r"`?([A-Za-z0-9_]+)`?", fk.group(3))]
            info["fk"].append((from_cols, to_table, to_cols, "declared"))


def infer_relationships(tables: dict[str, dict]) -> list[tuple[str, str, str, str, str]]:
    declared = []
    for table, info in tables.items():
        for from_cols, to_table, to_cols, source in info["fk"]:
            declared.append((table, ",".join(from_cols), to_table, ",".join(to_cols), source))
    existing = {(a, b, c, d) for a, b, c, d, _ in declared}
    inferred = []
    table_names = set(tables)
    aliases = {
        "user_id": ["users"],
        "course_id": ["courses", "teacher_published_courses"],
        "problem_id": ["oj_problems", "question_bank_problems"],
        "question_id": ["question_bank_problems"],
        "assignment_id": ["academy_assignments"],
        "exam_id": ["academy_exams"],
        "submission_id": ["oj_submissions"],
        "textbook_id": ["excellent_textbooks"],
        "order_id": ["textbook_orders"],
        "template_id": ["well_log_templates"],
    }
    for table, info in tables.items():
        for column in info["columns"]:
            if column == "id" or not column.endswith("_id"):
                continue
            candidates = aliases.get(column, [])
            stem = column[:-3]
            candidates.extend([stem, f"{stem}s", f"{stem}_records"])
            for candidate in candidates:
                if candidate in table_names and (table, column, candidate, "id") not in existing:
                    inferred.append((table, column, candidate, "id", "inferred"))
                    existing.add((table, column, candidate, "id"))
                    break
    return declared + inferred


def main() -> None:
    files = sorted(MIGRATION_DIR.glob("V*.sql"), key=lambda path: path.name)
    sql = "\n".join(path.read_text(encoding="utf-8", errors="ignore") for path in files)
    clean = strip_comments(sql)
    tables = parse_create_tables(clean)
    apply_alters(clean, tables)
    relationships = infer_relationships(tables)
    print(f"migration_files={len(files)}")
    print(f"tables={len(tables)}")
    for table in sorted(tables):
        info = tables[table]
        print(f"TABLE {table} columns={len(info['columns'])} pk={info['pk']}")
        for column, definition in list(info["columns"].items())[:12]:
            print(f"  - {column}: {definition[:90]}")
        if len(info["columns"]) > 12:
            print(f"  ... +{len(info['columns']) - 12} columns")
    print(f"relationships={len(relationships)}")
    for rel in sorted(relationships):
        print("REL", " -> ".join([f"{rel[0]}.{rel[1]}", f"{rel[2]}.{rel[3]}"]), rel[4])


if __name__ == "__main__":
    main()
