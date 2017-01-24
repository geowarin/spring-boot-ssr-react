'use strict';

const generateDll = require('../src/generateDll');
try {
  process.env.NODE_ENV = 'generate-dll';
  generateDll(options, errorCallback, compilationCallback);
} catch (e) {
  errorCallback(e);
}
