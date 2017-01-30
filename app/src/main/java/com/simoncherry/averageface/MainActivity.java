package com.simoncherry.averageface;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.orhanobut.logger.Logger;
import com.squareup.picasso.Picasso;
import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();
    private final static int FILE_REQUEST_CODE = 233;

    @ViewById(R.id.iv_img)
    ImageView ivImg;
    @ViewById(R.id.btn_detect)
    Button btnDetect;
    @ViewById(R.id.btn_reset)
    Button btnReset;
    @ViewById(R.id.btn_gray)
    Button btnGray;
    @ViewById(R.id.btn_binary)
    Button btnBinary;
    @ViewById(R.id.btn_edge)
    Button btnEdge;
    @ViewById(R.id.btn_file)
    Button btnFile;

    private Context mContext;
    //private Unbinder unbinder;

    private FaceDet mFaceDet;
    private ProgressDialog mDialog;
    private String mImgPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        //unbinder = ButterKnife.bind(this);
        mContext = MainActivity.this;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unbinder.unbind();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == FILE_REQUEST_CODE) {
                Uri uri = data.getData();
                mImgPath = FileUtils.getFileAbsolutePath(this, uri);
//                if (mImgPath != null) {
//                    File file = new File(mImgPath);
//                    Picasso.with(mContext).load(file)
//                            .fit().centerCrop()
//                            .into(ivImg);
//
//                    //runDetectAsync(path);
//                } else {
//                    Toast.makeText(mContext, "Image Path is null", Toast.LENGTH_SHORT).show();
//                }
                showOriginalImage();
            }
        }
    }

    private void startFileManager() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, FILE_REQUEST_CODE);
    }

    private void detectFace() {
        if (mImgPath != null) {
//            String fileName = FileUtils.getMD5(mImgPath) + ".txt";
            String fileName = getFilesDir().getAbsolutePath() + "/" + FileUtils.getMD5(mImgPath) + ".txt";
//            String json = FileUtils.readFileData(mContext, fileName);
//            if (json != null && !TextUtils.isEmpty(json)) {
//                Toast.makeText(mContext, "This image had already detected", Toast.LENGTH_SHORT).show();
//                Logger.t(TAG).e("get json file: " + json);
//                //JNIUtils.testParseJson(json);
//                try {
//                    JSONObject jsonObject = JSON.parseObject(json);
//                    final List<Landmark> mLandmarks = JSON.parseArray(jsonObject.getString("landmark"), Landmark.class);
//                    Logger.t(TAG).e("get landmarks: " + mLandmarks.toString());
//
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            //ivImg.setImageDrawable(drawRect(mImgPath, landmarks, Color.GREEN));
//                            ivImg.setImageDrawable(drawRectWithLandmark(mImgPath, mLandmarks, Color.GREEN));
//                        }
//                    });
//
//                } catch (Exception e) {
//                    Logger.t(TAG).e(e.toString());
//                }
//            } else {
//                runDetectAsync(mImgPath);
//            }
            File file = new File(fileName);
            if (file.exists()) {
                try {
                    FileReader fileReader = new FileReader(fileName);
                    BufferedReader br = new BufferedReader(fileReader);
                    final List<Landmark> mLandmarks = new ArrayList<>();
                    int i = 0;
                    for(String str; (str = br.readLine()) != null; ) {  // 这里不能用while(br.readLine()) != null) 因为循环条件已经读了一条
                        Logger.t(TAG).e("read landmark[" + String.valueOf(i) + "]: " + str);
                        i++;
                        String[] strArray = str.split(" ");
                        //Logger.t(TAG).e("get x: " + strArray[0]);
                        //Logger.t(TAG).e("get y: " + strArray[1]);
                        Landmark landmark = new Landmark();
                        landmark.setX(Integer.parseInt(strArray[0]));
                        landmark.setY(Integer.parseInt(strArray[1]));
                        mLandmarks.add(landmark);
                    }
                    br.close();

                    Toast.makeText(mContext, "This image had already detected", Toast.LENGTH_SHORT).show();
                    Logger.t(TAG).e("get landmarks: " + mLandmarks.toString());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //ivImg.setImageDrawable(drawRect(mImgPath, landmarks, Color.GREEN));
                            ivImg.setImageDrawable(drawRectWithLandmark(mImgPath, mLandmarks, Color.GREEN));
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    Logger.t(TAG).e(e.toString());
                }
            } else {
                //Toast.makeText(mContext, "cannot read landmarks txt", Toast.LENGTH_SHORT).show();
                runDetectAsync(mImgPath);
            }

        } else {
            Toast.makeText(mContext, "Image path is null, cannot detect face", Toast.LENGTH_SHORT).show();
        }
    }

    @Background
    protected void runDetectAsync(@NonNull final String imgPath) {
        showDialog();

        final String targetPath = Constants.getFaceShapeModelPath();
        if (!new File(targetPath).exists()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Copy landmark model to " + targetPath, Toast.LENGTH_SHORT).show();
                }
            });
            FileUtils.copyFileFromRawToOthers(getApplicationContext(), R.raw.shape_predictor_68_face_landmarks, targetPath);
        }
        // Init
        if (mFaceDet == null) {
            mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());
        }

        Log.d(TAG, "Image path: " + imgPath);
        final List<VisionDetRet> faceList = mFaceDet.detect(imgPath);
        if (faceList != null && faceList.size() > 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ivImg.setImageDrawable(drawRect(imgPath, faceList, Color.GREEN));
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "No face", Toast.LENGTH_SHORT).show();
                }
            });
        }

        dismissDialog();
    }

    protected BitmapDrawable drawRect(String path, List<VisionDetRet> results, int color) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        Bitmap bm = BitmapFactory.decodeFile(path, options);
        android.graphics.Bitmap.Config bitmapConfig = bm.getConfig();
        // set default bitmap config if none
        if (bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        // resource bitmaps are imutable,
        // so we need to convert it to mutable one
        bm = bm.copy(bitmapConfig, true);
        int width = bm.getWidth();
        int height = bm.getHeight();
        // By ratio scale
        float aspectRatio = bm.getWidth() / (float) bm.getHeight();

        final int MAX_SIZE = 512;
        int newWidth = MAX_SIZE;
        int newHeight = MAX_SIZE;
        float resizeRatio = 1;
        newHeight = Math.round(newWidth / aspectRatio);
        if (bm.getWidth() > MAX_SIZE && bm.getHeight() > MAX_SIZE) {
            Log.d(TAG, "Resize Bitmap");
            bm = getResizedBitmap(bm, newWidth, newHeight);
            resizeRatio = (float) bm.getWidth() / (float) width;
            Log.d(TAG, "resizeRatio " + resizeRatio);
        }

        // Create canvas to draw
        Canvas canvas = new Canvas(bm);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.STROKE);

        // Loop result list
        //for (VisionDetRet ret : results)
        {
            VisionDetRet ret = results.get(0);
            if (Math.abs(ret.getRight()-ret.getLeft()) > 0 && Math.abs(ret.getBottom()-ret.getTop()) > 0) {
                Rect bounds = new Rect();
                bounds.left = (int) (ret.getLeft() * resizeRatio);
                bounds.top = (int) (ret.getTop() * resizeRatio);
                bounds.right = (int) (ret.getRight() * resizeRatio);
                bounds.bottom = (int) (ret.getBottom() * resizeRatio);
                canvas.drawRect(bounds, paint);
            }
            // Get landmark
            ArrayList<Point> landmarks = ret.getFaceLandmarks();
            String jsonString = JSON.toJSONString(landmarks);
            Logger.t(TAG).e("landmarks: " + jsonString);

            //JNIUtils.testParseJson(jsonString);

//            List<Landmark> mLandmarks = new ArrayList<>();
//            for (Point point : landmarks) {
//                int pointX = (int) (point.x * resizeRatio);
//                int pointY = (int) (point.y * resizeRatio);
//                canvas.drawCircle(pointX, pointY, 2, paint);
//
//                Landmark landmark = new Landmark();
//                landmark.setX(pointX);
//                landmark.setY(pointY);
//                mLandmarks.add(landmark);
//            }
//
//            JSONObject jsonObject = new JSONObject();
//            jsonObject.put("landmark", mLandmarks);
//            jsonString = jsonObject.toJSONString();
//            Logger.t(TAG).e("new landmarks: " + jsonString);
//            String fileName = FileUtils.getMD5(mImgPath) + ".txt";
//            FileUtils.writeFileData(mContext, fileName, jsonString);

            String fileName = getFilesDir().getAbsolutePath() + "/" + FileUtils.getMD5(mImgPath) + ".txt";
            try {
                int i = 0;
                FileWriter writer = new FileWriter(fileName);
                for (Point point : landmarks) {
                    int pointX = (int) (point.x * resizeRatio);
                    int pointY = (int) (point.y * resizeRatio);
                    canvas.drawCircle(pointX, pointY, 2, paint);

                    String landmark = String.valueOf(pointX) + " " + String.valueOf(pointY) + "\n";
                    Logger.t(TAG).e("write landmark[" + String.valueOf(i) + "]: " + landmark);
                    i++;
                    writer.write(landmark);
                }
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
                Logger.t(TAG).e(e.toString());
            }
        }

        return new BitmapDrawable(getResources(), bm);
    }

