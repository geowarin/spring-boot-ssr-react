import React from 'react';

export default class home extends React.Component {

    render() {
        return <div>Hey tati, {this.props.model.message}</div>;
    }
}