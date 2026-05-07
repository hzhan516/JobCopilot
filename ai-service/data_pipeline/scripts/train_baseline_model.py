import json
from pathlib import Path


OUTPUT_DIR = Path(__file__).resolve().parents[1] / "output"
FEATURES_FILE = OUTPUT_DIR / "training_features_sample.jsonl"
MODEL_FILE = OUTPUT_DIR / "baseline_model.json"


FEATURE_KEYS = [
    "skill_overlap_ratio",
    "title_keyword_overlap",
    "experience_description_overlap",
]


def load_jsonl(path: Path) -> list[dict]:
    rows: list[dict] = []

    with path.open("r", encoding="utf-8") as file:
        for line in file:
            line = line.strip()
            if not line:
                continue
            rows.append(json.loads(line))

    return rows


def avg(items: list[dict], key: str) -> float:
    if not items:
        return 0.0

    return sum(float(item.get(key, 0.0)) for item in items) / len(items)


def compute_average_scores(rows: list[dict]) -> dict[str, float]:
    positive_rows = [row for row in rows if row.get("label_suitable") is True]
    negative_rows = [row for row in rows if row.get("label_suitable") is False]

    return {
        "positive_skill_overlap_ratio": round(avg(positive_rows, "skill_overlap_ratio"), 4),
        "negative_skill_overlap_ratio": round(avg(negative_rows, "skill_overlap_ratio"), 4),
        "positive_title_keyword_overlap": round(avg(positive_rows, "title_keyword_overlap"), 4),
        "negative_title_keyword_overlap": round(avg(negative_rows, "title_keyword_overlap"), 4),
        "positive_experience_description_overlap": round(avg(positive_rows, "experience_description_overlap"), 4),
        "negative_experience_description_overlap": round(avg(negative_rows, "experience_description_overlap"), 4),
        "positive_label_score": round(avg(positive_rows, "label_score"), 4),
        "negative_label_score": round(avg(negative_rows, "label_score"), 4),
    }


def build_model_artifact(rows: list[dict]) -> dict:
    """Build a weighted-feature baseline model by comparing positive vs. negative class averages.
    构建加权特征基线模型：通过正负样本在各特征上的均值差异推导权重，
    并对特征做归一化处理，使模型输出与 LLM 评分可比，用于线上 ensemble 兜底。"""
    positive_rows = [row for row in rows if row.get("label_suitable") is True]
    negative_rows = [row for row in rows if row.get("label_suitable") is False]

    raw_weights: dict[str, float] = {}
    normalization: dict[str, float] = {}

    for key in FEATURE_KEYS:
        positive_avg = avg(positive_rows, key)
        negative_avg = avg(negative_rows, key)

        raw_weights[key] = max(positive_avg - negative_avg, 0.0)

        max_value = max(float(row.get(key, 0.0)) for row in rows) if rows else 1.0
        normalization[key] = max(max_value, 1.0)

    total_weight = sum(raw_weights.values())

    if total_weight == 0:
        feature_weights = {key: round(1 / len(FEATURE_KEYS), 4) for key in FEATURE_KEYS}
    else:
        feature_weights = {
            key: round(raw_weights[key] / total_weight, 4)
            for key in FEATURE_KEYS
        }

    return {
        "model_type": "weighted_feature_baseline",
        "version": "v1",
        "feature_weights": feature_weights,
        "normalization": normalization,
        "bias": 0.0,
    }


def main() -> None:
    rows = load_jsonl(FEATURES_FILE)

    summary = {
        "total_rows": len(rows),
        "positive_rows": sum(1 for row in rows if row.get("label_suitable") is True),
        "negative_rows": sum(1 for row in rows if row.get("label_suitable") is False),
        "feature_averages": compute_average_scores(rows),
        "model_artifact": build_model_artifact(rows),
    }

    with MODEL_FILE.open("w", encoding="utf-8") as file:
        json.dump(summary, file, ensure_ascii=False, indent=2)

    print(f"Wrote baseline model summary to {MODEL_FILE}")
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
