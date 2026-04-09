import Markdown from './Markdown.common';
import { createTextArea } from "./factory.js";
export default createTextArea({
  Markdown,
  useMinHeight: true
});