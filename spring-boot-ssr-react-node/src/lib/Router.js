import {parse, format} from 'url';
import axios from 'axios';
import loadScript from 'little-loader';

export default class Router {
  constructor(url = window.location.href) {

    this.currentUrl = parse(url, true);
    this.subscriptions = new Set();
    this.onPopState = this.onPopState.bind(this);

    if (typeof window.location !== 'undefined') {
      window.addEventListener('popstate', this.onPopState)
    }
  }

  onPopState(e) {
    const currentUrl = getURL();
    this.change(null, currentUrl);
  }

  loadModel(url) {
    const modelData = axios.get(url, {
      data: {} // sets content-type to 'application/json'
    });
    return modelData.then(r => r.data);
  }

  loadPage(url) {
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

  change(historyMethod, url) {
    this.loadModel(url).then(modelAndScript => {
      const {model, script} = modelAndScript;
      this.loadPage(script).then(() => {

        if (historyMethod) {
          window.history[historyMethod]({}, null, url);
        }

        this.notifyListeners({
          componentProps: {model},
          Component: window.currentComponent.default
        });
      });
    });
  }

  back() {
    window.history.back()
  }

  push(url) {
    return this.change('pushState', url)
  }

  replace(url) {
    return this.change('replaceState', url)
  }

  notifyListeners(newProps) {
    const newUrl = parse(window.location.href, true);

    if (this.urlIsNew(newUrl)) {
      this.currentUrl = newUrl;
      this.notify(newProps)
    }
  }

  urlIsNew({pathname, query}) {
    return this.currentUrl.pathname !== pathname || !shallowEquals(query, this.currentUrl.query)
  }

  notify(data) {
    this.subscriptions.forEach(cb => cb(data))
  }

  subscribe(fn) {
    this.subscriptions.add(fn);
    return () => this.subscriptions.delete(fn);
  }
}

function getURL() {
  return window.location.pathname + (window.location.search || '') + (window.location.hash || '')
}

function shallowEquals(a, b) {
  for (const i in a) {
    if (b[i] !== a[i]) return false
  }

  for (const i in b) {
    if (b[i] !== a[i]) return false
  }

  return true
}
