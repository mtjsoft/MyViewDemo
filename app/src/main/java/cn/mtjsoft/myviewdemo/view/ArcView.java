package cn.mtjsoft.myviewdemo.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import cn.mtjsoft.myviewdemo.R;

/**
 * 自定义继承View
 * 1、处理wrap_content问题 （最简单的方式是设置一个默认值）
 * 2、处理padding问题 （在onDraw中绘制获取宽高的时候，减掉 - getPaddingLeft() - getPaddingRight() - getPaddingTop() - getPaddingBottom()）
 */
public class ArcView extends View {


    private int mWidth;
    private int mHeight;
    /**
     * 弧形高度
     */
    private final int mArcHeight;
    /**
     * 背景颜色
     */
    private int mBgColor;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    /**
     * 底部曲线
     */
    private final Path path = new Path();
    private RectF rectF;
    /**
     * 波纹
     */
    private final Path mWavePath = new Path();
    private final Path mWavePath2 = new Path();
    private final int mWaveHeight;
    private int mWaveColor;
    private static final int SAMPLE_SIZE = 128;
    private final float[] samplingX = new float[SAMPLE_SIZE];//采样点
    private float waveGap;
    // 速度
    private static final int DEFAULT_PERIOD = 80;
    private final float mSpeed = 0.1F;
    private float offset = 0;

    private float offsetLine2 = 200;

    public ArcView(Context context) {
        this(context, null);
    }

    public ArcView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ArcView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ArcView);
        mBgColor = typedArray.getColor(R.styleable.ArcView_arc_bgColor, Color.parseColor("#FF0000"));
        mArcHeight = typedArray.getDimensionPixelSize(R.styleable.ArcView_arc_height, 0);
        mWaveHeight = typedArray.getDimensionPixelSize(R.styleable.ArcView_arc_wave_height, mArcHeight);
        mWaveColor = typedArray.getColor(R.styleable.ArcView_arc_wave_color, Color.parseColor("#50FFFFFF"));
        typedArray.recycle();
        mPaint.setAntiAlias(true);
        // 开始波浪动画
        postDelayed(new WaveAnimation(), DEFAULT_PERIOD);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            mWidth = widthSize;
            mHeight = heightSize;
        } else if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.AT_MOST) {
            mWidth = 200;
            mHeight = 200;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            mWidth = 200;
            mHeight = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            mWidth = widthSize;
            mHeight = 200;
        }
        setMeasuredDimension(mWidth, mHeight);
        waveGap = mWidth / (float) SAMPLE_SIZE;
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            if (i == SAMPLE_SIZE - 1) {
                samplingX[i] = mWidth;
                return;
            }
            samplingX[i] = i * waveGap;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBg(canvas);
        drawWave(canvas);
    }

    public void setBgColor(int bgColor) {
        mBgColor = bgColor;
    }

    public void setWaveColor(int color) {
        mWaveColor = color;
    }

    private void drawBg(Canvas canvas) {
        mPaint.setColor(mBgColor);
        mPaint.setStyle(Paint.Style.FILL);
        // 矩形区域
        rectF = new RectF(0, 0, mWidth, mHeight - mArcHeight);
        canvas.drawRect(rectF, mPaint);
        // 圆弧区域
        if (mArcHeight > 0) {
            path.reset();
            path.moveTo(0, rectF.bottom);
            path.quadTo(mWidth >> 1, mHeight, mWidth, rectF.bottom);
            canvas.drawPath(path, mPaint);
        }
    }

    /**
     * 绘制波浪
     *
     * @param canvas
     */
    private void drawWave(Canvas canvas) {
        mPaint.setColor(mWaveColor);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeWidth(2);
        if (mWaveHeight > 0) {
            //
            mWavePath.reset();
            mWavePath.moveTo(0, rectF.bottom);
            //
            mWavePath2.reset();
            mWavePath2.moveTo(0, rectF.bottom);
            for (int i = 0; i < SAMPLE_SIZE; i++) {
                // 计算y点值
                float y = getMyValue(samplingX[i], offset, mWaveHeight / 2, mWidth, (int) (rectF.bottom - mWaveHeight));
                if (y <= rectF.bottom) {
                    mWavePath.lineTo(samplingX[i], y);
                }
                //
                float y2 = getMyValue(samplingX[i], offsetLine2, mWaveHeight / 2, mWidth, (int) (rectF.bottom - mWaveHeight));
                if (y2 <= rectF.bottom) {
                    mWavePath2.lineTo(samplingX[i], y2);
                }
            }
            mWavePath.lineTo(mWidth, rectF.bottom);
            mWavePath.close();
            canvas.drawPath(mWavePath, mPaint);
            //
            mWavePath2.lineTo(mWidth, rectF.bottom);
            mWavePath2.close();
            canvas.drawPath(mWavePath2, mPaint);
            //
            canvas.drawPath(path, mPaint);
            canvas.drawPath(path, mPaint);
        }
    }

    private float getMyValue(float x, float offset, int i, int width, int height) {
        return (float) Math.sin(2 * Math.PI * x / width + offset) * i + (height);
    }

    private final class WaveAnimation implements Runnable {
        @Override
        public void run() {
            mWavePath.reset();
            offset += mSpeed;
            offsetLine2 += mSpeed * 1.5;
            invalidate();
            ArcView.this.postDelayed(this, DEFAULT_PERIOD);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(null);
        super.onDetachedFromWindow();
    }
}
