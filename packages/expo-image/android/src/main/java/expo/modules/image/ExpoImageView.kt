package expo.modules.image

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.request.RequestOptions
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.modules.i18nmanager.I18nUtil
import com.facebook.react.uimanager.PixelUtil
import com.facebook.react.views.view.ReactViewBackgroundDrawable
import expo.modules.image.drawing.OutlineProvider
import expo.modules.image.enums.ImageResizeMode
import expo.modules.image.events.ImageLoadEventsManager
import expo.modules.image.okhttp.OkHttpClientProgressInterceptor
import expo.modules.image.records.ImageErrorEvent
import expo.modules.image.records.ImageLoadEvent
import expo.modules.image.records.ImageProgressEvent
import expo.modules.image.svg.SVGSoftwareLayerSetter
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.viewevent.EventDispatcher
import expo.modules.kotlin.views.ExpoView
import jp.wasabeef.glide.transformations.BlurTransformation
import java.lang.ref.WeakReference

private const val SOURCE_URI_KEY = "uri"
private const val SOURCE_WIDTH_KEY = "width"
private const val SOURCE_HEIGHT_KEY = "height"
private const val SOURCE_SCALE_KEY = "scale"

@SuppressLint("ViewConstructor")
class ExpoImageViewWrapper(context: Context, appContext: AppContext) : ExpoView(context, appContext) {
  internal val onLoadStart by EventDispatcher<Unit>()
  internal val onProgress by EventDispatcher<ImageProgressEvent>()
  internal val onError by EventDispatcher<ImageErrorEvent>()
  internal val onLoad by EventDispatcher<ImageLoadEvent>()

  private val imageView = ExpoImageView(
    appContext.reactContext?.applicationContext!!,
    getOrCreateRequestManager(appContext),
    ImageLoadEventsManager(
      WeakReference(this)
    )
  ).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    addView(this)
  }

  var sourceMap by imageView::sourceMap
  var resizeMode by imageView::resizeMode
  var blurRadius by imageView::blurRadius
  var fadeDuration by imageView::fadeDuration
  var defaultSourceMap by imageView::defaultSourceMap

  fun setBorderStyle(borderStyle: String?) = imageView.setBorderStyle(borderStyle)
  fun setTintColor(color: Int?) = imageView.setTintColor(color)
  fun onAfterUpdateTransaction() = imageView.onAfterUpdateTransaction()
  fun onDrop() = imageView.onDrop()
  fun setBorderRadius(index: Int, radius: Float) = imageView.setBorderRadius(index, radius)
  fun setBorderWidth(location: Int, width: Float) = imageView.setBorderWidth(location, width)
  fun setBorderColor(location: Int, rgbComponent: Float, alphaComponent: Float) = imageView.setBorderColor(location, rgbComponent, alphaComponent)

  companion object {
    private var requestManager: RequestManager? = null
    private var appContextRef: WeakReference<AppContext?> = WeakReference(null)

    fun getOrCreateRequestManager(appContext: AppContext): RequestManager = synchronized(Companion) {
      val cachedRequestManager = requestManager
        ?: return createNewRequestManager(appContext).also {
          requestManager = it
          appContextRef = WeakReference(appContext)
        }

      // Request manager was created using different app context
      if (appContextRef.get() != appContext) {
        return createNewRequestManager(appContext).also {
          requestManager = it
          appContextRef = WeakReference(appContext)
        }
      }

      return cachedRequestManager
    }

    private fun createNewRequestManager(appContext: AppContext): RequestManager =
      Glide.with(requireNotNull(appContext.reactContext)).addDefaultRequestListener(SVGSoftwareLayerSetter())
  }
}

