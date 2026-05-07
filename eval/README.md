# AI Evaluation Results

This directory contains the evaluation harness and generated results for the
Resume Assistant AI pipeline.

## What Is Evaluated

The current evaluation run uses fixed benchmark cases in `eval/data/` and calls
the real AI-service parsing and scoring functions:

- Resume parsing extraction quality
- Job posting parsing extraction quality
- Single-job resume suitability scoring
- Legacy job ranking quality against a keyword baseline

## Metrics

| Area | Metric | Latest Result |
| --- | --- | --- |
| Resume parsing | Average skill F1 | 1.0000 |
| Resume parsing | Name accuracy | 1.0000 |
| Resume parsing | Email accuracy | 1.0000 |
| Job parsing | Average requirement F1 | 0.9231 |
| Job parsing | Title accuracy | 1.0000 |
| Job parsing | Company accuracy | 1.0000 |
| Suitability scoring | Decision accuracy | 1.0000 |
| Suitability scoring | Score range accuracy | 1.0000 |
| Job ranking | System NDCG@5 | 1.0000 |
| Job ranking | Baseline NDCG@5 | 0.7059 |

The machine-readable output for the latest run is committed at
`eval/results/metrics.json`.

## Baseline Comparison

The ranking evaluation compares the AI-assisted ranking against a fixed keyword
baseline. In the latest run, the system achieved `NDCG@5 = 1.0000`, while the
baseline achieved `NDCG@5 = 0.7059`.

## How To Reproduce

From the repository root:

```bash
cd eval
../ai-service/.venv/bin/python run_eval.py --include-legacy-ranking
```

If using a fresh environment:

```bash
cd ai-service
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

cd ../eval
../ai-service/.venv/bin/python run_eval.py --include-legacy-ranking
```

The evaluation requires a configured `.env` file with a LiteLLM-compatible
provider key matching `LLM_TEXT_MODEL`.

