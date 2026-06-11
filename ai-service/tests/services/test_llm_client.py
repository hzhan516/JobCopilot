import json
import pytest
from unittest.mock import MagicMock, patch

from app.services.llm_client import (
    LlmJsonParseError,
    _extract_json_text,
    _generate_text,
    _safe_json_loads,
    generate_json_from_text_prompt_with_repair,
    generate_json_from_text_prompt,
    generate_json_from_image_prompt,
)


def test_extract_json_text_success():
    raw_text = '```json\n{"key": "value"}\n```'
    assert _extract_json_text(raw_text) == '{"key": "value"}'

    raw_text = '```\n{"key": "value"}\n```'
    assert _extract_json_text(raw_text) == '{"key": "value"}'

    raw_text = '{"key": "value"}'
    assert _extract_json_text(raw_text) == '{"key": "value"}'

    raw_text = 'Some text before\n{"key": "value"}\nSome text after'
    assert _extract_json_text(raw_text) == '{"key": "value"}'


def test_extract_json_text_nested_resume_payload():
    raw_text = (
        "```json\n"
        '{"name": "Alice", "experience": [{"company": "Acme", "title": "Engineer"}]}\n'
        "```"
    )

    assert _extract_json_text(raw_text) == (
        '{"name": "Alice", "experience": [{"company": "Acme", "title": "Engineer"}]}'
    )


def test_extract_json_text_ignores_braces_inside_strings():
    raw_text = 'prefix {"summary": "Worked with {templates} and escaped \\" braces", "ok": true} suffix'

    assert _extract_json_text(raw_text) == (
        '{"summary": "Worked with {templates} and escaped \\" braces", "ok": true}'
    )


def test_extract_json_text_failure():
    raw_text = "No json here"
    with pytest.raises(ValueError, match="LLM response did not contain a JSON object"):
        _extract_json_text(raw_text)


def test_extract_json_text_incomplete_json():
    # Missing closing brace for the outer object — braces mismatch triggers early rejection.
    # 外层对象的闭合大括号缺失，导致大括号数量不匹配，应被提前拦截。
    raw_text = '{"content": "test", "nested": {"key": "val"}'
    with pytest.raises(
        ValueError, match="Extracted JSON is incomplete: braces mismatch"
    ):
        _extract_json_text(raw_text)


@patch("app.services.llm_client.completion")
def test_generate_text_success(mock_completion):
    mock_response = MagicMock()
    mock_choice = MagicMock()
    mock_message = MagicMock()
    mock_message.content = "generated text"
    mock_choice.message = mock_message
    mock_choice.finish_reason = "stop"
    mock_response.choices = [mock_choice]
    mock_completion.return_value = mock_response

    result = _generate_text("model", [{"role": "user", "content": "prompt"}])
    assert result == "generated text"
    mock_completion.assert_called_once()
    call_kwargs = mock_completion.call_args.kwargs
    assert "max_tokens" in call_kwargs


@patch("app.services.llm_client.completion")
def test_generate_text_empty_response(mock_completion):
    mock_response = MagicMock()
    mock_choice = MagicMock()
    mock_message = MagicMock()
    mock_message.content = ""
    mock_choice.message = mock_message
    mock_choice.finish_reason = "stop"
    mock_response.choices = [mock_choice]
    mock_completion.return_value = mock_response

    with pytest.raises(ValueError, match="LiteLLM returned an empty response."):
        _generate_text("model", [{"role": "user", "content": "prompt"}])


@patch("app.services.llm_client.completion")
def test_generate_text_truncated_response(mock_completion):
    mock_response = MagicMock()
    mock_choice = MagicMock()
    mock_message = MagicMock()
    mock_message.content = '{"content": "truncated'
    mock_choice.message = mock_message
    mock_choice.finish_reason = "length"
    mock_response.choices = [mock_choice]
    mock_completion.return_value = mock_response

    with pytest.raises(ValueError, match="truncated due to token limit"):
        _generate_text("model", [{"role": "user", "content": "prompt"}])


