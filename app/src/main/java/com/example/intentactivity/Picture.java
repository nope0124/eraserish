package com.example.intentactivity;
import androidx.fragment.app.FragmentActivity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.Button;

import java.io.File;
import java.io.IOException;
import java.io.FileDescriptor;
import java.util.Calendar;


public class Picture extends FragmentActivity implements OnClickListener {
    private static final int REQUEST_PICK_IMAGEFILE = 1000;
    private static final int REQUEST_CAPTURE_IMAGEFILE = 100;
    public CanvasView mCanvasView;
    private Button mUndoBtn;
    private Button mRedoBtn;
    private Button mResetBtn;
    private Button mMosaicBtn;
    private Button mDrawBtn;
    private File photoFile;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);
        mCanvasView = (CanvasView)findViewById(R.id.canvasView);

        mUndoBtn = (Button) findViewById(R.id.undoBtn);
        mUndoBtn.setOnClickListener(this);

        mRedoBtn = (Button) findViewById(R.id.redoBtn);
        mRedoBtn.setOnClickListener(this);

        mResetBtn = (Button) findViewById(R.id.resetBtn);
        mResetBtn.setOnClickListener(this);

        mMosaicBtn = (Button) findViewById(R.id.mosaicBtn);
        mMosaicBtn.setOnClickListener(this);

        mDrawBtn = (Button) findViewById(R.id.drawBtn);
        mDrawBtn.setOnClickListener(this);

        // 画像選択ボタン
        findViewById(R.id.albumBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_PICK_IMAGEFILE);
            }
        });

        // 撮影ボタン
        findViewById(R.id.cameraBtn).setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), getFileName());
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(intent, REQUEST_CAPTURE_IMAGEFILE);
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == REQUEST_PICK_IMAGEFILE && resultCode == RESULT_OK) {
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                try {
                    Bitmap mBitmap = getBitmapFromUri(uri);
                    mCanvasView.setCanvas(mBitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if(requestCode == REQUEST_CAPTURE_IMAGEFILE && resultCode == Activity.RESULT_OK ){
            Bitmap capturedImage = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
            mCanvasView.setCanvas(capturedImage);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mUndoBtn) {
            mCanvasView.undo();
        } else if (v == mRedoBtn) {
            mCanvasView.redo();
        } else if (v == mResetBtn) {
            mCanvasView.reset();
        } else if (v == mMosaicBtn) {
            mCanvasView.setMosaic(true);
        } else if (v == mDrawBtn) {
            mCanvasView.setMosaic(false);
        }
    }

    protected String getFileName(){
        Calendar c = Calendar.getInstance();
        String s = c.get(Calendar.YEAR)
                + "_" + (c.get(Calendar.MONTH)+1)
                + "_" + c.get(Calendar.DAY_OF_MONTH)
                + "_" + c.get(Calendar.HOUR_OF_DAY)
                + "_" + c.get(Calendar.MINUTE)
                + "_" + c.get(Calendar.SECOND)
                + ".jpg";
        return s;
    }

    public Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

}