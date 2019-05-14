package com.example.administrator.hasset;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.zhy.autolayout.AutoLayoutActivity;

import org.xutils.common.util.IOUtil;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AutoLayoutActivity {

    private Button button;//拍摄按钮
    private SurfaceView surfaceView;//预览界面
    private Camera mCamera;//相机
    private Camera.Size mBestPictureSize;//最佳拍摄尺寸
    private Camera.Size mBestPreviewSize;//最佳拍摄尺寸
    private boolean mIsSurfaceReady;//预览页面是否准备完成
    private boolean waitTakePicture = false;//是否等待拍照完成
    private OrientationEventListener mOrEventListener; // 设备方向监听器
    private Boolean mCurrentOrientation = true; // 当前设备方向 横屏false,竖屏true
    private ImageView pic_img;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();//初始化视图
        initData();//初始化数据
        initListener();//初始化监听
    }

    /**
     * 初始化监听
     */
    private void initListener() {
        // 拍摄按钮点击
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("mIsSurfaceReady",mIsSurfaceReady+"");
//        if(!mIsSurfaceReady){
//            surfaceView.removeCallbacks(null);
//            surfaceView.getHolder().addCallback(new HolderListener());
//        }

        // 确保能够获取到SurfaceView的大小
        surfaceView.post(new Runnable() {
            @Override
            public void run() {
                //打开相机
                openCamera();
            }
        });
    }



    @Override
    protected void onPause() {
        super.onPause();
        stopPreview();
        closeCamera();
    }

    private void initView() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //全屏显示
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // 全屏显示
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // 当此窗口为用户可见时，保持设备常开，并保持亮度不变。
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        button = (Button) this.findViewById(R.id.button_picture);
        surfaceView = (SurfaceView) this.findViewById(R.id.surfaceView_myFirst);
        pic_img = (ImageView) this.findViewById(R.id.pic_img);
        startOrientationChangeListener();
    }

    private void initData() {
        //设置分辨率
//        surfaceView.getHolder().setFixedSize(320,240);
        //设置预览回调
        surfaceView.getHolder().addCallback(new HolderListener());
    }

    private void openCamera() {
        if (mCamera == null) {
            try {
                mCamera = Camera.open();
            } catch (RuntimeException e) {
                if ("Fail to connect to camera service".equals(e.getMessage())) {
                    //提示无法打开相机，请检查是否已经开启权限
                } else if ("Camera initialization failed".equals(e.getMessage())) {
                    //提示相机初始化失败，无法打开
                } else {
                    //提示相机发生未知错误，无法打开
                }
                finish();
                return;
            }
        }
        initCamera();

    }

    private void initCamera() {
        //获取相机参数
        final Camera.Parameters parameters = mCamera.getParameters();
        //设置拍摄后的照片格式
        parameters.setPictureFormat(ImageFormat.JPEG);
        //设置自动对焦
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 连续对焦

        //设置旋转90度，竖屏拍照，所以还需要对Camera旋转90度
        if(mCurrentOrientation){
            parameters.setRotation(90);
        }else{
            parameters.setRotation(0);
        }
        //获取预览界面宽高比
        final float ratio =  ((float)surfaceView.getMeasuredWidth() / (float)surfaceView.getMeasuredHeight());
        //获取相机支持的宽高比,此为预览画面
        List<Camera.Size> previewSize = parameters.getSupportedPreviewSizes();
        //获取相机支持的图片尺寸
        List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
        for (Camera.Size item : pictureSizes ) {
//            System.out.println("支持的宽："+item.width+"支持的高："+item.height );
        }
        //获取最佳图片尺寸,使图片和surfaceView的比例接近
        if (mBestPictureSize == null) {
            mBestPictureSize = getPictureSize(pictureSizes, ratio);
        }
        //获取最佳预览尺寸
        if(mBestPreviewSize == null){
            mBestPreviewSize = getPictureSize(previewSize, ratio);
        }
        //设置照片尺寸,可以改变分辨率
        parameters.setPictureSize(mBestPictureSize.width, mBestPictureSize.height);
        Log.e("camera", "图片宽高------->" + "宽："+ mBestPictureSize.width + "|" + "高"+mBestPictureSize.height);

        // 设置相机预览的尺寸
        parameters.setPreviewSize(mBestPreviewSize.width, mBestPreviewSize.height);
        Log.e("camera", "预览宽高------->" + "宽："+ mBestPreviewSize.width + "|" + "高"+mBestPreviewSize.height);

        //设置预览界面surfaceView的比例，与相机的预览尺寸比例一样，才不会导致预览出来的surfaceView结果是变形的
        ViewGroup.LayoutParams layoutParams = surfaceView.getLayoutParams();
        float fl = (float) mBestPreviewSize.width / (float) mBestPreviewSize.height;
        float f2 = (float) surfaceView.getHeight() / (float)surfaceView.getWidth();

        if(fl > f2){
            layoutParams.height = (int) (surfaceView.getWidth() * fl);
        }else{
            layoutParams.width = (int) (surfaceView.getHeight() / fl);
        }
//
//        //因为我们是旋转了相机的，所以计算的时候，对surfaceView的比例是宽除以高，而对Camera.Size则是高除以宽。
//        layoutParams.height = (int) (surfaceView.getWidth() * fl + 0.5);
        Log.e("camera", "实际预览宽高------->" + "宽："+ layoutParams.width + "|" + "高"+layoutParams.height);
        surfaceView.setLayoutParams(layoutParams);
        mCamera.setParameters(parameters);

        //预览界面是否初始化完成
        if (mIsSurfaceReady) {
            startPreview();
        }
    }

    /**
     * 获取最佳预览尺寸
     * @param previewSize
     * @param defaultPreviewSize
     * @param ratio
     * @return
     */
    private Camera.Size getBestPreviewSize(List<Camera.Size> previewSize, Camera.Size defaultPreviewSize, float ratio) {
        Iterator<Camera.Size> iterator = previewSize.iterator();
        DisplayMetrics dm = new DisplayMetrics();
        //获取屏幕信息
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenWidth = dm.widthPixels;
        int screenHeigh = dm.heightPixels;
        float q = screenWidth * 1000/(1600 * screenHeigh / 1280);
        float p = 5000;
        while (iterator.hasNext()){
            Camera.Size size = iterator.next();
            if(size.width * size.height < 900000){
                continue;
            }
            float w = size.width * 1000 / size.height;
            if(Math.abs(w - q) < Math.abs(p - q)){
                p = w;
                defaultPreviewSize.width = size.width;
                defaultPreviewSize.height = size.height;
            }
        }
        return defaultPreviewSize;
    }

    /**
     * 开启相机预览
     */
    private void startPreview() {
        if (mCamera == null) {
            return;
        }
        try {
            mCamera.setPreviewDisplay(surfaceView.getHolder());
            mCamera.setDisplayOrientation(90);
            mCamera.startPreview();
            // 连续对焦需要加上
//            mCamera.cancelAutoFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 请求自动对焦
     */
    private void requestFocus() {
        if (mCamera == null || waitTakePicture) {
            return;
        }
        mCamera.autoFocus(null);
    }

    /**
     * 拍照
     */
    public void takePicture() {
        if (mCamera == null || waitTakePicture) {
            return;
        }
        requestFocus();// 设置自动对焦，必须在相机初始化完成
        waitTakePicture = true;

            //拍照方法
            mCamera.takePicture(null, null, new Camera.PictureCallback() {

                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    processPicture(data);
                    waitTakePicture = false;
                    // 拍照之后，预览的展示会停止。如果想继续拍照，需要先再调用startPreview()
                    //预览界面是否初始化完成
                    if (mIsSurfaceReady) {
                        startPreview();
                    }
                }
            });


    }

    /**
     * 停止预览
     */
    private void stopPreview() {
        if (mCamera == null) {
            return;
        }
        try {
            mCamera.setPreviewDisplay(null);
            mCamera.setDisplayOrientation(0);
            mCamera.stopPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭相机
     */
    private void closeCamera() {
        if (mCamera == null) {
            return;
        }
        mCamera.cancelAutoFocus();
        stopPreview();
        mCamera.release();
        mCamera = null;
    }

    /**
     * 处理图片数据
     *
     * @param data
     */
    private void processPicture(byte[] data) {

        //sd卡路径
        String absolutePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File fileDir = new File(absolutePath + "/testfile");
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }
        File file = new File(fileDir.getPath() + "/" + System.currentTimeMillis() + ".jpg");

        if(!mCurrentOrientation){// 如果是横屏拍摄
            Bitmap bitmap=null;
            if(isRight){
                bitmap = ImgUtils.rotateBitmap(270, ImgUtils.Bytes2Bimap(data));
            }else{
                bitmap = ImgUtils.rotateBitmap(90, ImgUtils.Bytes2Bimap(data));
            }
            pic_img.setImageBitmap(bitmap);
            ImgUtils.data2file(bitmap,file.getAbsolutePath());
            Log.e("文件的路径",file.getAbsolutePath());
            return;
        }


        // 竖屏拍摄的处理，图片不需要旋转
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(file);
            os.write(data);
            os.flush();
            Toast.makeText(this, "拍摄完成", Toast.LENGTH_LONG).show();
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            pic_img.setImageBitmap(bitmap);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                IOUtil.closeQuietly(os);
            }
        }
    }

    /**
     * 获取最佳的尺寸
     *
     * @param pictureSizes
     * @param minRatio
     * @return
     */
    private Camera.Size getBestPictureSize(List<Camera.Size> pictureSizes, Camera.Size defaultPictureSize, float minRatio) {
        Iterator<Camera.Size> iterator = pictureSizes.iterator();
        DisplayMetrics dm = new DisplayMetrics();
        //获取屏幕信息
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenWidth = dm.widthPixels;
        int screenHeigh = dm.heightPixels;

        Log.e("camera", "screenWidth------->" + screenWidth + "===screenHeigh --->" + screenHeigh);
        int bestX = 0;
        int bestY = 0;
        int diff = Integer.MAX_VALUE;

        while (iterator.hasNext()){
            Camera.Size size = iterator.next();
            int newX = size.width;
            int newY = size.height;

            int newDiff = Math.abs(newX - screenWidth) + Math.abs(newY - screenHeigh);
            if (newDiff == 0) {
                bestX = newX;
                bestY = newY;
                break;
            } else if (newDiff < diff) {
                bestX = newX;
                bestY = newY;
                diff = newDiff;
            }
        }

        if (bestX > 0 && bestY > 0) {
            defaultPictureSize.width = bestX;
            defaultPictureSize.height = bestY;
        }else{
            defaultPictureSize.width = 1920;
            defaultPictureSize.height = 1080;
        }

        return defaultPictureSize;
    }

    /**
     * 相机预览回调
     */
    public class HolderListener implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mIsSurfaceReady = true;
            // 在surfaceView回调成功后再打开相机，避免出现预览黑屏，解决小米手机重后台打开无法打开预览
            startPreview();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mIsSurfaceReady = false;
        }
    }


    private boolean isRight;//按钮在右边拍摄

    //横竖屏监听
    private final void startOrientationChangeListener() {
        mOrEventListener = new OrientationEventListener(this) {

            @Override
            public void onOrientationChanged(int rotation) {
                if (((rotation >= 0) && (rotation <= 45)) || (rotation >= 315)
                        || ((rotation >= 135) && (rotation <= 225))) {// portrait
                    mCurrentOrientation = true;
//                    Log.e("Camera", "竖屏");
                } else if ( (rotation > 225) && (rotation < 315)){// 90度
                    mCurrentOrientation = false;
                    isRight = true ;
//                    Log.e("Camera", "横屏");
                }else if((rotation > 45) && (rotation < 135)){// 270度
                    mCurrentOrientation = false;
                    isRight = false;
                }
            }
        };
        mOrEventListener.enable();
    }


    /**
     * 获取pictureSize的合适值
     * @param pictureSizes      size集合
     * @param rate      传入的宽高比
     * @return
     */
    public Camera.Size getPictureSize(List<Camera.Size> pictureSizes, float rate) {

        DisplayMetrics dm = new DisplayMetrics();
        //获取屏幕信息
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;

        ArrayList<Camera.Size> equalRateList = new ArrayList();

        Collections.sort(pictureSizes, new CameraSizeComparator());   //统一以升序的方式排列

        for (Camera.Size s : pictureSizes) {
            if ((s.width > screenHeight) && equalRate(s, rate)) {        //合适参数的判断条件， 大于我们传入的最小高度且宽高比的差值不能超过0.2
                equalRateList.add(s);
            }
        }
        if (equalRateList.size() <= 0) {                 //如果到最后也没有找到合适的，哪么就放宽条件去找
            return getBestSize(pictureSizes, rate);
        } else {

            int size = equalRateList.size();
            int index = size / 2;

            if(size % 2 == 0){
                index -= 1;
            }

            if(index < 0){
                index = 0;
            }

            return equalRateList.get(index);                 //返回找到的合适size
        }
    }

    /**
     * 遍历所有的size，找到和传入的宽高比的差值最小的一个
     * @param list
     * @param rate
     * @return
     */
    private Camera.Size getBestSize(List<Camera.Size> list, float rate) {
        float previewDisparity = 100;
        int index = 0;
        for (int i = 0; i < list.size(); i++) {
            Camera.Size cur = list.get(i);
            float prop = (float) cur.width / (float) cur.height;
            if (Math.abs(rate - prop) < previewDisparity) {
                previewDisparity = Math.abs(rate - prop);
                index = i;
            }
        }
        return list.get(index);
    }


    private boolean equalRate(Camera.Size s, float rate) {
        float r = (float) (s.height) / (float) (s.width);
        return Math.abs(r - rate) <= 0.2;   //传入的宽高比和size的宽高比的差不能大于0.2,要尽量的和传入的宽高比相同
    }

    private class CameraSizeComparator implements Comparator<Camera.Size> {
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            if (lhs.width == rhs.width) {
                return 0;
            } else if (lhs.width > rhs.width) {
                return 1;
            } else {
                return -1;
            }
        }

    }

}
