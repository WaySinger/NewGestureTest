package com.lenovo.way.newgesturetest;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.concurrent.ExecutorService;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,DataReceive {

    private ImageView mIvDepth, mIvIr; // 显示image
    private Bitmap mDepthBmp, mIrBmp;
    private ImageView mClosePreview; // 关闭程序

    private TextView mTvFps, mTvFingerCnt, mTvRange, mTvExp, mTvEventCode; // 实时参数
    private TextView mTvSetFps, mTvSetExp, mTvSetRange; // 设置后参数

    private Button mBtStartImg, mBtStopImg, mBtStartInfo, mBtStopInfo, mBtSaveimage; // Btn Image & Info
    private Button mBtSetFps, mBtSetExp, mBtSetRange, mBtGetSetInfo; // Btn set & get SetInfo
    private Button mBtStartDump, mBtStopDump; // Btn start & stop Dump
    private EditText mEtExp, mEtFps, mEtSwitch; // EditText input SetInfo

    private final String SOCKET_NAME = "gestured";
    private final String TAG = "LenovoPreviewApp";
    private String file = "/mnt/sdcard/Documents/GesturePreview/"; // image 保存路径

    private int[] resolution = {224, 172};
//    private int DepImgIndex = 0, IrImgIndex = 0;
//    private int len = 172 * 224 * 2;
//    private int connetTime = 1;
//    private byte[] sendData;
//    private byte[] byteString = new byte[4];
//    private boolean isConnected = false;
    public boolean isSaveImage = false;
    private boolean delFile = false;

//    private LocalSocket client;
//    private LocalSocketAddress address;
//    private ExecutorService mCachedThreadPool;
//    private AcceptSocketThread mAcceptSocketThread;

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

//    private byte START_IMG = 0x02;
//    private byte CLOSE_SOCKET = 0x03;
//    private byte STOP_IMG = 0x04;
//    private byte SET_SHORT_LONG_SWITCH = 0x05;
//    private byte GET_SHORT_LONG_SWITCH = 0x06;
//    private byte SET_FPS = 0x07;
//    private byte GET_FPS = 0x08;
//    private byte SET_EXP_TIME = 0x09;
//    private byte GET_EXP_TIME = 0x0a;
//    private byte START_INFO = 0x0b;
//    private byte STOP_INFO = 0x0c;
//    private byte GROUP_PREVIEW = 0x0e;
//    private byte START_DUMP_FRAME = 0x12;
//    private byte STOP_DUMP_FRAME = 0x13;

    private DataReceive dataReceive;
    private GestureUtil gestureUtil ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // findView
        initView();
        // permission
        ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, 100);

        gestureUtil = new GestureUtil();

        gestureUtil.useDataReceive(this);

        gestureUtil.socketClient();
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "----------onResume");
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "----------onStart");
        // gestureUtil.socketClient();
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "----------onStop");
        super.onStop();
        gestureUtil.closePreview();
        // finish();
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "----------onDestroy");
        super.onDestroy();
        gestureUtil.closePreview();
        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_startimg:
                Log.i(TAG, "onClick+++++++++start Img !");
                gestureUtil.startImg();

                break;

            case R.id.bt_stopimg:
                Log.e(TAG, "onClick--------stop Img !");
                gestureUtil.stopImg();
                break;

            case R.id.bt_startinfo:
                Log.i(TAG, "onClick+++++++++start Info");
                gestureUtil.startInfo();
                break;

            case R.id.bt_stopinfo:
                Log.e(TAG, "onClick-----stop Info");
                gestureUtil.stopInfo();
                break;

            case R.id.bt_setfps:
                Log.i(TAG, "onClick-----setfps----");
                //TODO: 设置帧率
                String fps = mEtFps.getText().toString().trim();
                if (fps.length() <= 0) {
                    Toast.makeText(this, "输入不能为空！", Toast.LENGTH_SHORT).show();
                } else {
                    int value = Integer.parseInt(fps);
                    if (value > 0 && value <= 30) {
                        gestureUtil.setFps(value);

                    }
                }
                break;

            case R.id.bt_setexposure:
                Log.i(TAG, "onClick-----setexposure----");
                //TODO: 设置曝光时间
                String exp = mEtExp.getText().toString().trim();
                if (exp.length() <= 0) {
                    Toast.makeText(this, "输入不能为空！", Toast.LENGTH_SHORT).show();
                } else {
                    int value = Integer.parseInt(exp);
                    if (value > 0 && value <= 1500) {
                        gestureUtil.setExp(value);

                    }
                }
                break;

            case R.id.bt_setSwitch:
                Log.i(TAG, "onClick--------bt_setSwitch-------");
                //TODO: 设置长短距
                String strSwitch = mEtSwitch.getText().toString().trim();
                if (strSwitch.length() <= 0) {
                    Toast.makeText(this, "输入不能为空！", Toast.LENGTH_SHORT).show();
                } else {
                    int value = Integer.parseInt(strSwitch);
                    if (value == 0 || value == 1) {
                        gestureUtil.setRange(value);

                    }
                }
                break;

            case R.id.bt_saveimage:
                Log.i(TAG, "onClick---------saveimage!!!!!!!");
                if (!isSaveImage) {
                    delFiles();
                } else {
                    isSaveImage = false;
                    gestureUtil.setIsSaveImage(false);
                    mBtSaveimage.setBackground(getResources().getDrawable(R.drawable.btn_bg_off));
                    setButtonText("startSaveImage");
                }
                break;

            case R.id.btn_get_setInfo:
                Log.i(TAG, "onClick=========get Set Info ");
                gestureUtil.getSetInfo();

                break;

            case R.id.bt_start_dump:
                Log.i(TAG, "onClick=========start Dump");
                gestureUtil.startDump();

                mBtStartDump.setText("Dumping ...");
                mBtStartDump.setBackground(getResources().getDrawable(R.drawable.btn_bg_on));
                Toast.makeText(MainActivity.this,"Don't forget to stop dump !",Toast.LENGTH_SHORT).show();
                break;

            case R.id.bt_stop_dump:
                Log.i(TAG, "onClick=========stop Dump");
                gestureUtil.stopDump();

                mBtStartDump.setText("Start Dump");
                mBtStartDump.setBackground(getResources().getDrawable(R.drawable.btn_bg_off));
                break;

            case R.id.iv_close_preview:
                Log.v(TAG, "click close ======onDestroy");
                gestureUtil.closePreview();

                finish();
                break;
        }
    }



    private void setButtonText(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBtSaveimage.setText(text);
            }
        });
    }


    private void initView() {
        mIvDepth = (ImageView) findViewById(R.id.iv_depth);
        mIvIr = (ImageView) findViewById(R.id.iv_ir);
        mClosePreview = (ImageView) findViewById(R.id.iv_close_preview);

        mTvFingerCnt = (TextView) findViewById(R.id.tv_finger_cnt);
        mTvEventCode = (TextView) findViewById(R.id.tv_eventcode);
        mTvFps = (TextView) findViewById(R.id.tv_fps);
        mTvRange = (TextView) findViewById(R.id.tv_range);
        mTvExp = (TextView) findViewById(R.id.tv_exposure);

        mTvSetFps = (TextView) findViewById(R.id.tv_set_fps);
        mTvSetExp = (TextView) findViewById(R.id.tv_set_exp);
        mTvSetRange = (TextView) findViewById(R.id.tv_set_range);

        mEtExp = (EditText) findViewById(R.id.et_exposure);
        mEtFps = (EditText) findViewById(R.id.et_fps);
        mEtSwitch = (EditText) findViewById(R.id.et_switch);

        mBtStartImg = (Button) findViewById(R.id.bt_startimg);
        mBtSaveimage = (Button) findViewById(R.id.bt_saveimage);
        mBtStopImg = (Button) findViewById(R.id.bt_stopimg);
        mBtStartInfo = (Button) findViewById(R.id.bt_startinfo);
        mBtStopInfo = (Button) findViewById(R.id.bt_stopinfo);
        mBtSetFps = (Button) findViewById(R.id.bt_setfps);
        mBtSetExp = (Button) findViewById(R.id.bt_setexposure);
        mBtSetRange = (Button) findViewById(R.id.bt_setSwitch);
        mBtGetSetInfo = (Button) findViewById(R.id.btn_get_setInfo);
        mBtStartDump = (Button) findViewById(R.id.bt_start_dump);
        mBtStopDump = (Button) findViewById(R.id.bt_stop_dump);

        mBtStartImg.setOnClickListener(this);
        mBtStopImg.setOnClickListener(this);
        mBtStartInfo.setOnClickListener(this);
        mBtStopInfo.setOnClickListener(this);
        mBtSaveimage.setOnClickListener(this);
        mBtSetExp.setOnClickListener(this);
        mBtSetFps.setOnClickListener(this);
        mBtSetRange.setOnClickListener(this);
        mBtGetSetInfo.setOnClickListener(this);
        mBtStartDump.setOnClickListener(this);
        mBtStopDump.setOnClickListener(this);
        mClosePreview.setOnClickListener(this);

        createBitmap();
    }


    private void createBitmap() {
        // createBitmap (int width, int height, Bitmap.Config config)
        // 根据参数创建新位图

        if (mDepthBmp == null) {
            mDepthBmp = Bitmap.createBitmap(resolution[0], resolution[1],
                    Bitmap.Config.ARGB_8888);
        }

        if (mIrBmp == null) {
            mIrBmp = Bitmap.createBitmap(resolution[0], resolution[1],
                    Bitmap.Config.ARGB_8888);
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e(TAG, "onKeyDown");
        // Log.d("GesturePreviewActivity", "event.getKeyCode():" + event.getKeyCode());
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:  // 屏蔽Back键
                return false;
//            case KeyEvent.KEYCODE_HOME:  // 屏蔽Home键
//                Log.e(TAG,"Home……………………");
//                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void setGestureInfo(final int fps, final int exp, final int longshort, final int fingercnt, final int eventCode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvFingerCnt.setText("Finger : " + fingercnt);
                mTvEventCode.setText("Event : " + eventCode);
                mTvFps.setText("Fps : " + fps);
                mTvRange.setText("Range : " + longshort);
                mTvExp.setText("Exp : " + exp);
                if (eventCode != 0) {
                    // 设置个背景 大红色的总能看清了吧
                    mTvEventCode.setBackgroundColor(Color.RED);
                    Log.i(TAG,"************* eventCode = "+eventCode+" *************");
                } else {
                    mTvEventCode.setBackgroundColor(Color.WHITE);
                }
            }
        });
    }

    @Override
    public void setSetInfo(final int set_fps, final int set_exp, final int set_range) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvSetFps.setText("Set_Fps : " + set_fps);
                mTvSetExp.setText("Set_Exp : " + set_exp);
                mTvSetRange.setText("Set_Range : " + set_range);
            }
        });
    }

    @Override
    public void setDepthImageData(int[] imgData) {
        mDepthBmp.setPixels(imgData, 0, resolution[0], 0, 0, resolution[0], resolution[1]);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mIvDepth.setImageBitmap(Bitmap.createScaledBitmap(mDepthBmp, resolution[0], resolution[1], false));
            }
        });
    }

    @Override
    public void setIRImageData(int[] imgData) {
        mIrBmp.setPixels(imgData, 0, resolution[0], 0, 0, resolution[0], resolution[1]);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mIvIr.setImageBitmap(Bitmap.createScaledBitmap(mIrBmp, resolution[0], resolution[1], false));
            }
        });
    }

    @Override
    public void delFiles() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                delFile = deleteDirectory(file);
                if (delFile) {
                    isSaveImage = true;
                    gestureUtil.setIsSaveImage(true);
                    setButtonText("stopSaveImage");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mBtSaveimage.setBackground(getResources().getDrawable(R.drawable.btn_bg_on));
                            Toast.makeText(MainActivity.this,"Don't forget to stop save !",Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    isSaveImage = false;
                    gestureUtil.setIsSaveImage(false);
                    setButtonText("startSaveImage");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "删除文件失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }



    public boolean deleteDirectory(String filePath) {
        boolean flag = false;
//        if (!filePath.endsWith(File.separator)) {
//            filePath = filePath + File.separator;
//        }
        File dirFile = new File(filePath);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        flag = true;
        File[] files = dirFile.listFiles();
        if (files.length == 0) {
            return true;
        }
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                //  flag = deleteFile(files[i].getAbsolutePath());
                flag = files[i].delete();
                if (!flag)
                    break;
            } else {
                flag = deleteDirectory(files[i].getAbsolutePath());
                if (!flag) {
                    break;
                }
            }
        }
        if (!flag) {
            return false;
        }
        return true;
    }


}
