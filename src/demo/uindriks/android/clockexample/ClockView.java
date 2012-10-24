/**
 *
 */

package demo.uindriks.android.clockexample;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.BounceInterpolator;

import java.util.Date;

/**
 * @author uldis.indriksons
 */
public class ClockView extends View {

    private static final String TAG = "ClockView";

    private static final int STROKE_WIDTH = 5;

    private Bitmap mBackground;

    private int mSec;

    private float mMinute;

    private float mHour;

    private final Paint mSecPaint;

    private final Paint mMinutePaint;

    private final Paint mHourPaint;

    private int mRadius;

    private int mCx;

    private int mCy;

    private final int mNumerals;

    private final int mFaceColor;

    private static final String[] ROMAN_NUMBERS = new String[] {
            "XII", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI"
    };


    // animation variables

    // animation interpolator
    private final BounceInterpolator mInterpolator;
    // stores time when animation was started
    private long mAnimationStartTime;
    // in frames per second
    private static final int SECONDS_ANIMATION_REFRESH_RATE = 1000 / 32;

    // stores time of current frame, 1 means animation has ended 0 just started
    private float mAnimationTimeDelta;
    private float mAnimationPosition;

    public ClockView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // extract custom attribute values
        TypedArray clockAttrs = context.obtainStyledAttributes(attrs, R.styleable.ClockView);
        mNumerals = clockAttrs.getInt(R.styleable.ClockView_numerals, 0);
        mFaceColor = clockAttrs.getColor(R.styleable.ClockView_faceColor, Color.TRANSPARENT);
        clockAttrs.recycle();

        // construct Paint objects for clock hands drawing
        mSecPaint = createHandPaint(Color.RED, 3);
        mMinutePaint = createHandPaint(Color.YELLOW, 7);
        mHourPaint = createHandPaint(Color.YELLOW, 12);

