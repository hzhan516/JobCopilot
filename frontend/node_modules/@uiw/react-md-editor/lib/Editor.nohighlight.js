"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault")["default"];
Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = void 0;
var _nohighlight = _interopRequireDefault(require("@uiw/react-markdown-preview/nohighlight"));
var _index = _interopRequireDefault(require("./components/TextArea/index.nohighlight"));
var _Editor = require("./Editor.factory");
var _default = exports["default"] = (0, _Editor.createMDEditor)({
  MarkdownPreview: _nohighlight["default"],
  TextArea: _index["default"]
});
module.exports = exports.default;