import React from 'react';
import { Image as RNImage } from 'react-native';
import ExpoImageView from './ExpoImage';
export default class Image extends React.Component {
    render() {
        const source = RNImage.resolveAssetSource(this.props.source);
        return React.createElement(ExpoImageView, { ...this.props, source: source });
    }
}
//# sourceMappingURL=Image.js.map