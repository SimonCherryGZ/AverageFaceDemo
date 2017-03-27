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
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.orhanobut.logger.Logger;
import com.squareup.picasso.MemoryPolicy;
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

import me.nereo.multi_image_selector.MultiImageSelector;
import me.nereo.multi_image_selector.MultiImageSelectorActivity;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();
    private final static int FILE_REQUEST_CODE = 233;
    private final static int REQUEST_IMAGE = 666;

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
    @ViewById(R.id.btn_save)
    Button btnSave;

    private Context mContext;
    //private Unbinder unbinder;

    private FaceDet mFaceDet;
    private ProgressDialog mDialog;
    private String mImgPath;
    private int pathSize = 0;

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
                showOriginalImage();
            } else if (requestCode == REQUEST_IMAGE) {
                // 获取返回的图片列表
                List<String> paths = data.getStringArrayListExtra(MultiImageSelectorActivity.EXTRA_RESULT);
                pathSize = paths.size();
                Toast.makeText(mContext, "get " + String.valueOf(pathSize + " pics"), Toast.LENGTH_SHORT).show();

                ivImg.setImageResource(R.drawable.add_icon);
                handleImageSelectorResult(paths);
            }
        }
    }

    private void startFileManager() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, FILE_REQUEST_CODE);
    }

    private void handleImageSelectorResult(List<String> paths) {
        String[] pathArray = new String[paths.size()];
        List<String> processList = new ArrayList<>();

        for (int i=0; i<paths.size(); i++) {
            String path = paths.get(i);
            Logger.t(TAG).e("get path: " + path);
            pathArray[i] = path;

            if (!isLandmarkTxtExist(path)) {
                Logger.t(TAG).e("landmark not exist, need to create");
                //createLandMarkTxt(path);
                processList.add(path);
            }
        }

        if (processList.size() == 0) {
            doAverageFace(pathArray);
        } else {
            createLandMarkTxt(processList, pathArray);
        }
    }

    private boolean isLandmarkTxtExist(String imgPath) {
        String fileName = getFilesDir().getAbsolutePath() + "/" + FileUtils.getMD5(imgPath) + ".txt";
        Logger.t(TAG).e("txt path: " + fileName);

        File file = new File(fileName);
        return file.exists();
    }

    @Background
    protected void createLandMarkTxt(final String imgPath) {
        showDialog(imgPath);

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
            writeLandmark2Txt(imgPath, faceList);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //ivImg.setImageDrawable(drawRect(imgPath, faceList, Color.GREEN));
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

    @Background
    protected void createLandMarkTxt(List<String> imgPaths, final String[] pathArray) {
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

        dismissDialog();

        for (String imgPath : imgPaths) {
            showDialog(imgPath);
            Log.d(TAG, "Image path: " + imgPath);
            final List<VisionDetRet> faceList = mFaceDet.detect(imgPath);
            if (faceList != null && faceList.size() > 0) {
                writeLandmark2Txt(imgPath, faceList);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //ivImg.setImageDrawable(drawRect(imgPath, faceList, Color.GREEN));
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

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doAverageFace(pathArray);
            }
        });

    }

    private void writeLandmark2Txt(String path, List<VisionDetRet> results) {
        VisionDetRet ret = results.get(0);
        // Get landmark
        ArrayList<Point> landmarks = ret.getFaceLandmarks();
        String jsonString = JSON.toJSONString(landmarks);
        Logger.t(TAG).e("landmarks: " + jsonString);

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

        String fileName = getFilesDir().getAbsolutePath() + "/" + FileUtils.getMD5(path) + ".txt";
        try {
            int i = 0;
            FileWriter writer = new FileWriter(fileName);
            for (Point point : landmarks) {
                int pointX = (int) (point.x * resizeRatio);
                int pointY = (int) (point.y * resizeRatio);

                String landmark = String.valueOf(pointX) + " " + String.valueOf(pointY) + "\n";
                //Logger.t(TAG).e("write landmark[" + String.valueOf(i) + "]: " + landmark);
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

    private void detectFace() {
        if (mImgPath != null) {
            String fileName = getFilesDir().getAbsolutePath() + "/" + FileUtils.getMD5(mImgPath) + ".txt";
            Logger.t(TAG).e("txt path: " + fileName);

            File file = new File(fileName);
            if (file.exists()) {
                try {
                    FileReader fileReader = new FileReader(fileName);
                    BufferedReader br = new BufferedReader(fileReader);
                    final List<Landmark> mLandmarks = new ArrayList<>();
                    int i = 0;
                    for(String str; (str = br.readLine()) != null; ) {  // 这里不能用while(br.readLine()) != null) 因为循环条件已经读了一条
                        //Logger.t(TAG).e("read landmark[" + String.valueOf(i) + "]: " + str);
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
    protected void showDialog(String path) {
        mDialog = ProgressDialog.show(MainActivity.this, "Wait", "正在处理 " + path, true);
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
                    .memoryPolicy(MemoryPolicy.NO_CACHE)
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

    private void saveBitmapToFile() {
        Bitmap bitmap = BitmapUtils.getViewBitmap(ivImg);

        final String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + "AverageFaceDemo";

        long timestamp = System.currentTimeMillis();
        String fileName = "averageFace" + timestamp + ".png";

        FileUtils.saveBitmapToFile(getApplicationContext(), bitmap, filePath, fileName);
        callMediaScanner(filePath, fileName);
    }

    private void callMediaScanner(String filePath, String fileName) {
        String path = filePath + "/" + fileName;
        MediaScanner mediaScanner = new MediaScanner(getApplicationContext());
        String[] filePaths = new String[]{path};
        String[] mimeTypes = new String[]{MimeTypeMap.getSingleton().getMimeTypeFromExtension("png")};
        mediaScanner.scanFiles(filePaths, mimeTypes);
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

    private void averageFaceTest() {
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
    }

    private void startImageSelector() {
        MultiImageSelector.create(mContext)
                .showCamera(false) // 是否显示相机. 默认为显示
                .count(99) // 最大选择图片数量, 默认为9. 只有在选择模式为多选时有效
                .multi() // 多选模式, 默认模式;
                .start(this, REQUEST_IMAGE);
    }

    @Background
    protected void doAverageFace(String[] pathArray) {
        showDialog();
        String result = JNIUtils.doAverageFace(pathArray);

        if (result != null) {
            final File file = new File(result);
            if (file.exists()) {
                mImgPath = result;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Picasso.with(mContext).load(file)
                                //.fit().centerCrop()
                                .memoryPolicy(MemoryPolicy.NO_CACHE)
                                .into(ivImg);
                    }
                });

            } else {
                Toast.makeText(mContext, "cannot create average face", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(mContext, "cannot create average face", Toast.LENGTH_SHORT).show();
        }

        dismissDialog();
    }

    @Click({R.id.iv_img, R.id.btn_detect, R.id.btn_reset, R.id.btn_gray, R.id.btn_binary, R.id.btn_edge, R.id.btn_file, R.id.btn_save})
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
                //averageFaceTest();
                startImageSelector();
                break;
            case R.id.btn_save:
                saveBitmapToFile();
                break;
        }
    }
}
