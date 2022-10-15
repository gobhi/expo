// Copyright 2022-present 650 Industries. All rights reserved.

import ExpoModulesCore

struct ImageSource: Record {
  @Field
  var width: Double?

  @Field
  var height: Double?

  @Field
  var uri: URL?

  @Field
  var scale: Double = 1.0

  @Field
  var headers: [String: String]?

  @Field
  var blurHash: ImageBlurHashOption?
}

struct ImageBlurHashOption: Record {
  @Field
  var hash: String

  @Field
  var width: Double = 32

  @Field
  var height: Double = 32

  @Field
  var punch: Float = 1.0

  var size: CGSize {
    return CGSize(width: width, height: height)
  }
}
