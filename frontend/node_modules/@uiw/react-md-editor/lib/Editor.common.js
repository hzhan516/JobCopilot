"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault")["default"];
Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = void 0;
var _common = _interopRequireDefault(require("@uiw/react-markdown-preview/common"));
var _index = _interopRequireDefault(require("./components/TextArea/index.common"));
var _Editor = require("./Editor.factory");
var _default = exports["default"] = (0, _Editor.createMDEditor)({
  MarkdownPreview: _common["default"],
  TextArea: _index["default"]
});
module.exports = exports.default;