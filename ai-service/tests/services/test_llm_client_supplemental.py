"""Test LLM client module (supplemental).
LLM 客户端补充测试：拆分原子性用例、补充异常场景。
"""
from unittest.mock import MagicMock, patch

import pytest

from app.services.llm_client import _extract_json_text


# ── _extract_json_text — 拆分后的原子用例 ─────────────────

def test_extract_json_text_json_codeblock():
    """Should extract JSON from ```json ... ``` block.
    应从 ```json 代码块中提取 JSON。"""
    raw = '```json\n{"key": "value"}\n```'
    assert _extract_json_text(raw) == '{"key": "value"}'


def test_extract_json_text_plain_codeblock():
    """Should extract JSON from ``` ... ``` block without language tag.
    应从无语言标签的代码块中提取 JSON。"""
    raw = '```\n{"key": "value"}\n```'
    assert _extract_json_text(raw) == '{"key": "value"}'


def test_extract_json_text_bare_json():
    """Should return bare JSON string as-is.
    裸 JSON 字符串应原样返回。"""
    raw = '{"key": "value"}'
    assert _extract_json_text(raw) == '{"key": "value"}'


def test_extract_json_text_embedded_in_text():
    """Should extract JSON embedded in surrounding text.
    应从包裹文本中提取内嵌 JSON。"""
    raw = 'Some intro\n{"key": "value"}\nSome outro'
    assert _extract_json_text(raw) == '{"key": "value"}'


def test_extract_json_text_no_json():
    """Should raise ValueError when no JSON object is found.
    无 JSON 对象时应抛出 ValueError。"""
    with pytest.raises(ValueError, match="did not contain a JSON object"):
        _extract_json_text("No json here")


def test_extract_json_text_incomplete_braces():
    """Should raise ValueError for unbalanced braces.
    大括号不匹配时应抛出 ValueError。"""
    raw = '{"content": "test", "nested": {"key": "val"}'
    with pytest.raises(ValueError, match="braces mismatch"):
        _extract_json_text(raw)


# ── generate_json_from_text_prompt — 补充异常场景 ────────

@patch("app.services.llm_client._generate_text")
def test_generate_json_returns_non_dict(mock_generate):
    """When LLM returns non-dict (e.g., list), should wrap or raise.
    LLM 返回非字典（如列表）时应包装或抛出异常。"""
    mock_generate.return_value = "[1, 2, 3]"

    from app.services.llm_client import generate_json_from_text_prompt
    with pytest.raises((ValueError, TypeError)):
        generate_json_from_text_prompt("prompt")


@patch("app.services.llm_client._generate_text")
def test_generate_json_returns_null(mock_generate):
    """When LLM returns 'null', should raise ValueError.
    LLM 返回 'null' 时应抛出 ValueError。"""
    mock_generate.return_value = "null"

    from app.services.llm_client import generate_json_from_text_prompt
    with pytest.raises((ValueError, TypeError)):
        generate_json_from_text_prompt("prompt")
