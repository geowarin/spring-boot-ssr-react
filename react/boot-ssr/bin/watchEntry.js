'use strict';

const watch = require('../src/watch');
try {
  process.env.NODE_ENV = 'development';
  watch(options, errorCallback, compilationCallback);
} catch (e) {
  errorCallback(e);
}

