import React, {Component, PropTypes} from "react";
// import { AppContainer } from 'react-hot-loader'

export default class App extends Component {
  constructor(props) {
    super(props);
    this.state = propsToState(props);
    this.cleanup = null;
  }

  componentDidMount () {
    const { router } = this.props;

    this.cleanup = router.subscribe(newProps => {
      try {
        const state = propsToState({...newProps, router: this.props.router});
        this.setState(state);
      } catch (err) {
        console.error(err)
      }
    })
  }

  componentWillUnmount () {
    if (this.cleanup) this.cleanup()
  }

  componentWillReceiveProps(nextProps) {
    const state = propsToState(nextProps);
    try {
      this.setState(state);
    } catch (err) {
      console.error(err);
    }
  }

  render() {
    const {Component, componentProps, router} = this.state;

    return (
      <Component {...componentProps} router={router} />
    );
  }
}

function propsToState(props) {
  const {Component, componentProps, router} = props;

  return {
    Component,
    componentProps,
    router
  }
}
