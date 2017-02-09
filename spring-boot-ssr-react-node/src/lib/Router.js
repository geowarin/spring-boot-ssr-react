import {parse, format} from "url";
import axios from "axios";
import loadScript from "little-loader";
import {createBrowserHistory} from 'history';

let currentUrl = "";

class BaseRouter {
  back() {
  }

  get location() {
    return parse(currentUrl);
  }

  push(url) {
  }

  replace(url) {
  }

  subscribe(fn) {
  }
}

class BrowserRouter {
  constructor() {
    this.history = createBrowserHistory();
    this.history.listen((location, action) => {
      this._navigate(location.pathname);
    });
    this.subscriptions = new Set();
  }

  get location() {
    return this.history.location;
  }

  back() {
    this.history.goBack();
  }

  push(...args) {
    this.history.push(...args)
  }

  replace(...args) {
    this.history.replace(...args)
  }

  subscribe(fn) {
    this.subscriptions.add(fn);
    return () => this.subscriptions.delete(fn);
  }

  async _navigate(url) {

    const {model, script} = await loadModel(url);
    await loadPage(script);
    this._notifyListeners({
      componentProps: {model},
      Component: window.currentComponent.default
    });
    return model;
  }

  _notifyListeners(data) {
    this.subscriptions.forEach(listener => listener(data))
  }
}

function loadPage(url) {
  const parsed = parse(url, true);
  parsed.query['modulePath'] = 'window.currentComponent';
  const scriptUrl = format(parsed);

  return new Promise((resolve, reject) => {
    loadScript(scriptUrl, err => {
      if (err) {
        reject(err);
      } else {
        resolve();
      }
    })
  })
}

function loadModel(url) {
  const modelData = axios.get(url, {
    data: {} // sets content-type to 'application/json'
  });
  return modelData.then(r => r.data);
}

export function setUrl(url) {
  currentUrl = url;
}
export default !!(typeof window !== 'undefined' && window.document && window.document.createElement) ? new BrowserRouter() : new BaseRouter();
