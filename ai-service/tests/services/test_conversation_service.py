from app.services.conversation_service import _infer_file_format

def test_infer_file_format():
    assert _infer_file_format("http://example.com/resume.pdf") == "pdf"
    assert _infer_file_format("http://example.com/resume.docx") == "docx"
    assert _infer_file_format("http://example.com/resume.txt") == "txt"
    assert _infer_file_format("http://example.com/resume.md") == "md"
    assert _infer_file_format("http://example.com/resume.jpg") is None
    assert _infer_file_format("http://example.com/resume") is None
