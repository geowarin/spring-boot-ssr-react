function values(obj) {
  const values = [];
  for (const key of Object.keys(obj)) {
    values.push(obj[key]);
  }
  return values;
}

class WsPlugin {
  constructor() {
    this.assets = {};
    this.ws = require('socket.io').listen(3000);
    this.ws.on('connection', this.onConnect.bind(this));
  }

  onConnect(socket) {
    this.emitAssets(socket);
  }

  emitAssets(socket, assets) {
    const destination = socket ? socket : this.ws;
    const updatedAssets = values(assets || this.assets);
    console.log(updatedAssets.map(a => a.name));
    destination.emit('wp-emit', updatedAssets);
  }

  apply(compiler) {

    compiler.plugin('done', stats => {
      const assets = stats.compilation.assets;
      const assetNames = Object.keys(assets);

      const newAssets = assetNames.reduce((res, assetName) => {
        const asset = assets[assetName];
        return Object.assign(res, {
          [assetName]: {
            name: assetName,
            source: asset.source()
          }
        })
      }, {});

      values(newAssets).forEach(asset => {
        this.assets[asset.name] = asset;
      });

      this.emitAssets(null, newAssets);
    });

    // compiler.plugin('invalid', () => { });

    // compiler.plugin('emit', (curCompiler, callback) => {
    //   const stats = curCompiler.getStats();
    //   callback();
    // });

    // compiler.plugin('watch-run', () => { });
    // compiler.plugin('run', () => { });
  }
}

module.exports = WsPlugin;