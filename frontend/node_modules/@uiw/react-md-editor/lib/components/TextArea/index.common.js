"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault")["default"];
Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = void 0;
var _Markdown = _interopRequireDefault(require("./Markdown.common"));
var _factory = require("./factory");
var _default = exports["default"] = (0, _factory.createTextArea)({
  Markdown: _Markdown["default"],
  useMinHeight: true
});
module.exports = exports.default;