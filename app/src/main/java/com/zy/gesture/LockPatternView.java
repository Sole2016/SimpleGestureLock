package com.zy.gesture;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class LockPatternView extends View {
    private static final String TAG = "LockPatternView";
    private Paint mNormalPaint;//普通画笔
    private Paint mSelectPaint;//选中画笔
    private Paint mErrorPaint;//错误画笔
    private ArrayList<Integer> mSelectedPoint;//已选中的点

    private int mMinPointWidth = 100;
    private int mMinPointHeight = 100;
    private int mMinMargin = 20;
    private int mPointRadius;//点的半径

    private int mPointWidth;//点的宽度
    private int mPointHeight;//点的高度
    private int mPointMargin;//点之间的边距

    private int lineX, lineY;
    private boolean isTouching;//是否是触摸状态
    private int normalColor = Color.GRAY;
    private int handingColor = Color.GREEN;
    private int errorColor = Color.RED;

    private Path mLinePath;
    private Paint mLinePaint;

    private ArrayList<LockPoint> mPoints;

    private OnGestureLockListener gestureLockListener;

    public interface PointState {
        int NORMAL_STATE = 0X001;
        int HANDING_STATE = 0X002;//绘制图案
        int ERROR_STATE = 0X003;//错误
    }

    public void setGestureLockListener(OnGestureLockListener gestureLockListener) {
        this.gestureLockListener = gestureLockListener;
    }

    public static class LockPoint implements Parcelable {
        private int mRaw;
        private int mColumn;
        private int x;
        private int y;
        private int mState;

        private LockPoint(int raw, int column, int state) {
            mRaw = raw;
            mColumn = column;
            mState = state;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getRaw() {
            return mRaw;
        }

        public void setRaw(int mRaw) {
            this.mRaw = mRaw;
        }

        public int getColumn() {
            return mColumn;
        }

        public void setColumn(int mColumn) {
            this.mColumn = mColumn;
        }

        public int getState() {
            return mState;
        }

        public void setState(int mState) {
            this.mState = mState;
        }

        private LockPoint(Parcel in) {
            mRaw = in.readInt();
            mColumn = in.readInt();
            mState = in.readInt();
            x = in.readInt();
            y = in.readInt();
        }

        public boolean isInRound(int touchX, int touchY, int radius) {
            double distance = Math.sqrt(((touchX - x) * (touchX - x)) + ((touchY - y) * (touchY - y)));
            return distance < radius;
        }

        public static final Creator<LockPoint> CREATOR = new Creator<LockPoint>() {
            @Override
            public LockPoint createFromParcel(Parcel in) {
                return new LockPoint(in);
            }

            @Override
            public LockPoint[] newArray(int size) {
                return new LockPoint[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(mRaw);
            parcel.writeInt(mColumn);
            parcel.writeInt(mState);
            parcel.writeInt(x);
            parcel.writeInt(y);
        }


        @Override
        public String toString() {
            return "LockPoint{" +
                    "mRaw=" + mRaw +
                    ", mColumn=" + mColumn +
                    '}';
        }
    }

    private LockPatternView(Context context) {
        this(context, null);
    }

    public LockPatternView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public LockPatternView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray array = context.obtainStyledAttributes(attrs,R.styleable.LockPatternView);
        normalColor = array.getColor(R.styleable.LockPatternView_normalColor,Color.GRAY);
        handingColor = array.getColor(R.styleable.LockPatternView_selectColor,Color.GREEN);
        errorColor = array.getColor(R.styleable.LockPatternView_errorColor,Color.RED);
        array.recycle();
        initView();
    }

    private void initView() {
        setClickable(true);
        this.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mNormalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mNormalPaint.setColor(normalColor);
        mNormalPaint.setAntiAlias(true);
        mNormalPaint.setStyle(Paint.Style.FILL);

        mSelectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSelectPaint.setColor(handingColor);
        mSelectPaint.setStyle(Paint.Style.FILL);

        mErrorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mErrorPaint.setStyle(Paint.Style.FILL);
        mErrorPaint.setColor(errorColor);

        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setColor(mSelectPaint.getColor());
        mLinePaint.setStrokeWidth(5);
        mLinePaint.setAntiAlias(true);

        mLinePath = new Path();
        mSelectedPoint = new ArrayList<>();

        mPoints = new ArrayList<>();
        int raw = 0;
        int column = 1;
        for (int i = 0; i < 9; i++) {
            if (i % 3 == 0) {
                raw++;
                column = 1;
            }
            LockPoint point = new LockPoint(raw, column, PointState.NORMAL_STATE);
            mPoints.add(point);
            column++;
        }
        Log.w(TAG, "initView: " + mPoints.toString());
    }


    private void initInfo(int w, int h) {
        mPointWidth = (w / 3) / 3;
        mPointMargin = ((w / 3) - mPointWidth) / 2;
        mPointRadius = mPointWidth / 2;
        mPointHeight = (h / 3) / 3;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initInfo(w, h);
        Log.e(TAG, "onSizeChanged: " + mPointWidth + "," + mPointMargin + "," + mPointRadius);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getSize(widthMeasureSpec);
        int height = getSize(heightMeasureSpec);
        int minWidth = 3 * mMinPointWidth + 3 * 2 * mMinMargin;
        int minHeight = 3 * mMinPointHeight + 3 * 2 * mMinMargin;
        int measureWidth = Math.max(width, minWidth);
        int measureHeight = Math.max(height, minHeight);
        initInfo(measureWidth, measureHeight);
        Log.e(TAG, "onMeasure: setMeasuredDimension=" + measureWidth + "," + measureHeight);
        setMeasuredDimension(measureWidth, measureHeight);
    }

    public int getSize(int spec) {
        switch (MeasureSpec.getMode(spec)) {
            case MeasureSpec.EXACTLY:
                return MeasureSpec.getSize(spec);
            case MeasureSpec.AT_MOST:
            case MeasureSpec.UNSPECIFIED:
                return 0;
            default:
                return 0;
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handleActionDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                handleActionMove(event);
                break;
            case MotionEvent.ACTION_UP:
                handleActionUp(event);
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
            default:
                break;
        }
        lineX = (int) event.getX();
        lineY = (int) event.getY();
        invalidate();
        return true;
    }

    private void handleActionDown(MotionEvent event) {
        clearState();
        isTouching = true;
        Log.d(TAG, "handleActionDown() called with: x = [" + event.getX() + ",y=" + event.getY() + "]");
        int x = (int) event.getX();
        int y = (int) event.getY();
        LockPoint point = checkPoint(x, y);
        if (point != null) {
            mLinePaint.setColor(mSelectPaint.getColor());
            mSelectedPoint.add(mPoints.indexOf(point));
            point.setState(PointState.HANDING_STATE);
        }
        if (gestureLockListener != null) {
            gestureLockListener.onStartGestureLock();
        }
    }

    private void handleActionMove(MotionEvent event) {
        Log.d(TAG, "handleActionMove() called with: x = [" + event.getX() + ",y=" + event.getY() + "]");
        int x = (int) event.getX();
        int y = (int) event.getY();
        LockPoint point = checkPoint(x, y);
        //如果连接到一个点，先判断这条线上是否经过另一个点，经过另一个点就先添加经过的点，然后连接得到的点
        if (point != null && point.getState() == PointState.NORMAL_STATE) {
            if (mSelectedPoint.size() > 0) {
                int lastPosition = mSelectedPoint.get(mSelectedPoint.size() - 1);
                LockPoint lastPoint = mPoints.get(lastPosition);
                LockPoint centerPoint = getCenterPoint(lastPoint, point);
                if (centerPoint != null && centerPoint.getState() == PointState.NORMAL_STATE) {
                    centerPoint.setState(PointState.HANDING_STATE);
                    mSelectedPoint.add(mPoints.indexOf(centerPoint));
                }
            } else {
                mLinePaint.setColor(mSelectPaint.getColor());
                if (gestureLockListener != null) {
                    gestureLockListener.onStartGestureLock();
                }
            }
            mSelectedPoint.add(mPoints.indexOf(point));
            point.setState(PointState.HANDING_STATE);
        }
    }

    public LockPoint getCenterPoint(LockPoint last, LockPoint select) {
        if (Math.abs(select.mRaw - last.mRaw) == 2 &&
                Math.abs(select.mColumn - last.mColumn) == 2) {
            return getPointByRawAndColumn(Math.abs(select.mRaw - last.mRaw),
                    Math.abs(select.mColumn - last.mColumn));
        } else if (Math.abs(select.mColumn - last.mColumn) == 2 && select.getRaw() == last.getRaw()) {//纵方向差2
            return getPointByRawAndColumn(select.getRaw(), Math.abs(select.mColumn - last.mColumn));
        } else if (Math.abs(select.mRaw - last.mRaw) == 2 && select.getColumn() == last.getColumn()) {//横向差2
            return getPointByRawAndColumn(Math.abs(select.mRaw - last.mRaw), select.getColumn());
        }
        return null;
    }

    public LockPoint getPointByRawAndColumn(int raw, int column) {
        for (LockPoint mPoint : mPoints) {
            if (mPoint.mRaw == raw && mPoint.mColumn == column) {
                return mPoint;
            }
        }
        return null;
    }

    /**
     * 绘制结束
     */
    private void handleActionUp(MotionEvent event) {
        isTouching = false;
        if (mSelectedPoint.size() > 0 && mSelectedPoint.size() < 4) {
            for (Integer integer : mSelectedPoint) {
                LockPoint point = mPoints.get(integer);
                point.setState(PointState.ERROR_STATE);
            }
            mLinePaint.setColor(mErrorPaint.getColor());
            if (gestureLockListener != null) {
                gestureLockListener.onError("请连接4个以上的点");
            }
            resetState();
        } else if (mSelectedPoint.size() >= 4) {
            resetState();
            if (gestureLockListener != null) {
                gestureLockListener.onGestureSuccess(mSelectedPoint);
            }
        }
    }

    private LockPoint checkPoint(int x, int y) {
        for (LockPoint mPoint : mPoints) {
            if (mPoint.isInRound(x, y, mPointRadius)) {
                return mPoint;
            }
        }
        return null;
    }

    private Handler mDelayHandler = new Handler();

    /**
     * 延迟1s重制界面
     */
    public void resetState() {
        mDelayHandler.removeCallbacksAndMessages(null);
        mDelayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mSelectedPoint.clear();
                mLinePath.reset();
                for (LockPoint mPoint : mPoints) {
                    mPoint.setState(PointState.NORMAL_STATE);
                }
                setClickable(true);
                invalidate();
            }
        }, 500);
    }

    public void clearState() {
        mDelayHandler.removeCallbacksAndMessages(null);
        if (mSelectedPoint.size() == 0) {
            return;
        }
        mSelectedPoint.clear();
        mLinePath.reset();
        for (LockPoint mPoint : mPoints) {
            mPoint.setState(PointState.NORMAL_STATE);
        }
        setClickable(true);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (int i = 0; i < mPoints.size(); i++) {
            LockPoint point = mPoints.get(i);
            int x, y;
            if (point.x == 0) {
                x = mPointMargin + mPointRadius + (point.mColumn - 1) * (mPointWidth + 2 * mPointMargin);
                y = mPointMargin + mPointRadius + (point.mRaw - 1) * (mPointHeight + 2 * mPointMargin);
                point.setX(x);
                point.setY(y);
            } else {
                x = point.x;
                y = point.y;
            }
            if (point.getState() == PointState.ERROR_STATE) {
                canvas.drawCircle(x, y, mPointRadius, mErrorPaint);
            } else if (point.getState() == PointState.NORMAL_STATE) {
                canvas.drawCircle(x, y, mPointRadius, mNormalPaint);
            } else {
                canvas.drawCircle(x, y, mPointRadius, mSelectPaint);
            }
        }
        drawLines(canvas);
    }

    /**
     * 绘制path
     */
    public void drawLines(Canvas canvas) {
        mLinePath.reset();
        if (mSelectedPoint.size() > 0) {
            for (int i = 0; i < mSelectedPoint.size(); i++) {
                LockPoint point = mPoints.get(mSelectedPoint.get(i));
                if (i == 0) {
                    mLinePath.moveTo(point.x, point.y);
                } else {
                    mLinePath.lineTo(point.x, point.y);
                }
            }
            //触摸停止最后的线停止绘制
            if (isTouching && lineY != 0) {
                mLinePath.lineTo(lineX, lineY);
            }
            canvas.drawPath(mLinePath, mLinePaint);
        }
    }

}
