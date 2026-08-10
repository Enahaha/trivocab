#!/usr/bin/env python3
"""Generate H2/MySQL seed SQL for the bundled trilingual word books.

Only Python's standard library is required.  The generated SQL deliberately uses
plain INSERT statements and standard SQL apostrophe escaping so that Spring's
SQL initializer can execute it against H2 running in MySQL compatibility mode.

The full seed (src/main/resources/db/data-h2.sql) is used for fresh databases.
The incremental seed (src/main/resources/db/data-book-fh-ielts.sql) is applied
by DatabaseBootstrap to existing databases, guarded by NOT EXISTS so that it is
safe to run repeatedly without touching user progress.
"""

from __future__ import annotations

import argparse
import json
import sys
import unicodedata
from pathlib import Path
from typing import Any, Iterable


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUTPUT = PROJECT_ROOT / "src/main/resources/db/data-h2.sql"
DEFAULT_EXTRA_OUTPUT = PROJECT_ROOT / "src/main/resources/db/data-book-fh-ielts.sql"

DEFAULT_BOOKS = [
    {
        "source": Path(
            "/Users/zhouzihao/Documents/ChatGPT/韩英中三语背词软件/"
            ".vocab_work/ielts_vocab_data.json"
        ),
        "book_id": 1,
        "code": "IELTS_3000",
        "name": "雅思英中韩重点词汇 3000",
        "description": "面向雅思备考的 3000 个英语重点词，同时提供中文与韩语释义。",
        "id_offset": 0,
        "word_id_prefix": "IELTS",
        "expected_count": 3000,
        "source_name": "ECDICT + Korean Wiktionary",
        "source_url": (
            "https://github.com/skywind3000/ECDICT | "
            "https://ko.wiktionary.org/"
        ),
    },
    {
        "source": Path(
            "/Users/zhouzihao/Documents/ChatGPT/韩英中三语背词软件/"
            ".vocab_work/github_ielts_vocab_data.json"
        ),
        "book_id": 2,
        "code": "FH_GITHUB_IELTS_3611",
        "name": "范洪滔 GitHub 雅思词汇表 3611",
        "description": (
            "新东方《雅思词汇词根+联想记忆法（乱序便携版）》手工抄录版，"
            "共 3611 个词条，按书中 Word List 顺序排列。"
        ),
        "id_offset": 1_000_000,
        "word_id_prefix": "FH-IELTS",
        "expected_count": 3611,
        "source_name": "GitHub IELTS Word List",
        "source_url": (
            "https://github.com/fanhongtao/IELTS/blob/master/"
            "IELTS%20Word%20List.txt"
        ),
    },
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--extra-output", type=Path, default=DEFAULT_EXTRA_OUTPUT)
    return parser.parse_args()


def first_present(row: dict[str, Any], *keys: str) -> Any:
    """Return the first non-null, non-blank value among candidate keys."""
    for key in keys:
        value = row.get(key)
        if value is None:
            continue
        if isinstance(value, str) and not value.strip():
            continue
        return value
    return None


def clean_text(value: Any) -> str | None:
    if value is None:
        return None
    text = unicodedata.normalize("NFC", str(value))
    text = text.replace("\x00", "").replace("\r\n", "\n").replace("\r", "\n")
    text = text.strip()
    return text or None


def sql_literal(value: Any) -> str:
    """Render a scalar as a SQL literal; blank strings intentionally become NULL."""
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "TRUE" if value else "FALSE"
    if isinstance(value, (int, float)) and not isinstance(value, bool):
        return str(value)

    text = clean_text(value)
    if text is None:
        return "NULL"
    # SQL-standard escaping.  This also handles the many apostrophes in IPA data.
    return "'" + text.replace("'", "''") + "'"


def require_int(value: Any, label: str) -> int:
    try:
        return int(value)
    except (TypeError, ValueError) as exc:
        raise ValueError(f"{label} must be an integer, got {value!r}") from exc


def source_metadata(row: dict[str, Any], book: dict[str, Any]) -> tuple[str | None, str | None]:
    en_url = clean_text(first_present(row, "source_en_zh_url", "source_url"))
    ko_url = clean_text(first_present(row, "source_ko_url"))
    fallback_name = book.get("source_name")
    fallback_url = book.get("source_url")

    names: list[str] = []
    urls: list[str] = []
    if en_url:
        names.append(fallback_name or "ECDICT")
        urls.append(en_url)
    elif fallback_url:
        urls.append(fallback_url)
    if ko_url:
        names.append("Korean Wiktionary")
        urls.append(ko_url)
    return " + ".join(names) or None, " | ".join(urls) or None


def normalize_row(row: dict[str, Any], fallback_id: int, book: dict[str, Any]) -> dict[str, Any]:
    book_id = require_int(book["book_id"], "book id")
    id_offset = require_int(book["id_offset"], "id offset")
    prefix = book["word_id_prefix"]

    priority = require_int(
        first_present(
            row, "ielts_priority_rank", "priority_rank", "rank"
        ) or fallback_id,
        f"priority rank for word id={fallback_id}",
    )
    word = clean_text(first_present(row, "english", "word", "headword"))
    if not word:
        raise ValueError(f"word id={fallback_id} has no English headword")

    source_name, source_url = source_metadata(row, book)
    return {
        "id": id_offset + fallback_id,
        "word_id": clean_text(first_present(row, "word_id", "external_word_id"))
        or f"{prefix}-{priority:04d}",
        "book_id": book_id,
        "priority_rank": priority,
        "word": word,
        "phonetic": clean_text(first_present(row, "phonetic", "pronunciation")),
        "part_of_speech": clean_text(
            first_present(row, "part_of_speech", "pos", "word_class")
        ),
        "chinese_meaning": clean_text(
            first_present(row, "chinese_meaning", "meaning_zh", "zh_meaning")
        ),
        "korean_meaning": clean_text(
            first_present(row, "korean_meaning", "meaning_ko", "ko_meaning")
        ),
        "korean_equivalents": clean_text(
            first_present(row, "korean_equivalents", "ko_equivalents")
        ),
        "korean_definition": clean_text(
            first_present(row, "korean_definition", "ko_definition")
        ),
        "korean_source_flag": clean_text(
            first_present(row, "korean_source_flag", "korean_source_type")
        ),
        "english_example": clean_text(
            first_present(row, "english_example", "example_en", "example_sentence_en")
        ),
        "korean_example": clean_text(
            first_present(row, "korean_example", "example_ko", "example_sentence_ko")
        ),
        "learning_stage": clean_text(
            first_present(row, "learning_stage", "source_section", "stage")
        ),
        "selection_basis": clean_text(
            first_present(row, "selection_basis", "selection_reason")
        ),
        "source_name": source_name,
        "source_url": source_url,
    }


def validate_rows(rows: list[dict[str, Any]], book: dict[str, Any]) -> None:
    expected_count = require_int(book["expected_count"], "expected count")
    if len(rows) != expected_count:
        raise ValueError(
            f"book {book['code']}: expected {expected_count} rows, found {len(rows)}"
        )

    ids = [row["id"] for row in rows]
    word_ids = [row["word_id"].casefold() for row in rows]
    ranks = [row["priority_rank"] for row in rows]
    for values, label in (
        (ids, "ids"),
        (word_ids, "word ids"),
        (ranks, "priority ranks"),
    ):
        if len(values) != len(set(values)):
            raise ValueError(f"book {book['code']}: duplicate {label} detected")

    expected_ranks = set(range(1, expected_count + 1))
    if set(ranks) != expected_ranks:
        missing = sorted(expected_ranks - set(ranks))[:10]
        raise ValueError(
            f"book {book['code']}: priority ranks are not contiguous; "
            f"first missing: {missing}"
        )

    for field in ("chinese_meaning", "korean_meaning"):
        missing_count = sum(row[field] is None for row in rows)
        if missing_count:
            raise ValueError(
                f"book {book['code']}: {field} is missing in {missing_count} rows"
            )


def render_insert(table: str, columns: Iterable[str], values: Iterable[Any]) -> str:
    column_list = ", ".join(columns)
    value_list = ", ".join(sql_literal(value) for value in values)
    return f"INSERT INTO {table} ({column_list}) VALUES ({value_list});"


def render_guarded_insert(
    table: str,
    columns: Iterable[str],
    values: Iterable[Any],
    guard: str,
) -> str:
    column_list = ", ".join(columns)
    value_list = ", ".join(sql_literal(value) for value in values)
    return (
        f"INSERT INTO {table} ({column_list})\n"
        f"SELECT {value_list} FROM DUAL\n"
        f"WHERE NOT EXISTS ({guard});"
    )


WORD_COLUMNS = (
    "id",
    "word_id",
    "book_id",
    "priority_rank",
    "word",
    "phonetic",
    "part_of_speech",
    "chinese_meaning",
    "korean_meaning",
    "korean_equivalents",
    "korean_definition",
    "korean_source_flag",
    "english_example",
    "korean_example",
    "learning_stage",
    "selection_basis",
    "source_name",
    "source_url",
)


def book_row(book: dict[str, Any], row_count: int) -> tuple[tuple[str, ...], tuple[Any, ...]]:
    return (
        ("id", "code", "name", "description", "total_words"),
        (book["book_id"], book["code"], book["name"], book["description"], row_count),
    )


def build_full_sql(
    books: list[dict[str, Any]],
    sources: list[Path],
    all_rows: list[list[dict[str, Any]]],
) -> str:
    lines = [
        "-- Generated by scripts/generate-seed.py. Do not edit by hand.",
        *[f"-- Source: {source}" for source in sources],
        *[
            f"-- {book['code']} words: {book['expected_count']}"
            for book in books
        ],
        "",
        render_insert(
            "users",
            ("id", "username", "display_name", "daily_goal", "selected_book_id"),
            (1, "demo", "IELTS Learner", 20, books[0]["book_id"]),
        ),
    ]
    for book, rows in zip(books, all_rows):
        book_columns, book_values = book_row(book, len(rows))
        lines.append(render_insert("word_books", book_columns, book_values))
    lines.append("")

    for book, rows in zip(books, all_rows):
        for row in rows:
            lines.append(
                render_insert("words", WORD_COLUMNS, (row[column] for column in WORD_COLUMNS))
            )
        lines.append("")
    return "\n".join(lines)


def build_extra_sql(book: dict[str, Any], rows: list[dict[str, Any]]) -> str:
    lines = [
        "-- Generated by scripts/generate-seed.py. Do not edit by hand.",
        f"-- Extra book: {book['code']} ({len(rows)} words)",
        f"-- Safe to run repeatedly: every statement is guarded by NOT EXISTS.",
        "",
    ]
    book_columns, book_values = book_row(book, len(rows))
    lines.append(
        render_guarded_insert(
            "word_books",
            book_columns,
            book_values,
            f"SELECT 1 FROM word_books WHERE code = {sql_literal(book['code'])}",
        )
    )
    lines.append("")
    for row in rows:
        lines.append(
            render_guarded_insert(
                "words",
                WORD_COLUMNS,
                (row[column] for column in WORD_COLUMNS),
                (
                    "SELECT 1 FROM words "
                    f"WHERE book_id = {row['book_id']} "
                    f"AND word_id = {sql_literal(row['word_id'])}"
                ),
            )
        )
    lines.append("")
    return "\n".join(lines)


def load_book(book: dict[str, Any]) -> list[dict[str, Any]]:
    source = book["source"]
    if not source.is_file():
        raise FileNotFoundError(f"Source JSON not found: {source}")
    with source.open("r", encoding="utf-8") as source_file:
        payload = json.load(source_file)
    raw_rows = payload.get("rows") if isinstance(payload, dict) else payload
    if not isinstance(raw_rows, list):
        raise ValueError(f"{source}: source JSON must be a row list or contain 'rows'")
    if any(not isinstance(row, dict) for row in raw_rows):
        raise ValueError(f"{source}: every source row must be a JSON object")
    rows = [
        normalize_row(row, index, book)
        for index, row in enumerate(raw_rows, start=1)
    ]
    rows.sort(key=lambda row: (row["priority_rank"], row["id"]))
    validate_rows(rows, book)
    return rows


def build_rows(books: list[dict[str, Any]]) -> list[list[dict[str, Any]]]:
    return [load_book(book) for book in books]


def main() -> int:
    args = parse_args()
    try:
        all_rows = build_rows(DEFAULT_BOOKS)
        full_sql = build_full_sql(
            DEFAULT_BOOKS,
            [book["source"] for book in DEFAULT_BOOKS],
            all_rows,
        )
        extra_book = DEFAULT_BOOKS[1]
        extra_sql = build_extra_sql(extra_book, all_rows[1])
    except (OSError, json.JSONDecodeError, ValueError) as exc:
        print(f"Seed generation failed: {exc}", file=sys.stderr)
        return 1

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(full_sql, encoding="utf-8", newline="\n")
    print(
        f"Generated {args.output} with "
        + ", ".join(
            f"{len(rows)} words ({book['code']})"
            for book, rows in zip(DEFAULT_BOOKS, all_rows)
        )
        + "."
    )

    if args.extra_output is not None:
        args.extra_output.parent.mkdir(parents=True, exist_ok=True)
        args.extra_output.write_text(extra_sql, encoding="utf-8", newline="\n")
        print(f"Generated {args.extra_output} with {len(all_rows[1])} words.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
