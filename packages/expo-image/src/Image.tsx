import React from 'react';

import ExpoImageView from './ExpoImage';
import { ImageProps } from './Image.types';

export default class Image extends React.Component<ImageProps, any> {
  render(): React.ReactNode {
    return <ExpoImageView {...this.props} />;
  }
}
