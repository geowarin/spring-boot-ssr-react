"use strict";

var async = require('async');
var createDebug = require('debug');
var EventEmitter = require('events');
var Int64 = require('node-int64');
var fs = require('fs');
var path = require('path');
var fsAccurency = require('./utils/fsAccurency');

const debug = createDebug('watchman:connector');

/*
 type Options = { aggregateTimeout: number, projectPath: string};

 type WatchmanResponse = {
 clock: string,
 subscription: string,
 files: Array<{ name: string, mtime_ms: number, 'new': boolean, exists: boolean }>
 };
 */

class SaneWatcher extends EventEmitter {

   /*
   // @type {Array<string>}
  aggregatedChanges = [];
  connected = false;
  // string
  lastClock;

  fileTimes = {};
  options;
  paused = true;
  timeoutRef = 0;
  initialScan = true;
  initialScanRemoved = false;
   // @type {Set} Set<{ name: string, mtime: number }>
  initialScanQueue = new Set();
*/
  //: Options = { aggregateTimeout: 200, projectPath: '' }
  constructor(options) {
    super();
    if (!options.projectPath) throw new Error('projectPath is missing for WatchmanPlugin');

    this.options = options;

    this.aggregatedChanges = [];
    this.connected = false;
    this.fileTimes = {};
    this.paused = true;
    this.timeoutRef = 0;
    this.initialScan = true;
    this.initialScanRemoved = false;
    this.initialScanQueue = new Set();
  }

  /**
   * @param files:Array<string>
   * @param dirs:Array<string>
   * @param since:string|number
   * @param done?: () => void
   *
   * `since` has to be either a string with a watchman clock value, or a number
   * which is then treated as a timestamp in milliseconds
   */
  watch(files, dirs, since, done) {
    debug(`watch() called, current connection status: ${this.connected ? 'connected' : 'disconnected'}`);
    this.paused = false;

    if (this.connected) return;

    const allFiles = files.concat(dirs);

    Promise.all([
      new Promise((resolve, reject) => {
        this._startWatch(allFiles, since, err => (err ? reject(err) : resolve()));
      }),
      new Promise((resolve) => {
        this._doInitialScan(allFiles, resolve);
      }),
    ])
      .catch((err) => {
        throw err;
      })
      .then(() => (done ? done() : null));
  }

  /**
   * { [key: string]: number }
   * @return
   */
  getTimes() {
    return this.fileTimes;
  }

  close() {
    debug('close() called');
    this.paused = true;
    if (this.timeoutRef) clearTimeout(this.timeoutRef);
    this.removeAllListeners();

    if (this.watcher) {
      this.watcher.close();
      this.watcher = null;
    }
  }

  pause() {
    debug('pause() called');
    this.paused = true;
    if (this.timeoutRef) clearTimeout(this.timeoutRef);
  }

  /**
   * @param files: Array<string>
   * @param since: string|number
   * @param done: () => void
   * @private
   */
  _startWatch(files, since, done) {

    this.watcher = sane(this.options.projectPath, {glob: ['**/*.js', '**/*.css']});

    this.watcher.on('ready', function () {
      console.log('ready');
      done();
    });
    this.watcher.on('change', function (filepath, root, stat) {
      console.log('file changed', filepath);
    });
    this.watcher.on('add', function (filepath, root, stat) {
      console.log('file added', filepath);
    });
    this.watcher.on('delete', function (filepath, root) {
      console.log('file deleted', filepath);
    });


    // const client = this._getClientInstance();
    //
    // client.capabilityCheck({ optional: [], required: ['cmd-watch-project', 'relative_root'] },
    //   (capabilityErr) => {
    //     /* istanbul ignore if: cannot happen in tests */
    //     if (capabilityErr) {
    //       done(capabilityErr);
    //       return;
    //     }
    //     debug('watchman capabilityCheck() successful');
    //
    //     // Initiate the watch
    //     client.command(['watch-project', this.options.projectPath],
    //       (watchError, watchResponse) => {
    //         /* istanbul ignore if: cannot happen in tests */
    //         if (watchError) {
    //           done(watchError);
    //           return;
    //         }
    //         debug('watchman command watch-project successful');
    //
    //         /* istanbul ignore if: cannot happen in tests */
    //         if (watchResponse.warning) {
    //           console.warn('warning: ', watchResponse.warning); // eslint-disable-line no-console
    //         }
    //
    //         const sub = {
    //           expression: [
    //             'allof',
    //             [
    //               'name',
    //               files.map(file => path.relative(this.options.projectPath, file)),
    //               'wholename',
    //             ],
    //           ],
    //           fields: ['name', 'mtime_ms', 'exists'],
    //           since: typeof since === 'string' ? since : new Int64(Math.floor(since / 1000)),
    //           relative_root: watchResponse.relative_path,
    //         };
    //
    //         client.on('subscription', this._onSubscription);
    //
    //         debug('watchman command subscription data: ', sub);
    //
    //         client.command(['subscribe', watchResponse.watch, 'webpack_subscription', sub],
    //           (subscribeError) => {
    //             /* istanbul ignore if: cannot happen in testsn */
    //             if (subscribeError) {
    //               done(subscribeError);
    //               return;
    //             }
    //             debug('watchman command subscribe successful');
    //             done();
    //           });
    //       },
    //     );
    //   },
    // );
  }

