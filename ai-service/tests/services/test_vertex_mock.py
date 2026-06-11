"""Test vertex connectivity — Mock version.
Vertex AI 连接测试（Mock 版本）：验证 prompt 构造与参数传递，不调用真实 API。
"""

import pytest
from unittest.mock import MagicMock, patch

from app.services.llm_client import _generate_text


@patch("app.services.llm_client.completion")
def test_vertex_prompt_construction(mock_completion):
    """Should construct messages with correct role and content.
    应构造包含正确角色和内容的消息。"""
    mock_response = MagicMock()
    mock_choice = MagicMock()
    mock_message = MagicMock()
    mock_message.content = "Vertex Connection Successful!"
    mock_choice.message = mock_message
    mock_choice.finish_reason = "stop"
    mock_response.choices = [mock_choice]
    mock_completion.return_value = mock_response

    messages = [
        {
            "role": "user",
            "content": "Please reply with EXACTLY this text: 'Vertex Connection Successful!'",
        }
    ]
    result = _generate_text(model="gemini/gemini-1.5-flash", messages=messages)

    assert result == "Vertex Connection Successful!"
    mock_completion.assert_called_once()
    call_kwargs = mock_completion.call_args.kwargs
    assert call_kwargs["model"] == "gemini/gemini-1.5-flash"
    assert call_kwargs["messages"] == messages


@patch("app.services.llm_client.completion")
def test_vertex_empty_response(mock_completion):
    """Empty response should raise ValueError.
    空响应应抛出 ValueError。"""
    mock_response = MagicMock()
    mock_choice = MagicMock()
    mock_message = MagicMock()
    mock_message.content = ""
    mock_choice.message = mock_message
    mock_choice.finish_reason = "stop"
    mock_response.choices = [mock_choice]
    mock_completion.return_value = mock_response

    with pytest.raises(ValueError, match="empty response"):
        _generate_text(
            model="gemini/gemini-1.5-flash",
            messages=[{"role": "user", "content": "test"}],
        )


@patch("app.services.llm_client.completion")
def test_vertex_truncated_response(mock_completion):
    """Truncated response (finish_reason=length) should raise ValueError.
    截断响应应抛出 ValueError。"""
    mock_response = MagicMock()
    mock_choice = MagicMock()
    mock_message = MagicMock()
    mock_message.content = '{"partial": "data"'
    mock_choice.message = mock_message
    mock_choice.finish_reason = "length"
    mock_response.choices = [mock_choice]
    mock_completion.return_value = mock_response

    with pytest.raises(ValueError, match="truncated"):
        _generate_text(
            model="gemini/gemini-1.5-flash",
            messages=[{"role": "user", "content": "test"}],
        )


@patch("app.services.llm_client.completion")
def test_vertex_api_error(mock_completion):
    """API error should be propagated.
    API 错误应被传播。"""
    mock_completion.side_effect = Exception("API key invalid")

    with pytest.raises(Exception, match="API key invalid"):
        _generate_text(
            model="gemini/gemini-1.5-flash",
            messages=[{"role": "user", "content": "test"}],
        )
