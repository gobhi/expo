import Image, {
  ImageProps,
  ImageResizeMode,
  ImageSource,
  ImageTransition,
  ImageTransitionEffect,
  ImageTransitionTiming,
} from 'expo-image';
import { useCallback, useState } from 'react';
import { StyleSheet, View, Dimensions, ScrollView } from 'react-native';

import Button from '../../components/Button';
import { FunctionParameter } from '../../components/FunctionDemo';
import Configurator from '../../components/FunctionDemo/Configurator';
import { useArguments } from '../../components/FunctionDemo/FunctionDemo';
import { Colors } from '../../constants';
import images from './images/images';

const parameters: FunctionParameter[] = [
  {
    name: 'source',
    type: 'enum',
    values: [
      { name: 'random', value: 'random' },
      { name: 'PNG (network)', value: images.uri_png },
      { name: 'JPG (network)', value: images.uri_jpg },
      { name: 'GIF (network)', value: images.uri_gif },
      { name: 'ICO (network)', value: images.uri_ico },
    ],
  },
  {
    name: 'resizeMode',
    type: 'enum',
    values: [
      { name: 'cover', value: ImageResizeMode.COVER },
      { name: 'contain', value: ImageResizeMode.CONTAIN },
      { name: 'stretch', value: ImageResizeMode.STRETCH },
      { name: 'repeat', value: ImageResizeMode.REPEAT },
      { name: 'center', value: ImageResizeMode.CENTER },
    ],
  },
  {
    name: 'transition',
    type: 'object',
    properties: [
      {
        name: 'duration',
        type: 'number',
        values: [0.2, 1, 5, 0],
      },
      {
        name: 'timing',
        type: 'enum',
        values: [
          { name: 'default', value: undefined },
          { name: 'Ease in out', value: ImageTransitionTiming.EASE_IN_OUT },
          { name: 'Ease in', value: ImageTransitionTiming.EASE_IN },
          { name: 'Ease out', value: ImageTransitionTiming.EASE_OUT },
          { name: 'Linear', value: ImageTransitionTiming.LINEAR },
        ],
      },
      {
        name: 'effect',
        type: 'enum',
        values: [
          { name: 'Cross disolve', value: ImageTransitionEffect.CROSS_DISOLVE },
          { name: 'Flip from left', value: ImageTransitionEffect.FLIP_FROM_LEFT },
          { name: 'Flip from right', value: ImageTransitionEffect.FLIP_FROM_RIGHT },
          { name: 'Flip from top', value: ImageTransitionEffect.FLIP_FROM_TOP },
          { name: 'Flip from bottom', value: ImageTransitionEffect.FLIP_FROM_BOTTOM },
          { name: 'Curl up', value: ImageTransitionEffect.CURL_UP },
          { name: 'Curl down', value: ImageTransitionEffect.CURL_DOWN },
        ],
      },
    ],
  },
  {
    name: 'style',
    type: 'object',
    properties: [
      {
        name: 'borderRadius',
        type: 'number',
        values: [0, 30, 500],
      },
      {
        name: 'borderWidth',
        type: 'number',
        values: [0, 10],
      },
    ],
  },
  {
    name: 'blurRadius',
    type: 'number',
    values: [0, 3, 10, 100],
  },
  {
    name: 'tintColor',
    type: 'enum',
    values: [
      { name: 'none', value: 'transparent' },
      { name: 'navy', value: 'navy' },
      { name: 'papayawhip', value: 'papayawhip' },
      { name: 'tomato', value: 'tomato' },
    ],
  },
];

export default function ImageScreen() {
  const [args, updateArgument] = useArguments(parameters);
  const [randomId, setRandomId] = useState(randomInt());
  const [sourceArg, resizeMode, transition, style, blurRadius, tintColor] = args as [
    ImageSource,
    ImageResizeMode,
    ImageTransition,
    any,
    number,
    string
  ];
  const source =
    sourceArg === 'random'
      ? {
          uri: `https://source.unsplash.com/random/featured?animal&r=${randomId}`,
        }
      : sourceArg;

  const onError = useCallback<ImageProps['onError']>((event) => {
    console.log('Image loading error:', event.nativeEvent);
  }, []);

  return (
    <ScrollView contentContainerStyle={styles.scrollContent}>
      <View style={styles.configurator}>
        <Configurator parameters={parameters} onChange={updateArgument} value={args} />
      </View>
      <View style={styles.imageContainer}>
        <Image
          style={[styles.image, style]}
          source={source}
          resizeMode={resizeMode}
          transition={transition}
          blurRadius={blurRadius}
          tintColor={tintColor}
          onError={onError}
        />
      </View>
      <Button title="Random image" onPress={() => setRandomId(randomInt())} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  scrollContent: {
    margin: 10,
  },
  configurator: {
    paddingHorizontal: 20,
  },
  imageContainer: {
    alignItems: 'center',
    margin: 20,
  },
  image: {
    width: Dimensions.get('window').width - 40,
    height: 420,
    borderColor: Colors.tintColor,
  },
});

function randomInt(): number {
  return Math.floor(Math.random() * 1000);
}
