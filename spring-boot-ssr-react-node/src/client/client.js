import domReady from "domready";
import {createElement} from "react";
import {render} from "react-dom";
import App from "../lib/App";
import Router from '../lib/Router';

domReady(() => {

  const router = new Router();
  const model = window.currentProps;
  const Component = window.currentComponent.default;
  const appProps = {
    Component,
    componentProps: {
      model
    },
    router
  };
  const element = createElement(App, appProps);

  const container = document.getElementById('app');
  render(element, container)
});