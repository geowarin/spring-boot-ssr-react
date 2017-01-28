'use strict';
const transformErrors = require('friendly-errors-webpack-plugin/src/core/transformErrors');

const defaultTransformers = [
  require('friendly-errors-webpack-plugin/src/transformers/babelSyntax'),
  require('friendly-errors-webpack-plugin/src/transformers/moduleNotFound'),
  require('friendly-errors-webpack-plugin/src/transformers/esLintError'),
];

function extractErrors(errors) {
  return transformErrors(errors, defaultTransformers)
}

module.exports = extractErrors;
