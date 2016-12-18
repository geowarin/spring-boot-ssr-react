'use strict';

const compile = require('../src/compile');
try {
  compile(pages, errorCallback, compilationCallback);
} catch (e) {
  errorCallback(e);
}