@patch("app.services.llm_client._generate_text")
def test_generate_json_from_text_prompt(mock_generate):
    mock_generate.return_value = '{"result": "success"}'

    result = generate_json_from_text_prompt("test prompt")

    assert result == {"result": "success"}
    mock_generate.assert_called_once()
    kwargs = mock_generate.call_args.kwargs
    assert kwargs["messages"] == [{"role": "user", "content": "test prompt"}]


@patch("app.services.llm_client._generate_text")
def test_generate_json_from_text_prompt_with_repair_succeeds(mock_generate):
    mock_generate.side_effect = [
        '{"content": "bad "quote"", "fileUrl": null}',
        '{"content": "bad quote", "fileUrl": null}',
    ]

    result = generate_json_from_text_prompt_with_repair(
        "test prompt",
        repair_context='{"content": "string", "fileUrl": null}',
    )

    assert result.data == {"content": "bad quote", "fileUrl": None}
    assert result.repaired is True
    assert mock_generate.call_count == 2
    repair_messages = mock_generate.call_args_list[1].kwargs["messages"]
    assert "Convert the malformed model output" in repair_messages[0]["content"]


@patch("app.services.llm_client._generate_text")
def test_generate_json_from_text_prompt_with_repair_raises_with_raw_text(mock_generate):
    mock_generate.side_effect = [
        '{"content": "bad "quote"", "fileUrl": null}',
        "still not json",
    ]

    with pytest.raises(LlmJsonParseError) as exc_info:
        generate_json_from_text_prompt_with_repair("test prompt")

    assert exc_info.value.raw_text == '{"content": "bad "quote"", "fileUrl": null}'
    assert exc_info.value.repair_raw_text == "still not json"


@patch("app.services.llm_client._generate_text")
def test_generate_json_from_image_prompt(mock_generate):
    mock_generate.return_value = '{"result": "success"}'

    image_bytes = b"fake_image_data"
    result = generate_json_from_image_prompt("test prompt", image_bytes)

    assert result == {"result": "success"}
    mock_generate.assert_called_once()
    kwargs = mock_generate.call_args.kwargs
    messages = kwargs["messages"]
    assert len(messages) == 1
    assert messages[0]["role"] == "user"
    content = messages[0]["content"]
    assert len(content) == 2
    assert content[0] == {"type": "text", "text": "test prompt"}
    assert content[1]["type"] == "image_url"
    assert content[1]["image_url"]["url"].startswith("data:image/png;base64,")


def test_safe_json_loads_valid_json():
    text = '{"key": "value", "num": 42}'
    result = _safe_json_loads(text)
    assert result == {"key": "value", "num": 42}


def test_safe_json_loads_literal_newline():
    text = '{"content": "Hello' + chr(10) + 'World"}'
    result = _safe_json_loads(text)
    assert result == {"content": "Hello\nWorld"}


def test_safe_json_loads_literal_tab():
    text = '{"content": "Hello' + chr(9) + 'World"}'
    result = _safe_json_loads(text)
    assert result == {"content": "Hello\tWorld"}


def test_safe_json_loads_literal_cr():
    text = '{"content": "Hello' + chr(13) + 'World"}'
    result = _safe_json_loads(text)
    assert result == {"content": "Hello\rWorld"}


def test_safe_json_loads_multiple_control_chars():
    text = (
        '{"content": "Line1'
        + chr(10)
        + "Line2"
        + chr(9)
        + "Tab"
        + chr(13)
        + 'Carriage"}'
    )
    result = _safe_json_loads(text)
    assert result == {"content": "Line1\nLine2\tTab\rCarriage"}


def test_safe_json_loads_preserves_escaped_quotes():
    text = '{"content": "He said \\"hello\\""}'
    result = _safe_json_loads(text)
    assert result == {"content": 'He said "hello"'}


def test_safe_json_loads_complex_nested():
    text = (
        '{"data": {"content": "Hello' + chr(10) + 'World", '
        '"nested": {"text": "Tab' + chr(9) + 'here"}}, "ok": true}'
    )
    result = _safe_json_loads(text)
    assert result == {
        "data": {"content": "Hello\nWorld", "nested": {"text": "Tab\there"}},
        "ok": True,
    }


def test_safe_json_loads_unrepairable_raises():
    text = '{"content": "unclosed string}'
    with pytest.raises(json.JSONDecodeError):
        _safe_json_loads(text)
