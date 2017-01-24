'use strict';

const generateDll = require('../src/generateDll');

try {

  // const pages = [
  //   {
  //     name: 'home',
  //     file: "/Users/geowarin/dev/projects/boot-wp/demo/src/main/js/pages/home.js",
  //   }
  // ];
  const options = {
    pages: [],
    projectDirectory: "/Users/geowarin/dev/projects/toto-is-a-test",
    watchDirectories: ["/Users/geowarin/dev/projects/toto-is-a-test"]
  };
  const errorCallback = (e) => console.error(e);
  const compilationCallback = (err, warn, assets) => console.log(assets.map(a => a.name));
  generateDll(options, errorCallback, compilationCallback);
} catch (e) {
  console.error(e);
}

