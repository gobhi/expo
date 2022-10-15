import React from 'react';
import { Image as RNImage } from 'react-native';

import ExpoImageView from './ExpoImage';
import { ImageProps } from './Image.types';

export default class Image extends React.Component<ImageProps, any> {
  render(): React.ReactNode {
    const source = RNImage.resolveAssetSource(this.props.source);

    return <ExpoImageView {...this.props} source={source} />;
  }
}
