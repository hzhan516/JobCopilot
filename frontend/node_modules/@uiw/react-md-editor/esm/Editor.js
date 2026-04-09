import MarkdownPreview from '@uiw/react-markdown-preview';
import TextArea from "./components/TextArea/index.js";
import { createMDEditor } from './Editor.factory';
export default createMDEditor({
  MarkdownPreview,
  TextArea
});