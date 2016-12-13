webpackJsonp([1],{

/***/ 218:
/***/ function(module, exports, __webpack_require__) {

"use strict";
"use strict";

var _domready = __webpack_require__(96);

var _domready2 = _interopRequireDefault(_domready);

var _react = __webpack_require__(20);

var _reactDom = __webpack_require__(35);

var _App = __webpack_require__(33);

var _App2 = _interopRequireDefault(_App);

var _Router = __webpack_require__(34);

var _Router2 = _interopRequireDefault(_Router);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

(0, _domready2.default)(function () {

  var router = new _Router2.default();
  var model = window.currentProps;
  var Component = window.currentComponent.default;
  var appProps = {
    Component: Component,
    componentProps: {
      model: model
    },
    router: router
  };
  var element = (0, _react.createElement)(_App2.default, appProps);

  var container = document.getElementById('app');
  (0, _reactDom.render)(element, container);
});

/***/ },

/***/ 96:
/***/ function(module, exports, __webpack_require__) {

/*!
  * domready (c) Dustin Diaz 2014 - License MIT
  */
!function (name, definition) {

  if (true) module.exports = definition()
  else if (typeof define == 'function' && typeof define.amd == 'object') define(definition)
  else this[name] = definition()

}('domready', function () {

  var fns = [], listener
    , doc = document
    , hack = doc.documentElement.doScroll
    , domContentLoaded = 'DOMContentLoaded'
    , loaded = (hack ? /^loaded|^c/ : /^loaded|^i|^c/).test(doc.readyState)


  if (!loaded)
  doc.addEventListener(domContentLoaded, listener = function () {
    doc.removeEventListener(domContentLoaded, listener)
    loaded = 1
    while (listener = fns.shift()) listener()
  })

  return function (fn) {
    loaded ? setTimeout(fn, 0) : fns.push(fn)
  }

});


/***/ }

},[218]);