package com.example.martinrgb.renderscriptfiltertest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsic3DLUT;
import android.support.v8.renderscript.Type;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.abcew.camera.ScriptC_image_translate_3d;

public class MainActivity extends AppCompatActivity {

    ImageView mImageView;
    RenderScript mRs;
    Bitmap mBitmap;
    Bitmap mLutBitmap;
    ScriptIntrinsic3DLUT mScriptlut;
    Bitmap mOutputBitmap;
    Allocation mAllocIn;
    Allocation mAllocOut;
    Allocation mAllocCube;
    int mFilter = 0;
    //512 Lut
    int[] mLut3D = {
            R.drawable.lut_purple,
            R.drawable.lut_ad1920,
            R.drawable.lut_ancient,
            R.drawable.lut_bleachedblue,
            R.drawable.lut_blues,
            R.drawable.lut_bw,
            R.drawable.lut_celsius,
            R.drawable.lut_chest,
            R.drawable.lut_breeze,
            R.drawable.lut_sin
    };

    //256 16 LUT
//    int[] mLut3D = {
//            R.drawable.lut_vintage,
//            R.drawable.lut_bleach,
//            R.drawable.lut_blue_crush,
//            R.drawable.lut_bw_contrast,
//            R.drawable.lut_instant,
//            R.drawable.lut_punch,
//            R.drawable.lut_washout,
//            R.drawable.lut_washout_color,
//            R.drawable.lut_x_process
//    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageView = (ImageView) findViewById(R.id.imageView);
        final ImageView mImageView2 = (ImageView) findViewById(R.id.imageView2);
        final Bitmap mBitmap2 = BitmapFactory.decodeResource(getResources(), R.drawable.scene);
        mImageView2.setImageBitmap(mBitmap2);
        setClickListener();
        mImageView.setOnClickListener(imageViewListener);
        mRs = RenderScript.create(this);
        Background background = new Background();
        background.execute();
    }

    private View.OnClickListener imageViewListener;

    private void setClickListener(){
        imageViewListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mFilter = (1 + mFilter) % (mLut3D.length + 1);
                Background background = new Background();
                background.execute();
            }
        };

    };

    //512x512 LUT
    class Background extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            int redDim, greenDim, blueDim;
            int w, h;
            int[] lut;
            if (mScriptlut == null) {
                mScriptlut = ScriptIntrinsic3DLUT.create(mRs, Element.U8_4(mRs));
            }
            if (mBitmap == null) {
                mBitmap = BitmapFactory.decodeResource(getResources(),
                        R.drawable.scene);

                mOutputBitmap = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), mBitmap.getConfig());

                mAllocIn = Allocation.createFromBitmap(mRs, mBitmap);
                mAllocOut = Allocation.createFromBitmap(mRs, mOutputBitmap);
            }
            Type.Builder tb = new Type.Builder(mRs, Element.U8_4(mRs));
            tb.setX(64).setY(64).setZ(64);
            Type t = tb.create();
            mAllocCube = Allocation.createTyped(mRs, t);
            if (mFilter != 0) {
                mLutBitmap = BitmapFactory.decodeResource(getResources(), mLut3D[mFilter - 1]);
                Bitmap cache = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);

                Allocation mAllocIn = Allocation.createFromBitmap(mRs, mLutBitmap);
                Allocation mAllocOut = Allocation.createFromBitmap(mRs, cache);
                ScriptC_image_translate_3d script = new ScriptC_image_translate_3d(mRs);
                script.set_gIn(mAllocIn);
                script.set_gOut(mAllocOut);
                //Invoke script
                script.forEach_root(mAllocIn, mAllocOut);

                byte[] lut2 = new byte[512 * 512 * 4];
                mAllocOut.copyTo(lut2);

                mAllocCube.copyFromUnchecked(lut2);

            } else {
                redDim = greenDim = blueDim = 64;
                int i = 0;
//                512*512 == 64 * 64 * 64
                lut = new int[redDim * greenDim * blueDim];

                for (int r = 0; r < redDim; r++) {
                    for (int g = 0; g < greenDim; g++) {
                        for (int b = 0; b < blueDim; b++) {
                            int bcol = (b * 255) / blueDim;
                            int gcol = (g * 255) / greenDim;
                            int rcol = (r * 255) / redDim;
                            lut[i++] = bcol | (gcol << 8) | (rcol << 16);
                        }
                    }
                }

                mAllocCube.copyFromUnchecked(lut);
            }
            long end2 = System.currentTimeMillis();
            mScriptlut.setLUT(mAllocCube);
            mScriptlut.forEach(mAllocIn, mAllocOut);
            mAllocOut.copyTo(mOutputBitmap);
            long end = System.currentTimeMillis();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mImageView.setImageBitmap(mOutputBitmap);
        }
    }

      //256 x 16 LUT
//    class Background extends AsyncTask<Void, Void, Void> {
//
//        @Override
//        protected Void doInBackground(Void... params) {
//            int redDim, greenDim, blueDim;
//            int w, h;
//            int[] lut;
//
//            if (mScriptlut == null) {
//                mScriptlut = ScriptIntrinsic3DLUT.create(mRs, Element.U8_4(mRs));
//            }
//            if (mBitmap == null) {
//                mBitmap = BitmapFactory.decodeResource(getResources(),
//                        R.drawable.scene);
//
//                mOutputBitmap = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), mBitmap.getConfig());
//
//                mAllocIn = Allocation.createFromBitmap(mRs, mBitmap);
//                mAllocOut = Allocation.createFromBitmap(mRs, mOutputBitmap);
//            }
//            if (mFilter != 0) {
//                mLutBitmap = BitmapFactory.decodeResource(getResources(), mLut3D[mFilter - 1]);
//                w = mLutBitmap.getWidth();
//                h = mLutBitmap.getHeight();
//                redDim = w / h;
//                greenDim = redDim;
//                blueDim = redDim;
//                int[] pixels = new int[w * h];
//                lut = new int[w * h];
//                mLutBitmap.getPixels(pixels, 0, w, 0, 0, w, h);
//                int i = 0;
//
//                for (int r = 0; r < redDim; r++) {
//                    for (int g = 0; g < greenDim; g++) {
//                        int p = r + g * w;
//                        for (int b = 0; b < blueDim; b++) {
//                            lut[i++] = pixels[p + b * h];
//                        }
//                    }
//                }
//
//            } else {
//                // identity filter provided for refrence
//                redDim = greenDim = blueDim = 16;
//                lut = new int[redDim * greenDim * blueDim];
//                int i = 0;
//                for (int r = 0; r < redDim; r++) {
//                    for (int g = 0; g < greenDim; g++) {
//                        for (int b = 0; b < blueDim; b++) {
//                            int bcol = (b * 255) / blueDim;
//                            int gcol = (g * 255) / greenDim;
//                            int rcol = (r * 255) / redDim;
//                            lut[i++] = bcol | (gcol << 8) | (rcol << 16);
//                        }
//                    }
//                }
//            }
//            Type.Builder tb = new Type.Builder(mRs, Element.U8_4(mRs));
//            tb.setX(redDim).setY(greenDim).setZ(blueDim);
//            Type t = tb.create();
//            mAllocCube = Allocation.createTyped(mRs, t);
//            mAllocCube.copyFromUnchecked(lut);
//
//            mScriptlut.setLUT(mAllocCube);
//            mScriptlut.forEach(mAllocIn, mAllocOut);
//
//            mAllocOut.copyTo(mOutputBitmap);
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(Void aVoid) {
//            mImageView.setImageBitmap(mOutputBitmap);
//        }
//    }
}
