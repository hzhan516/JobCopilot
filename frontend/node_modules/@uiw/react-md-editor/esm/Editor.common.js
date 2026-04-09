import MarkdownPreview from '@uiw/react-markdown-preview/common';
import TextArea from './components/TextArea/index.common';
import { createMDEditor } from './Editor.factory';
export default createMDEditor({
  MarkdownPreview,
  TextArea
});