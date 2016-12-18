'use strict';

const watch = require('../src/watch');

try {
  console.log("totototottoto");
  // const pages = ["/Users/geowarin/dev/projects/boot-wp/react/src/test/resources/watch/page1.js"];
  const errorCallback = (e) => console.error(e.message);
  const compilationCallback = (err, warn, assets) => console.log(assets.map(a => a.name));
  watch(pages, errorCallback, compilationCallback);
} catch (e) {
  console.error(e.message);
}

