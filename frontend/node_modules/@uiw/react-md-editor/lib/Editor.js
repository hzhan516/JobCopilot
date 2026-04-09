"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault")["default"];
Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = void 0;
var _reactMarkdownPreview = _interopRequireDefault(require("@uiw/react-markdown-preview"));
var _TextArea = _interopRequireDefault(require("./components/TextArea/"));
var _Editor = require("./Editor.factory");
var _default = exports["default"] = (0, _Editor.createMDEditor)({
  MarkdownPreview: _reactMarkdownPreview["default"],
  TextArea: _TextArea["default"]
});
module.exports = exports.default;