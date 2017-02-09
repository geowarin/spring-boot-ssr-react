import React from "react";
import App from "../lib/App";
import router, {setUrl} from '../lib/Router';
import {renderToString} from "react-dom/server";

export default function render(Component, model, url) {
  setUrl(url);
  const props = {
    Component,
    componentProps: {
      model
    },
    router
  };
  const Root = React.createElement(App, props);
  return renderToString(Root);
}