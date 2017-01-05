'use strict';

const compile = require('../src/compile');
try {
  compile(options, errorCallback, compilationCallback);
} catch (e) {
  errorCallback(e);
}
