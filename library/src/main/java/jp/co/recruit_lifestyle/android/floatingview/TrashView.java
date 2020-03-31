/**
 * Copyright 2015 RECRUIT LIFESTYLE CO., LTD.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.recruit_lifestyle.android.floatingview;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import androidx.annotation.IntDef;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;

class TrashView extends FrameLayout implements ViewTreeObserver.OnPreDrawListener {

    private static final int BACKGROUND_HEIGHT = 164;

    private static final float TARGET_CAPTURE_HORIZONTAL_REGION = 30.0f;

    private static final float TARGET_CAPTURE_VERTICAL_REGION = 4.0f;

    private static final long TRASH_ICON_SCALE_DURATION_MILLIS = 200L;

    static final int ANIMATION_NONE = 0;
    static final int ANIMATION_OPEN = 1;
    static final int ANIMATION_CLOSE = 2;
    static final int ANIMATION_FORCE_CLOSE = 3;

    /**
     * Animation State
     */
    @IntDef({ANIMATION_NONE, ANIMATION_OPEN, ANIMATION_CLOSE, ANIMATION_FORCE_CLOSE})
    @Retention(RetentionPolicy.SOURCE)
    @interface AnimationState {
    }

    private static final int LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();

    private static final int OVERLAY_TYPE;

    private final WindowManager mWindowManager;

    private final WindowManager.LayoutParams mParams;

    private final DisplayMetrics mMetrics;

    private final ViewGroup mRootView;

    private final FrameLayout mTrashIconRootView;

    private final ImageView mFixedTrashIconView;

    private final ImageView mActionTrashIconView;

    private int mActionTrashIconBaseWidth;

    private int mActionTrashIconBaseHeight;

    private float mActionTrashIconMaxScale;

    private final FrameLayout mBackgroundView;

    private ObjectAnimator mEnterScaleAnimator;

    private ObjectAnimator mExitScaleAnimator;

    private final AnimationHandler mAnimationHandler;

    private TrashViewListener mTrashViewListener;

    private boolean mIsEnabled;

    static {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            OVERLAY_TYPE = WindowManager.LayoutParams.TYPE_PRIORITY_PHONE;
        } else {
            OVERLAY_TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
    }

    TrashView(Context context) {
        super(context);
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(mMetrics);
        mAnimationHandler = new AnimationHandler(this);
        mIsEnabled = true;

        mParams = new WindowManager.LayoutParams();
        mParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        mParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        mParams.type = OVERLAY_TYPE;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        mParams.format = PixelFormat.TRANSLUCENT;
        mParams.gravity = Gravity.LEFT | Gravity.BOTTOM;

        mRootView = new FrameLayout(context);
        mRootView.setClipChildren(false);
        mTrashIconRootView = new FrameLayout(context);
        mTrashIconRootView.setClipChildren(false);
        mFixedTrashIconView = new ImageView(context);
        mActionTrashIconView = new ImageView(context);
        mBackgroundView = new FrameLayout(context);
        mBackgroundView.setAlpha(0.0f);
        final GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x00000000, 0x50000000});
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            //noinspection deprecation
            mBackgroundView.setBackgroundDrawable(gradientDrawable);
        } else {
            mBackgroundView.setBackground(gradientDrawable);
        }

        final FrameLayout.LayoutParams backgroundParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (BACKGROUND_HEIGHT * mMetrics.density));
        backgroundParams.gravity = Gravity.BOTTOM;
        mRootView.addView(mBackgroundView, backgroundParams);
        final FrameLayout.LayoutParams actionTrashIconParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionTrashIconParams.gravity = Gravity.CENTER;
        mTrashIconRootView.addView(mActionTrashIconView, actionTrashIconParams);
        final FrameLayout.LayoutParams fixedTrashIconParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fixedTrashIconParams.gravity = Gravity.CENTER;
        mTrashIconRootView.addView(mFixedTrashIconView, fixedTrashIconParams);
        final FrameLayout.LayoutParams trashIconParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        trashIconParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        mRootView.addView(mTrashIconRootView, trashIconParams);

        addView(mRootView);

        getViewTreeObserver().addOnPreDrawListener(this);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateViewLayout();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateViewLayout();
    }

    @Override
    public boolean onPreDraw() {
        getViewTreeObserver().removeOnPreDrawListener(this);
        mTrashIconRootView.setTranslationY(mTrashIconRootView.getMeasuredHeight());
        return true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mTrashViewListener.onUpdateActionTrashIcon();
    }

    private void updateViewLayout() {
        mWindowManager.getDefaultDisplay().getMetrics(mMetrics);
        mParams.x = (mMetrics.widthPixels - getWidth()) / 2;
        mParams.y = 0;

        // Update view and layout
        mTrashViewListener.onUpdateActionTrashIcon();
        mAnimationHandler.onUpdateViewLayout();

        mWindowManager.updateViewLayout(this, mParams);
    }

    void dismiss() {
        mAnimationHandler.removeMessages(ANIMATION_OPEN);
        mAnimationHandler.removeMessages(ANIMATION_CLOSE);
        mAnimationHandler.sendAnimationMessage(ANIMATION_FORCE_CLOSE);
        setScaleTrashIconImmediately(false);
    }

    void getWindowDrawingRect(Rect outRect) {
        final ImageView iconView = hasActionTrashIcon() ? mActionTrashIconView : mFixedTrashIconView;
        final float iconPaddingLeft = iconView.getPaddingLeft();
        final float iconPaddingTop = iconView.getPaddingTop();
        final float iconWidth = iconView.getWidth() - iconPaddingLeft - iconView.getPaddingRight();
        final float iconHeight = iconView.getHeight() - iconPaddingTop - iconView.getPaddingBottom();
        final float x = mTrashIconRootView.getX() + iconPaddingLeft;
        final float y = mRootView.getHeight() - mTrashIconRootView.getY() - iconPaddingTop - iconHeight;
        final int left = (int) (x - TARGET_CAPTURE_HORIZONTAL_REGION * mMetrics.density);
        final int top = -mRootView.getHeight();
        final int right = (int) (x + iconWidth + TARGET_CAPTURE_HORIZONTAL_REGION * mMetrics.density);
        final int bottom = (int) (y + iconHeight + TARGET_CAPTURE_VERTICAL_REGION * mMetrics.density);
        outRect.set(left, top, right, bottom);
    }

    void updateActionTrashIcon(float width, float height, float shape) {
        if (!hasActionTrashIcon()) {
            return;
        }
        mAnimationHandler.mTargetWidth = width;
        mAnimationHandler.mTargetHeight = height;
        final float newWidthScale = width / mActionTrashIconBaseWidth * shape;
        final float newHeightScale = height / mActionTrashIconBaseHeight * shape;
        mActionTrashIconMaxScale = Math.max(newWidthScale, newHeightScale);
        mEnterScaleAnimator = ObjectAnimator.ofPropertyValuesHolder(mActionTrashIconView, PropertyValuesHolder.ofFloat(ImageView.SCALE_X, mActionTrashIconMaxScale), PropertyValuesHolder.ofFloat(ImageView.SCALE_Y, mActionTrashIconMaxScale));
        mEnterScaleAnimator.setInterpolator(new OvershootInterpolator());
        mEnterScaleAnimator.setDuration(TRASH_ICON_SCALE_DURATION_MILLIS);
        mExitScaleAnimator = ObjectAnimator.ofPropertyValuesHolder(mActionTrashIconView, PropertyValuesHolder.ofFloat(ImageView.SCALE_X, 1.0f), PropertyValuesHolder.ofFloat(ImageView.SCALE_Y, 1.0f));
        mExitScaleAnimator.setInterpolator(new OvershootInterpolator());
        mExitScaleAnimator.setDuration(TRASH_ICON_SCALE_DURATION_MILLIS);
    }

    float getTrashIconCenterX() {
        final ImageView iconView = hasActionTrashIcon() ? mActionTrashIconView : mFixedTrashIconView;
        final float iconViewPaddingLeft = iconView.getPaddingLeft();
        final float iconWidth = iconView.getWidth() - iconViewPaddingLeft - iconView.getPaddingRight();
        final float x = mTrashIconRootView.getX() + iconViewPaddingLeft;
        return x + iconWidth / 2;
    }

    float getTrashIconCenterY() {
        final ImageView iconView = hasActionTrashIcon() ? mActionTrashIconView : mFixedTrashIconView;
        final float iconViewHeight = iconView.getHeight();
        final float iconViewPaddingBottom = iconView.getPaddingBottom();
        final float iconHeight = iconViewHeight - iconView.getPaddingTop() - iconViewPaddingBottom;
        final float y = mRootView.getHeight() - mTrashIconRootView.getY() - iconViewHeight + iconViewPaddingBottom;
        return y + iconHeight / 2;
    }


    private boolean hasActionTrashIcon() {
        return mActionTrashIconBaseWidth != 0 && mActionTrashIconBaseHeight != 0;
    }

    void setFixedTrashIconImage(int resId) {
        mFixedTrashIconView.setImageResource(resId);
    }

    void setActionTrashIconImage(int resId) {
        mActionTrashIconView.setImageResource(resId);
        final Drawable drawable = mActionTrashIconView.getDrawable();
        if (drawable != null) {
            mActionTrashIconBaseWidth = drawable.getIntrinsicWidth();
            mActionTrashIconBaseHeight = drawable.getIntrinsicHeight();
        }
    }

    void setFixedTrashIconImage(Drawable drawable) {
        mFixedTrashIconView.setImageDrawable(drawable);
    }

    void setActionTrashIconImage(Drawable drawable) {
        mActionTrashIconView.setImageDrawable(drawable);
        if (drawable != null) {
            mActionTrashIconBaseWidth = drawable.getIntrinsicWidth();
            mActionTrashIconBaseHeight = drawable.getIntrinsicHeight();
        }
    }

    private void setScaleTrashIconImmediately(boolean isEnter) {
        cancelScaleTrashAnimation();

        mActionTrashIconView.setScaleX(isEnter ? mActionTrashIconMaxScale : 1.0f);
        mActionTrashIconView.setScaleY(isEnter ? mActionTrashIconMaxScale : 1.0f);
    }

    void setScaleTrashIcon(boolean isEnter) {
        if (!hasActionTrashIcon()) {
            return;
        }

        cancelScaleTrashAnimation();

        if (isEnter) {
            mEnterScaleAnimator.start();
        } else {
            mExitScaleAnimator.start();
        }
    }

    void setTrashEnabled(boolean enabled) {
        if (mIsEnabled == enabled) {
            return;
        }

        mIsEnabled = enabled;
        if (!mIsEnabled) {
            dismiss();
        }
    }

    boolean isTrashEnabled() {
        return mIsEnabled;
    }

    private void cancelScaleTrashAnimation() {
        if (mEnterScaleAnimator != null && mEnterScaleAnimator.isStarted()) {
            mEnterScaleAnimator.cancel();
        }

        if (mExitScaleAnimator != null && mExitScaleAnimator.isStarted()) {
            mExitScaleAnimator.cancel();
        }
    }

    void setTrashViewListener(TrashViewListener listener) {
        mTrashViewListener = listener;
    }

    WindowManager.LayoutParams getWindowLayoutParams() {
        return mParams;
    }

    void onTouchFloatingView(MotionEvent event, float x, float y) {
        final int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            mAnimationHandler.updateTargetPosition(x, y);
            mAnimationHandler.removeMessages(ANIMATION_CLOSE);
            mAnimationHandler.sendAnimationMessageDelayed(ANIMATION_OPEN, LONG_PRESS_TIMEOUT);
        }
        else if (action == MotionEvent.ACTION_MOVE) {
            mAnimationHandler.updateTargetPosition(x, y);
            if (!mAnimationHandler.isAnimationStarted(ANIMATION_OPEN)) {
                mAnimationHandler.removeMessages(ANIMATION_OPEN);
                mAnimationHandler.sendAnimationMessage(ANIMATION_OPEN);
            }
        }
        else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            mAnimationHandler.removeMessages(ANIMATION_OPEN);
            mAnimationHandler.sendAnimationMessage(ANIMATION_CLOSE);
        }
    }

    static class AnimationHandler extends Handler {

        private static final long ANIMATION_REFRESH_TIME_MILLIS = 10L;

        private static final long BACKGROUND_DURATION_MILLIS = 200L;

        private static final long TRASH_OPEN_START_DELAY_MILLIS = 200L;

        private static final long TRASH_OPEN_DURATION_MILLIS = 400L;

        private static final long TRASH_CLOSE_DURATION_MILLIS = 200L;

        private static final float OVERSHOOT_TENSION = 1.0f;

        private static final int TRASH_MOVE_LIMIT_OFFSET_X = 22;

        private static final int TRASH_MOVE_LIMIT_TOP_OFFSET = -4;

        private static final int TYPE_FIRST = 1;
        private static final int TYPE_UPDATE = 2;

        private static final float MAX_ALPHA = 1.0f;

        private static final float MIN_ALPHA = 0.0f;

        private long mStartTime;

        private float mStartAlpha;

        private float mStartTransitionY;

        private int mStartedCode;

        private float mTargetPositionX;

        private float mTargetPositionY;

        private float mTargetWidth;

        private float mTargetHeight;

        private final Rect mTrashIconLimitPosition;

        private float mMoveStickyYRange;

        private final OvershootInterpolator mOvershootInterpolator;

        private final WeakReference<TrashView> mTrashView;

        AnimationHandler(TrashView trashView) {
            mTrashView = new WeakReference<>(trashView);
            mStartedCode = ANIMATION_NONE;
            mTrashIconLimitPosition = new Rect();
            mOvershootInterpolator = new OvershootInterpolator(OVERSHOOT_TENSION);
        }

        @Override
        public void handleMessage(Message msg) {
            final TrashView trashView = mTrashView.get();
            if (trashView == null) {
                removeMessages(ANIMATION_OPEN);
                removeMessages(ANIMATION_CLOSE);
                removeMessages(ANIMATION_FORCE_CLOSE);
                return;
            }

            if (!trashView.isTrashEnabled()) {
                return;
            }

            final int animationCode = msg.what;
            final int animationType = msg.arg1;
            final FrameLayout backgroundView = trashView.mBackgroundView;
            final FrameLayout trashIconRootView = trashView.mTrashIconRootView;
            final TrashViewListener listener = trashView.mTrashViewListener;
            final float screenWidth = trashView.mMetrics.widthPixels;
            final float trashViewX = trashView.mParams.x;

            if (animationType == TYPE_FIRST) {
                mStartTime = SystemClock.uptimeMillis();
                mStartAlpha = backgroundView.getAlpha();
                mStartTransitionY = trashIconRootView.getTranslationY();
                mStartedCode = animationCode;
                if (listener != null) {
                    listener.onTrashAnimationStarted(mStartedCode);
                }
            }
            final float elapsedTime = SystemClock.uptimeMillis() - mStartTime;

            if (animationCode == ANIMATION_OPEN) {
                final float currentAlpha = backgroundView.getAlpha();
                if (currentAlpha < MAX_ALPHA) {
                    final float alphaTimeRate = Math.min(elapsedTime / BACKGROUND_DURATION_MILLIS, 1.0f);
                    final float alpha = Math.min(mStartAlpha + alphaTimeRate, MAX_ALPHA);
                    backgroundView.setAlpha(alpha);
                }

                if (elapsedTime >= TRASH_OPEN_START_DELAY_MILLIS) {
                    final float screenHeight = trashView.mMetrics.heightPixels;
                    final float positionX = trashViewX + (mTargetPositionX + mTargetWidth) / (screenWidth + mTargetWidth) * mTrashIconLimitPosition.width() + mTrashIconLimitPosition.left;
                    final float targetPositionYRate = Math.min(2 * (mTargetPositionY + mTargetHeight) / (screenHeight + mTargetHeight), 1.0f);
                    final float stickyPositionY = mMoveStickyYRange * targetPositionYRate + mTrashIconLimitPosition.height() - mMoveStickyYRange;
                    final float translationYTimeRate = Math.min((elapsedTime - TRASH_OPEN_START_DELAY_MILLIS) / TRASH_OPEN_DURATION_MILLIS, 1.0f);
                    final float positionY = mTrashIconLimitPosition.bottom - stickyPositionY * mOvershootInterpolator.getInterpolation(translationYTimeRate);
                    trashIconRootView.setTranslationX(positionX);
                    trashIconRootView.setTranslationY(positionY);
                    // clear drag view garbage
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        clearClippedChildren(trashView.mRootView);
                        clearClippedChildren(trashView.mTrashIconRootView);
                    }
                }

                sendMessageAtTime(newMessage(animationCode, TYPE_UPDATE), SystemClock.uptimeMillis() + ANIMATION_REFRESH_TIME_MILLIS);
            }
            else if (animationCode == ANIMATION_CLOSE) {
                final float alphaElapseTimeRate = Math.min(elapsedTime / BACKGROUND_DURATION_MILLIS, 1.0f);
                final float alpha = Math.max(mStartAlpha - alphaElapseTimeRate, MIN_ALPHA);
                backgroundView.setAlpha(alpha);

                final float translationYTimeRate = Math.min(elapsedTime / TRASH_CLOSE_DURATION_MILLIS, 1.0f);
                if (alphaElapseTimeRate < 1.0f || translationYTimeRate < 1.0f) {
                    final float position = mStartTransitionY + mTrashIconLimitPosition.height() * translationYTimeRate;
                    trashIconRootView.setTranslationY(position);
                    sendMessageAtTime(newMessage(animationCode, TYPE_UPDATE), SystemClock.uptimeMillis() + ANIMATION_REFRESH_TIME_MILLIS);
                } else {
                    trashIconRootView.setTranslationY(mTrashIconLimitPosition.bottom);
                    mStartedCode = ANIMATION_NONE;
                    if (listener != null) {
                        listener.onTrashAnimationEnd(ANIMATION_CLOSE);
                    }
                }
            }
            else if (animationCode == ANIMATION_FORCE_CLOSE) {
                backgroundView.setAlpha(0.0f);
                trashIconRootView.setTranslationY(mTrashIconLimitPosition.bottom);
                mStartedCode = ANIMATION_NONE;
                if (listener != null) {
                    listener.onTrashAnimationEnd(ANIMATION_FORCE_CLOSE);
                }
            }
        }

        /**
         * Clear the animation garbage of the target view.
         */
        private static void clearClippedChildren(ViewGroup viewGroup) {
            viewGroup.setClipChildren(true);
            viewGroup.invalidate();
            viewGroup.setClipChildren(false);
        }

        void sendAnimationMessageDelayed(int animation, long delayMillis) {
            sendMessageAtTime(newMessage(animation, TYPE_FIRST), SystemClock.uptimeMillis() + delayMillis);
        }

        /**
         * @param animation ANIMATION_OPEN,ANIMATION_CLOSE,ANIMATION_FORCE_CLOSE
         */
        void sendAnimationMessage(int animation) {
            sendMessage(newMessage(animation, TYPE_FIRST));
        }

        /**
         * @param animation ANIMATION_OPEN,ANIMATION_CLOSE,ANIMATION_FORCE_CLOSE
         * @param type      TYPE_FIRST,TYPE_UPDATE
         * @return Message
         */
        private static Message newMessage(int animation, int type) {
            final Message message = Message.obtain();
            message.what = animation;
            message.arg1 = type;
            return message;
        }

        boolean isAnimationStarted(int animationCode) {
            return mStartedCode == animationCode;
        }

        void updateTargetPosition(float x, float y) {
            mTargetPositionX = x;
            mTargetPositionY = y;
        }

        void onUpdateViewLayout() {
            final TrashView trashView = mTrashView.get();
            if (trashView == null) {
                return;
            }
            final float density = trashView.mMetrics.density;
            final float backgroundHeight = trashView.mBackgroundView.getMeasuredHeight();
            final float offsetX = TRASH_MOVE_LIMIT_OFFSET_X * density;
            final int trashIconHeight = trashView.mTrashIconRootView.getMeasuredHeight();
            final int left = (int) -offsetX;
            final int top = (int) ((trashIconHeight - backgroundHeight) / 2 - TRASH_MOVE_LIMIT_TOP_OFFSET * density);
            final int right = (int) offsetX;
            final int bottom = trashIconHeight;
            mTrashIconLimitPosition.set(left, top, right, bottom);

            mMoveStickyYRange = backgroundHeight * 0.20f;
        }
    }
}
