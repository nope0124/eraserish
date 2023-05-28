package com.example.intentactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;

import java.util.ArrayDeque;
import java.util.Deque;


public class CanvasView extends SurfaceView implements Callback {
    private Bitmap originalBitmap; // 初期ビットマップ
    private SurfaceHolder mHolder;
    private Paint mPaint;
    private Path mPath;

    private Bitmap mMosaicBitmap;

    private Bitmap mDrawBitmap;
    private Canvas mDrawCanvas;

    private Deque<Path> mUndoStack = new ArrayDeque<Path>();
    private Deque<Path> mRedoStack = new ArrayDeque<Path>();


    public CanvasView(Context context) {
        super(context);
        init();
    }

    public CanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void clearDrawBitmap() {
        if (mDrawBitmap == null) {
            mDrawBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        }

        if (mDrawCanvas == null) {
            mDrawCanvas = new Canvas(mDrawBitmap);
        }

        mDrawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        clearDrawBitmap();
        // canvasを初期表示(画像のみ)
        Canvas canvas = mHolder.lockCanvas();
        canvas.drawBitmap(originalBitmap, 0, 0, null);
        mHolder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mDrawBitmap != null) {
            mDrawBitmap.recycle();
        }
    }



    private void init() {
        // bitmapの初期化処理
        originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sample).copy(Bitmap.Config.ARGB_8888, true);

        mMosaicBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        // canvasの初期化処理
        mHolder = getHolder();
        setZOrderOnTop(true);
        mHolder.setFormat(PixelFormat.TRANSPARENT);
        mHolder.addCallback(this);

        // ペイントの初期化処理
        mPaint = new Paint();
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(6);
    }


    // タッチ処理
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onTouchDown(event.getX(), event.getY());
                break;

            case MotionEvent.ACTION_MOVE:
                onTouchMove(event.getX(), event.getY());
                break;

            case MotionEvent.ACTION_UP:
                onTouchUp(event.getX(), event.getY());
                break;

            default:
        }
        return true;
    }

    // 指で触った時
    private void onTouchDown(float x, float y) {
        mPath = new Path();
        mPath.moveTo(x, y);
    }


    // 指動かした時
    private void onTouchMove(float x, float y) {
        mPath.lineTo(x, y);
        int radius = 30;
        int border = 30;
        int[][] R = new int[radius*2+2][radius*2+2];
        int[][] G = new int[radius*2+2][radius*2+2];
        int[][] B = new int[radius*2+2][radius*2+2];
        int[][] count = new int[radius*2+2][radius*2+2];
        for(int i = 0; i < radius*2+2; i++) {
            for(int j = 0; j < radius*2+2; j++) {
                count[i][j] = 0;
            }
        }
        for(int i = 0; i < radius*2+2; i++) {
            for(int j = 0; j < radius*2+2; j++) {
                int nx = (int)(x + j - radius);
                int ny = (int)(y + i - radius);
                if(nx < 0 || nx >= originalBitmap.getWidth() || ny < 0 || ny >= originalBitmap.getHeight()) continue;

                int pixel = originalBitmap.getPixel(nx, ny);

                int up = Math.max(i-border, 0);
                int down = Math.min(i+border+1, radius*2+1);
                int left = Math.max(j-border, 0);
                int right = Math.min(j+border+1, radius*2+1);

                R[up][left] += Color.red(pixel);
                R[down][right] += Color.red(pixel);
                R[up][right] -= Color.red(pixel);
                R[down][left] -= Color.red(pixel);

                G[up][left] += Color.green(pixel);
                G[down][right] += Color.green(pixel);
                G[up][right] -= Color.green(pixel);
                G[down][left] -= Color.green(pixel);

                B[up][left] += Color.blue(pixel);
                B[down][right] += Color.blue(pixel);
                B[up][right] -= Color.blue(pixel);
                B[down][left] -= Color.blue(pixel);

                count[up][left] += 1;
                count[down][right] += 1;
                count[up][right] -= 1;
                count[down][left] -= 1;

            }
        }

        for(int i = 0; i < radius*2+2; i++) {
            for(int j = 0; j < radius*2+1; j++) {
                R[i][j+1] += R[i][j];
                G[i][j+1] += G[i][j];
                B[i][j+1] += B[i][j];
                count[i][j+1] += count[i][j];
            }
        }

        for(int j = 0; j < radius*2+2; j++) {
            for(int i = 0; i < radius*2+1; i++) {
                R[i+1][j] += R[i][j];
                G[i+1][j] += G[i][j];
                B[i+1][j] += B[i][j];
                count[i+1][j] += count[i][j];
            }
        }

        for(int i = 0; i < radius*2+1; i++) {
            for(int j = 0; j < radius*2+1; j++) {
                int nx = (int)(x + j - radius);
                int ny = (int)(y + i - radius);
                if(nx < 0 || nx >= mMosaicBitmap.getWidth() || ny < 0 || ny >= mMosaicBitmap.getHeight()) continue;
                if(count[i][j] == 0) continue;

                R[i][j] /= count[i][j];
                G[i][j] /= count[i][j];
                B[i][j] /= count[i][j];

                int invertedPixel = Color.argb(255, R[i][j], G[i][j], B[i][j]);
                mMosaicBitmap.setPixel(nx, ny, invertedPixel);
            }
        }


        drawLine(mPath);
    }

    // 指離した時
    private void onTouchUp(float x, float y) {
        mPath.lineTo(x, y);
        drawLine(mPath);
        mUndoStack.addLast(mPath);
        mRedoStack.clear();
    }


    // 描画処理
    private void drawLine(Path path) {
        // ロックしてキャンバスを取得します。
        Canvas canvas = mHolder.lockCanvas();

        // キャンバスをクリアします。
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);

        // まず、画像を下のレイヤーに配置
        canvas.drawBitmap(originalBitmap, 0, 0, null);

        canvas.drawBitmap(mMosaicBitmap, 0, 0, null);

        canvas.drawBitmap(mDrawBitmap, 0, 0, null);

        // パスを描画します。
