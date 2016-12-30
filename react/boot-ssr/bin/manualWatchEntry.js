'use strict';

const watch = require('../src/watch');

try {
  const pages = ["/Users/geowarin/dev/projects/boot-wp/react/src/test/resources/watch/page1.js"];
  const errorCallback = (e) => console.error(e);
  const compilationCallback = (err, warn, assets) => console.log(assets.map(a => a.name));
  watch(pages, errorCallback, compilationCallback);
} catch (e) {
  console.error(e.message);
}

