'use strict';

const watch = require('../src/watch');

try {

  const pages = [
    {
      name: 'home',
      file: "/Users/geowarin/dev/projects/boot-wp/demo/src/main/js/pages/home.js",
    }
  ];
  const options = {
    pages: pages,
    watchDirectories: ["/Users/geowarin/dev/projects/boot-wp/demo/src/main/js"]
  };
  const errorCallback = (e) => console.error(e);
  const compilationCallback = (err, warn, assets) => console.log(assets.map(a => a.name));
  watch(options, errorCallback, compilationCallback);
} catch (e) {
  console.error(e);
}