//        canvas.drawPath(path, mPaint);

        // ロックを外します。
        mHolder.unlockCanvasAndPost(canvas);
    }

    public void undo() {
        if (mUndoStack.isEmpty()) return;

        // undoスタックからパスを取り出し、redoスタックに格納します。
        Path lastUndoPath = mUndoStack.removeLast();
        mRedoStack.addLast(lastUndoPath);

        // ロックしてキャンバスを取得します。
        Canvas canvas = mHolder.lockCanvas();

        // キャンバスをクリアします。
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        canvas.drawBitmap(originalBitmap, 0, 0, null);

        // 描画状態を保持するBitmapをクリアします。
        clearDrawBitmap();

        // パスを描画します。
        for (Path path : mUndoStack) {
            canvas.drawPath(path, mPaint);
            mDrawCanvas.drawPath(path, mPaint);
        }

        // ロックを外します。
        mHolder.unlockCanvasAndPost(canvas);
    }

    public void redo() {
        if (mRedoStack.isEmpty()) return;

        // redoスタックからパスを取り出し、undoスタックに格納します。
        Path lastRedoPath = mRedoStack.removeLast();
        mUndoStack.addLast(lastRedoPath);

        // パスを描画します。
        drawLine(lastRedoPath);

        mDrawCanvas.drawPath(lastRedoPath, mPaint);
    }

    public void reset() {
        mUndoStack.clear();
        mRedoStack.clear();

        clearDrawBitmap();

        if (mHolder != null && mHolder.getSurface().isValid()) {
            Canvas canvas = mHolder.lockCanvas();
            if (canvas != null) {
                // canvasを初期表示(画像のみ)
                canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                canvas.drawBitmap(originalBitmap, 0, 0, null);
                mHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    public void setCanvas(Bitmap mBitmap){
        // PictureActivityからBitMapを受け取る
        if (mDrawBitmap != null) {
            mDrawBitmap.recycle();
        }

        mDrawBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);

        mDrawCanvas = new Canvas(mDrawBitmap);

        originalBitmap = mBitmap.copy(Bitmap.Config.ARGB_8888, true);

        mMosaicBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        reset();
    }

}