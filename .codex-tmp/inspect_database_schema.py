import re
from collections import OrderedDict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MIGRATIONS = ROOT / "StudyPlatform-back" / "src" / "main" / "resources" / "db" / "migration"


def clean_sql(text):
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    text = re.sub(r"--.*?$", "", text, flags=re.M)
    return text


def split_top_level_csv(text):
    parts = []
    start = 0
    depth = 0
    in_quote = None
    i = 0
    while i < len(text):
        ch = text[i]
        if in_quote:
            if ch == in_quote:
                if i + 1 < len(text) and text[i + 1] == in_quote:
                    i += 1
                else:
                    in_quote = None
        else:
            if ch in ("'", '"', "`"):
                in_quote = ch
            elif ch == "(":
                depth += 1
            elif ch == ")":
                depth = max(0, depth - 1)
            elif ch == "," and depth == 0:
                parts.append(text[start:i].strip())
                start = i + 1
        i += 1
    last = text[start:].strip()
    if last:
        parts.append(last)
    return parts


def normalize_type(raw):
    raw = raw.strip()
    m = re.match(r"([A-Z]+)(?:\(([^)]*)\))?", raw, flags=re.I)
    if not m:
        return raw.upper(), ""
    typ = m.group(1).upper()
    length = (m.group(2) or "").strip()
    return typ, length


def parse_column(defn):
    m = re.match(r"`?([A-Za-z_][\w]*)`?\s+(.+)$", defn.strip(), flags=re.S)
    if not m:
        return None
    name, rest = m.group(1), m.group(2).strip()
    if name.upper() in {"PRIMARY", "FOREIGN", "CONSTRAINT", "UNIQUE", "KEY", "INDEX", "CHECK"}:
        return None
    typ, length = normalize_type(rest)
    return {
        "name": name,
        "type": typ,
        "length": length,
        "pk": bool(re.search(r"\bPRIMARY\s+KEY\b", rest, re.I)),
        "fk": "",
        "ref": "",
        "raw": rest,
    }


def iter_create_table_blocks(sql):
    pattern = re.compile(r"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?`?([A-Za-z_][\w]*)`?\s*\(", re.I)
    for m in pattern.finditer(sql):
        table = m.group(1)
        start = m.end() - 1
        depth = 0
        in_quote = None
        end = None
        i = start
        while i < len(sql):
            ch = sql[i]
            if in_quote:
                if ch == in_quote:
                    if i + 1 < len(sql) and sql[i + 1] == in_quote:
                        i += 1
                    else:
                        in_quote = None
            else:
                if ch in ("'", '"', "`"):
                    in_quote = ch
                elif ch == "(":
                    depth += 1
                elif ch == ")":
                    depth -= 1
                    if depth == 0:
                        end = i
                        break
            i += 1
        if end is not None:
            yield table, sql[start + 1 : end]


def init_table(schema, table):
    schema.setdefault(table, OrderedDict())


def add_column(schema, table, col):
    init_table(schema, table)
    if col and col["name"] not in schema[table]:
        schema[table][col["name"]] = col


def mark_primary(schema, table, columns):
    for col in columns:
        if table in schema and col in schema[table]:
            schema[table][col]["pk"] = True


def mark_foreign(schema, table, columns, ref_table):
    for col in columns:
        if table in schema and col in schema[table]:
            schema[table][col]["fk"] = "外键"
            schema[table][col]["ref"] = ref_table


def parse_columns_list(text):
    return [c.strip(" `") for c in text.split(",") if c.strip()]


def parse_schema():
    schema = OrderedDict()
    sql_files = sorted(MIGRATIONS.glob("V*.sql"), key=lambda p: [int(x) if x.isdigit() else x for x in re.split(r"(\d+)", p.name)])
    full_text = "\n".join(clean_sql(p.read_text(encoding="utf-8", errors="ignore")) for p in sql_files)
    dynamic_sql_parts = []
    for m in re.finditer(r"'(.*?)'", full_text, flags=re.S):
        value = m.group(1).replace("''", "'")
        if "ALTER TABLE" in value or "CREATE TABLE" in value:
            dynamic_sql_parts.append(value)
    dynamic_sql = ";\n".join(dynamic_sql_parts)
    combined = full_text + "\n" + dynamic_sql

    for table, body in iter_create_table_blocks(combined):
        init_table(schema, table)
        for item in split_top_level_csv(body):
            col = parse_column(item)
            if col:
                add_column(schema, table, col)
                continue
            pk = re.search(r"PRIMARY\s+KEY\s*\(([^)]*)\)", item, re.I)
            if pk:
                mark_primary(schema, table, parse_columns_list(pk.group(1)))
            fk = re.search(r"FOREIGN\s+KEY\s*\(([^)]*)\)\s+REFERENCES\s+`?([A-Za-z_][\w]*)`?", item, re.I)
            if fk:
                mark_foreign(schema, table, parse_columns_list(fk.group(1)), fk.group(2))

    for stmt in split_statements(combined):
        m = re.match(r"\s*ALTER\s+TABLE\s+`?([A-Za-z_][\w]*)`?\s+(.*)$", stmt, re.I | re.S)
        if not m:
            continue
        table, body = m.group(1), m.group(2)
        for item in split_top_level_csv(body):
            col_match = re.match(r"\s*ADD\s+(?:COLUMN\s+)?`?([A-Za-z_][\w]*)`?\s+(.+)$", item, re.I | re.S)
            if not col_match:
                continue
            col_name, rest = col_match.group(1), col_match.group(2)
            if col_name.upper() in {"CONSTRAINT", "PRIMARY", "FOREIGN", "UNIQUE", "INDEX", "KEY", "CHECK"}:
                continue
            col = parse_column(f"{col_name} {rest}")
            add_column(schema, table, col)

    for m in re.finditer(r"ALTER\s+TABLE\s+`?([A-Za-z_][\w]*)`?.*?FOREIGN\s+KEY\s*\(([^)]*)\)\s+REFERENCES\s+`?([A-Za-z_][\w]*)`?", combined, re.I | re.S):
        mark_foreign(schema, m.group(1), parse_columns_list(m.group(2)), m.group(3))

    return schema


def split_statements(text):
    statements = []
    start = 0
    in_quote = None
    i = 0
    while i < len(text):
        ch = text[i]
        if in_quote:
            if ch == in_quote:
                if i + 1 < len(text) and text[i + 1] == in_quote:
                    i += 1
                else:
                    in_quote = None
        else:
            if ch in ("'", '"', "`"):
                in_quote = ch
            elif ch == ";":
                stmt = text[start:i].strip()
                if stmt:
                    statements.append(stmt)
                start = i + 1
        i += 1
    tail = text[start:].strip()
    if tail:
        statements.append(tail)
    return statements


if __name__ == "__main__":
    schema = parse_schema()
    print(f"table_count={len(schema)}")
    for table, cols in schema.items():
        pk = [c["name"] for c in cols.values() if c["pk"]]
        fk = [f"{c['name']}->{c['ref']}" for c in cols.values() if c["fk"]]
        print(f"{table}: columns={len(cols)} pk={pk} fk={fk}")
