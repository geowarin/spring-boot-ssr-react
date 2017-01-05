'use strict';

const watch = require('../src/watch');
try {
  watch(options, errorCallback, compilationCallback);
} catch (e) {
  console.error(e.stack)
  errorCallback(e);
}

