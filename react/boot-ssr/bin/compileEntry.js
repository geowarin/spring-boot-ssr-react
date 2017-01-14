'use strict';

const compile = require('../src/compile');
try {
  process.env.NODE_ENV = 'production';
  compile(options, errorCallback, compilationCallback);
} catch (e) {
  errorCallback(e);
}