@SuppressLint("ViewConstructor")
class ExpoImageView(
  context: Context,
  private val requestManager: RequestManager,
  private val eventsManager: ImageLoadEventsManager
) : AppCompatImageView(context) {
  private val progressInterceptor = OkHttpClientProgressInterceptor

  private val outlineProvider = OutlineProvider(context)

  private var propsChanged = false
  private var loadedSource: GlideUrl? = null

  private val borderDrawable = lazy {
    ReactViewBackgroundDrawable(context).apply {
      callback = this@ExpoImageView

      outlineProvider.borderRadiiConfig
        .map { it.ifYogaDefinedUse(PixelUtil::toPixelFromDIP) }
        .withIndex()
        .forEach { (i, radius) ->
          if (i == 0) {
            setRadius(radius)
          } else {
            setRadius(radius, i - 1)
          }
        }
    }
  }

  init {
    clipToOutline = true
    super.setOutlineProvider(outlineProvider)
  }

  // region Component Props
  internal var sourceMap: ReadableMap? = null
  internal var defaultSourceMap: ReadableMap? = null
  internal var blurRadius: Int? = null
    set(value) {
      field = value?.takeIf { it > 0 }
      propsChanged = true
    }
  internal var fadeDuration: Int? = null
    set(value) {
      field = value?.takeIf { it > 0 }
      propsChanged = true
    }
  internal var resizeMode = ImageResizeMode.COVER.also { scaleType = it.scaleType }
    set(value) {
      field = value
      scaleType = value.scaleType
    }

  internal fun setBorderRadius(position: Int, borderRadius: Float) {
    var radius = borderRadius
    val isInvalidated = outlineProvider.setBorderRadius(radius, position)
    if (isInvalidated) {
      invalidateOutline()
      if (!outlineProvider.hasEqualCorners()) {
        invalidate()
      }
    }

    // Setting the border-radius doesn't necessarily mean that a border
    // should to be drawn. Only update the border-drawable when needed.
    if (borderDrawable.isInitialized()) {
      radius = radius.ifYogaDefinedUse(PixelUtil::toPixelFromDIP)
      borderDrawable.value.apply {
        if (position == 0) {
          setRadius(radius)
        } else {
          setRadius(radius, position - 1)
        }
      }
    }
  }

  internal fun setBorderWidth(position: Int, width: Float) {
    borderDrawable.value.setBorderWidth(position, width)
  }

  internal fun setBorderColor(position: Int, rgb: Float, alpha: Float) {
    borderDrawable.value.setBorderColor(position, rgb, alpha)
  }

  internal fun setBorderStyle(style: String?) {
    borderDrawable.value.setBorderStyle(style)
  }

  internal fun setTintColor(color: Int?) {
    color?.let { setColorFilter(it, PorterDuff.Mode.SRC_IN) } ?: clearColorFilter()
  }
  // endregion

  // region ViewManager Lifecycle methods
  internal fun onAfterUpdateTransaction() {
    val sourceToLoad = createUrlFromSourceMap(sourceMap)
    val defaultSourceToLoad = createUrlFromSourceMap(defaultSourceMap)
    if (sourceToLoad == null) {
      requestManager.clear(this)
      setImageDrawable(null)
      loadedSource = null
    } else if (sourceToLoad != loadedSource || propsChanged) {
      propsChanged = false
      loadedSource = sourceToLoad
      val options = createOptionsFromSourceMap(sourceMap)
      val propOptions = createPropOptions()
      progressInterceptor.registerProgressListener(sourceToLoad.toStringUrl(), eventsManager)
      eventsManager.onLoadStarted()
      requestManager
        .asDrawable()
        .load(sourceToLoad)
        .apply { if (defaultSourceToLoad != null) thumbnail(requestManager.load(defaultSourceToLoad)) }
        .apply(options)
        .addListener(eventsManager)
        .run {
          val fitCenter = FitCenter()
          this
            .optionalTransform(fitCenter)
            .optionalTransform(WebpDrawable::class.java, WebpDrawableTransformation(fitCenter))
        }
        .apply(propOptions)
        .into(this)

      requestManager
        .`as`(BitmapFactory.Options::class.java)
        // Remove any default listeners from this request
        // (an example would be an SVGSoftwareLayerSetter
        // added in ExpoImageViewManager).
        // This request won't load the image, only the size.
        .listener(null)
        .load(sourceToLoad)
        .into(eventsManager)
    }
  }

  internal fun onDrop() {
    requestManager.clear(this)
  }
  // endregion

  // region Helper methods
  private fun createUrlFromSourceMap(sourceMap: ReadableMap?): GlideUrl? {
    val uriKey = sourceMap?.getString(SOURCE_URI_KEY)
    return uriKey?.let { GlideUrl(uriKey) }
  }

  private fun createOptionsFromSourceMap(sourceMap: ReadableMap?): RequestOptions {
    return RequestOptions()
      .apply {
        // Override the size for local assets. This ensures that
        // resizeMode "center" displays the image in the correct size.
        if (sourceMap != null &&
          sourceMap.hasKey(SOURCE_WIDTH_KEY) &&
          sourceMap.hasKey(SOURCE_HEIGHT_KEY) &&
          sourceMap.hasKey(SOURCE_SCALE_KEY)
        ) {
          val scale = sourceMap.getDouble(SOURCE_SCALE_KEY)
          val width = sourceMap.getInt(SOURCE_WIDTH_KEY)
          val height = sourceMap.getInt(SOURCE_HEIGHT_KEY)
          override((width * scale).toInt(), (height * scale).toInt())
        }
      }
  }

  private fun createPropOptions(): RequestOptions {
    return RequestOptions()
      .apply {
        blurRadius?.let {
          transform(BlurTransformation(it + 1, 4))
        }
        fadeDuration?.let {
          alpha = 0f
          animate().alpha(1f).duration = it.toLong()
        }
      }
  }
  // endregion

  // region Drawing overrides
  override fun invalidateDrawable(drawable: Drawable) {
    super.invalidateDrawable(drawable)
    if (borderDrawable.isInitialized() && drawable === borderDrawable.value) {
      invalidate()
    }
  }

  override fun draw(canvas: Canvas) {
    // When the border-radii are not all the same, a convex-path
    // is used for the Outline. Unfortunately clipping is not supported
    // for convex-paths and we fallback to Canvas clipping.
    outlineProvider.clipCanvasIfNeeded(canvas, this)
    super.draw(canvas)
  }

  public override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    // Draw borders on top of the background and image
    if (borderDrawable.isInitialized()) {
      borderDrawable.value.apply {
        val layoutDirection = if (I18nUtil.getInstance().isRTL(context)) LAYOUT_DIRECTION_RTL else LAYOUT_DIRECTION_LTR
        resolvedLayoutDirection = layoutDirection
        setBounds(0, 0, width, height)
        draw(canvas)
      }
    }
  }

  /**
   * Called when Glide "injects" drawable into the view.
   * When `resizeMode = REPEAT`, we need to update
   * received drawable (unless null) and set correct tiling.
   */
  override fun setImageDrawable(drawable: Drawable?) {
    val maybeUpdatedDrawable = drawable
      ?.takeIf { resizeMode == ImageResizeMode.REPEAT }
      ?.toBitmapDrawable(resources)
      ?.apply {
        setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
      }
    super.setImageDrawable(maybeUpdatedDrawable ?: drawable)
  }
  // endregion
}
