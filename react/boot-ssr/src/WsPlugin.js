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

  emitAssets(socket) {
    const destination = socket ? socket : this.ws;
    console.log('published', Object.keys(this.assets));
    destination.emit('wp-emit', values(this.assets));
  }

  apply(compiler) {

    compiler.plugin('done', stats => {
      const assets = stats.compilation.assets;
      const assetNames = Object.keys(assets);
      assetNames.forEach(assetName => {
        const asset = assets[assetName];
        this.assets[assetName] = {
          name: assetName,
          source: asset.source()
        }
      });
      this.emitAssets();
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