  /**
   * @param resp: WatchmanResponse
   * @private
   */
  _onSubscription(resp) {
    debug('received subscription: %O', resp);
    if (resp.subscription === 'webpack_subscription') {
      this.lastClock = resp.clock;
      resp.files.forEach((file) => {
        const filePath = path.join(this.options.projectPath, file.name);
        const mtime = (!file.exists) ? null : +file.mtime_ms;

        this._setFileTime(filePath, mtime);

        if (this.initialScan) {
          if (mtime) {
            this.initialScanQueue.add({name: filePath, mtime});
          } else {
            this.initialScanRemoved = true;
          }
          return;
        }

        if (this.paused || !file.exists) return;

        this._handleEvents(filePath, mtime);
      });
    }
  };

  /**
   * @param file: string
   * @param mtime: ?number
   * @private
   */
  _setFileTime(file, mtime) {
    this.fileTimes[file] = mtime + fsAccurency.get();
  }

  /**
   * @param filePath: string
   * @param mtime: ?number
   * @private
   */
  _handleEvents(filePath, mtime) {
    this.emit('change', filePath, mtime);

    this._handleAggregated(filePath);
  }

  /**
   * @param file: string
   * @private
   */
  _handleAggregated(file) {
    if (this.timeoutRef) clearTimeout(this.timeoutRef);

    if (this.aggregatedChanges.indexOf(file) < 0) {
      this.aggregatedChanges.push(file);
    }

    this.timeoutRef = setTimeout(this._onTimeout, this.options.aggregateTimeout);
  }

  /**
   *
   * @return {Client}
   * @private
   */
  _getClientInstance() {
    if (!this.client) {
      const client = new Client();
      client.on('connect', () => {
        this.connected = true;
      });
      client.on('end', () => {
        this.connected = false;
      });

      this.client = client;
    }

    return this.client;
  }

  _onTimeout() {
    this.timeoutRef = 0;
    const changes = this.aggregatedChanges;
    this.aggregatedChanges = [];

    this.emit('aggregated', changes, this.lastClock);
  };

  /**
   * @param files: Array<string>
   * @param done: () => void
   * @private
   */
  _doInitialScan(files, done) {
    debug('starting initial file scan');
    async.eachLimit(files, 500, (file, callback) => {
      fs.stat(file, (err, stat) => {
        if (err) {
          callback(err);
          return;
        }

        const mtime = +stat.mtime;
        fsAccurency.revalidate(mtime);

        this._setFileTime(file, mtime);
        callback();
      });
    }, () => {
      this.initialScan = false;
      debug('initial file scan finished');

      if (this.initialScanQueue.size > 0) {
        const file = Array.from(this.initialScanQueue)[this.initialScanQueue.size - 1];
        this._handleEvents(file.name, file.mtime);
      }

      if (this.initialScanRemoved) {
        this.initialScanRemoved = false;
        this.emit('remove');
      }

      this.initialScanQueue.clear();
      done();
    });
  }
}

module.exports = SaneWatcher;