//    protected BitmapDrawable drawRect(String path, ArrayList<Point> landmarks, int color) {
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inSampleSize = 1;
//        Bitmap bm = BitmapFactory.decodeFile(path, options);
//        android.graphics.Bitmap.Config bitmapConfig = bm.getConfig();
//        // set default bitmap config if none
//        if (bitmapConfig == null) {
//            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
//        }
//        // resource bitmaps are imutable,
//        // so we need to convert it to mutable one
//        bm = bm.copy(bitmapConfig, true);
//        int width = bm.getWidth();
//        int height = bm.getHeight();
//        // By ratio scale
//        float aspectRatio = bm.getWidth() / (float) bm.getHeight();
//
//        final int MAX_SIZE = 512;
//        int newWidth = MAX_SIZE;
//        int newHeight = MAX_SIZE;
//        float resizeRatio = 1;
//        newHeight = Math.round(newWidth / aspectRatio);
//        if (bm.getWidth() > MAX_SIZE && bm.getHeight() > MAX_SIZE) {
//            Log.d(TAG, "Resize Bitmap");
//            bm = getResizedBitmap(bm, newWidth, newHeight);
//            resizeRatio = (float) bm.getWidth() / (float) width;
//            Log.d(TAG, "resizeRatio " + resizeRatio);
//        }
//
//        // Create canvas to draw
//        Canvas canvas = new Canvas(bm);
//        Paint paint = new Paint();
//        paint.setColor(color);
//        paint.setStrokeWidth(2);
//        paint.setStyle(Paint.Style.STROKE);
//
//        for (Point point : landmarks) {
//            int pointX = (int) (point.x * resizeRatio);
//            int pointY = (int) (point.y * resizeRatio);
//            canvas.drawCircle(pointX, pointY, 2, paint);
//        }
//
//        return new BitmapDrawable(getResources(), bm);
//    }

    protected BitmapDrawable drawRectWithLandmark(String path, List<Landmark> landmarks, int color) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        Bitmap bm = BitmapFactory.decodeFile(path, options);
        android.graphics.Bitmap.Config bitmapConfig = bm.getConfig();
        // set default bitmap config if none
        if (bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        // resource bitmaps are imutable,
        // so we need to convert it to mutable one
        bm = bm.copy(bitmapConfig, true);
        int width = bm.getWidth();
        int height = bm.getHeight();
        // By ratio scale
        float aspectRatio = bm.getWidth() / (float) bm.getHeight();

        final int MAX_SIZE = 512;
        int newWidth = MAX_SIZE;
        int newHeight = MAX_SIZE;
        float resizeRatio = 1;
        newHeight = Math.round(newWidth / aspectRatio);
        if (bm.getWidth() > MAX_SIZE && bm.getHeight() > MAX_SIZE) {
            Log.d(TAG, "Resize Bitmap");
            bm = getResizedBitmap(bm, newWidth, newHeight);
            resizeRatio = (float) bm.getWidth() / (float) width;
            Log.d(TAG, "resizeRatio " + resizeRatio);
        }

        // Create canvas to draw
        Canvas canvas = new Canvas(bm);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.STROKE);

        for (Landmark point : landmarks) {
            int pointX = (int) (point.getX() * resizeRatio);
            int pointY = (int) (point.getY() * resizeRatio);
            canvas.drawCircle(pointX, pointY, 2, paint);
        }

        return new BitmapDrawable(getResources(), bm);
    }

    protected Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        return Bitmap.createScaledBitmap(bm, newWidth, newHeight, true);
    }

    @UiThread
    protected void showDialog() {
        mDialog = ProgressDialog.show(MainActivity.this, "Wait", "Face Detecting...", true);
    }

    @UiThread
    protected void dismissDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    private void showProcessResult(int[] resultPixes, int w, int h) {
        Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        //Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        result.setPixels(resultPixes, 0, w, 0, 0, w, h);
        ivImg.setImageBitmap(result);
    }

    private void showOriginalImage() {
        if (mImgPath != null) {
            File file = new File(mImgPath);
            Picasso.with(mContext).load(file)
                    .fit().centerCrop()
                    .into(ivImg);
        } else {
            Toast.makeText(mContext, "Image Path is null", Toast.LENGTH_SHORT).show();
        }
    }

    private void doGrayScale() {
        Bitmap bitmap = BitmapUtils.getViewBitmap(ivImg);
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int [] resultPixes = JNIUtils.doGrayScale(pix, w, h);
        showProcessResult(resultPixes, w, h);
    }

    private void doEdgeDetection() {
        Bitmap bitmap = BitmapUtils.getViewBitmap(ivImg);
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int [] resultPixes = JNIUtils.doEdgeDetection(pix, w, h);
        showProcessResult(resultPixes, w, h);
    }

    private void doBinaryzation() {
        Bitmap bitmap = BitmapUtils.getViewBitmap(ivImg);
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int [] resultPixes = JNIUtils.doBinaryzation(pix, w, h);
        showProcessResult(resultPixes, w, h);
    }

    private String searchFiles() {
        String result = "";
        //File[] files = new File(String.valueOf(mContext.getFilesDir())).listFiles();
        File[] files = new File(Environment.getExternalStorageDirectory().getPath() + "/dlib").listFiles();
        for (File file : files) {
            result += file.getPath() + "\n";
        }
        if (result.equals("")){
            result = "找不到文件!!";
        }
        return result;
    }

    @Click({R.id.iv_img, R.id.btn_detect, R.id.btn_reset, R.id.btn_gray, R.id.btn_binary, R.id.btn_edge, R.id.btn_file})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_img:
                startFileManager();
                break;
            case R.id.btn_detect:
                detectFace();
                break;
            case R.id.btn_reset:
                showOriginalImage();
                break;
            case R.id.btn_gray:
                doGrayScale();
                break;
            case R.id.btn_binary:
                doBinaryzation();
                break;
            case R.id.btn_edge:
                doEdgeDetection();
                break;
            case R.id.btn_file:
                //JNIUtils.testReadFile(mContext.getFilesDir().getAbsolutePath() + "/");
                //Logger.t(TAG).e("searchFiles: \n" + searchFiles());
                String path = Environment.getExternalStorageDirectory().getPath() + "/dlib/";
                String result = JNIUtils.averageFaceTest(path);
                if (result != null) {
                    File file = new File(result);
                    if (file.exists()) {
                        Picasso.with(mContext).load(file)
                                .into(ivImg);
                    } else {
                        Toast.makeText(mContext, "cannot create average face", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(mContext, "cannot create average face", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