        // construct interpolator
        mInterpolator = new BounceInterpolator();

    }

    /**
     * Sets up time units depending on System time.
     */
    private boolean changeTime() {
        Date time = new Date();
        int sec = time.getSeconds();
        if (sec != mSec) {
            // if time has changed, invalidates view
            mSec = sec;
            mMinute = time.getMinutes() + mSec / 60f;
            mHour = time.getHours() % 12 * 5 + mMinute / 12f;
            Log.d(TAG, "" + mHour + ":" + mMinute + ":" + mSec);
            return true;
        }

        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (mBackground == null) {
            // create clock face bitmap
            // it will be used as cached resource and will save some
            // milliseconds on redraw
            mBackground = Bitmap.createBitmap(getWidth(), getHeight(), Config.ARGB_8888);
            Canvas bmpCanvas = new Canvas(mBackground);

            drawCircle(bmpCanvas);
            drawHours(bmpCanvas);
        }

        // draw clock face
        canvas.drawBitmap(mBackground, 0, 0, null);

        // draw clock hands starting with hour hand and ending with second hand
        // ;)
        Point endPoint = getPoint(mHour, (int) (mRadius * 0.6), mCx, mCy);
        canvas.drawLine(mCx, mCy, endPoint.x, endPoint.y, mHourPaint);

        endPoint = getPoint(mMinute, (int) (mRadius * 0.8), mCx, mCy);
        canvas.drawLine(mCx, mCy, endPoint.x, endPoint.y, mMinutePaint);

        // since mSec stores the target position decrement it by 1 and add animation
        // interpolation
        endPoint = getPoint((mSec - 1) + mAnimationPosition, (int) (mRadius * 0.9), mCx, mCy);
        canvas.drawLine(mCx, mCy, endPoint.x, endPoint.y, mSecPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.d(TAG, "widthSpec: " + MeasureSpec.toString(widthMeasureSpec));
        Log.d(TAG, "heightSpec: " + MeasureSpec.toString(heightMeasureSpec));

        // get measure size for both dimensions
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        // get measure mode for both dimensions
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        // here comes complex conditions logic to find out best width and height measurements
        if (widthMode == MeasureSpec.UNSPECIFIED) {
            width = getSuggestedMinimumWidth();

            if (width == 0) {
                width = height;
            } else {
                if (heightMode == MeasureSpec.AT_MOST) {
                    height = width;
                }
            }
        }

        if (heightMode == MeasureSpec.UNSPECIFIED) {
            height = getSuggestedMinimumWidth();

            if (height == 0) {
                height = width;
            } else {
                if (widthMode == MeasureSpec.AT_MOST) {
                    width = height;
                }
            }
        }

        if (widthMode == MeasureSpec.EXACTLY) {
            if (heightMode == MeasureSpec.UNSPECIFIED) {
                height = width;
            } else if (heightMode == MeasureSpec.AT_MOST) {
                height = Math.min(width, height);
            }
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            if (widthMode == MeasureSpec.UNSPECIFIED) {
                width = height;
            } else if (widthMode == MeasureSpec.AT_MOST) {
                width = Math.min(width, height);
            }
        }

        // our view wants to be a quadrate
        // if width or height mode isn't specified exactly, let's be a quadrate,
        // make width and height measurement the same
        if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(width, height);
        }

        if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(width, height);
        }

        // calculate clock radius and center coordinates
        mRadius = Math.min(width, height) / 2 - STROKE_WIDTH;
        mCx = width / 2;
        mCy = height / 2;

        setMeasuredDimension(width, height);
    }

    /**
     * Draws clock face ring.
     *
     * @param canvas
     */
    private void drawCircle(Canvas canvas) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Style.FILL);
        paint.setColor(mFaceColor);
        canvas.drawCircle(mCx, mCy, mRadius, paint);

        paint.setStyle(Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(STROKE_WIDTH);
        canvas.drawCircle(mCx, mCy, mRadius, paint);
    }

    /**
     * Draws lines around clock face representing hours. Draws hour values.
     *
     * @param canvas
     */
    private void drawHours(Canvas canvas) {

        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(STROKE_WIDTH);
        paint.setStyle(Style.STROKE);
        paint.setAntiAlias(true);

        canvas.save();
        int lineLength = (int) (mRadius * 0.05);
        for (int i = 0; i < 12; i++) {
            // draws short lines around clock face
            // use canvas transformation to rotate clock face during drawing
            canvas.drawLine(mCx, mCy - mRadius, mCx, mCy - mRadius + lineLength, paint);
            canvas.rotate(30, mCx, mCy);
        }
        // restore canvas to its saved position
        canvas.restore();

        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize((int) (mRadius * 0.2));
        textPaint.setColor(Color.WHITE);

        int textRadius = (int) (mRadius * 0.8);
        // canvas transformation can't be used for text drawing
        // text position will be calculated using trigonometric functions
        for (int i = 0; i < 12; i++) {

            Point p = getPoint(i * 5, textRadius, mCx, mCy);

            int hour = i % 12;

            String text = "";
            if (mNumerals == 1) {
                text = ROMAN_NUMBERS[i];
            } else {
                hour = (hour == 0) ? 12 : hour;
                text = String.valueOf(hour);
            }

            // get text bounds to calculate text position
            Rect textBounds = new Rect();
            textPaint.getTextBounds(text, 0, text.length(), textBounds);

            canvas.drawText(text, p.x - (textBounds.width() / 2), p.y + (textBounds.height() / 2),
                    textPaint);
        }
    }

    /**
     * AsyncTask used to tick clock.
     * @author uldis.indriksons
     *
     */
    private class ClockTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            while (!isCancelled()) {

                // if time has changed set new animation starttime
                if (changeTime()) {
                    mAnimationStartTime = System.currentTimeMillis();
                    mAnimationTimeDelta = 0;
                }

                // publish progress on every iteration if animation has not ended
                if (mAnimationTimeDelta != 1) {
                    publishProgress((Void) null);

                    // sleep in order to slow down the animation
                    try {
                        Thread.sleep(SECONDS_ANIMATION_REFRESH_RATE);
                    } catch (InterruptedException e) {
                        // Can swallow this time
                    }
                }

            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            // calculate elapsed animation time
            // multiply by two to make seconds' hand stand still for half a second
            mAnimationTimeDelta = (System.currentTimeMillis() - mAnimationStartTime) * 4 / 1000.0f;

            // if animation has not ended redraw clock
            if(mAnimationTimeDelta != 1) {

                // if timeDelta > 1 set it to 1, because cannot pass numbers
                // larger than 1 to interpolator. This will end animation on next iteration.
                if(mAnimationTimeDelta > 1) {
                    mAnimationTimeDelta = 1;
                }

                mAnimationPosition = mInterpolator.getInterpolation(mAnimationTimeDelta);
                invalidate();
            }

        }
    }

    ClockTask mClockTask;

    @Override
    protected void onDetachedFromWindow() {
        stopTicking();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {

        // start/stop ticking task to save CPU resource
        if (visibility == VISIBLE) {
            // start ticking when view comes to foreground
            mSec = -1;
            mAnimationPosition = 1; // to draw correctly first hand
            mAnimationTimeDelta = 0;
            changeTime();
            mClockTask = new ClockTask();
            mClockTask.execute((Void) null);
        } else {
            stopTicking();
        }
    }

    private void stopTicking() {
        // stop ticking
        if (mClockTask != null) {
            mClockTask.cancel(true);
        }
    }

    /**
     * Calculates position on clock face using given radius and center coordinates.
     * @param minutes
     * @param radius
     * @param cx
     * @param cy
     * @return
     */
    private Point getPoint(float minutes, int radius, int cx, int cy) {

        double angle = Math.toRadians(minutes * 6 - 90);

        int x = cx + (int) (Math.cos(angle) * radius);
        int y = cy + (int) (Math.sin(angle) * radius);

        return new Point(x, y);
    }

    /**
     * Creates Paint for clock hand with given color and width.
     * @param color
     * @param strokeWidth
     * @return
     */
    private Paint createHandPaint(int color, int strokeWidth) {
        Paint paint = new Paint();
        paint.setStyle(Style.STROKE);
        paint.setAntiAlias(true);
        // shadow delta relative to hand width
        // int shadowDelta = (int) (strokeWidth * 0.5);
        int shadowDelta = 3;
        paint.setShadowLayer(5, shadowDelta, shadowDelta, Color.BLACK);
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);

        return paint;
    }
}
