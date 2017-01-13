"use strict";

function getAssets(compilation) {
  const assets = compilation.assets;
  const assetNames = Object.keys(assets);

  return assetNames.map(assetName => {
    const asset = assets[assetName];
    const source = asset.source();
    // updateHash, size ?
    return {
      name: assetName,
      source: source
    }
  });
}

module.exports = getAssets;