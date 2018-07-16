package com.mrk.mrkvoice;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

//import com.huawei.asrdemo.util.ResultInfo;
//import com.huawei.asrdemo.util.StoragePermission;
//import com.huawei.asrdemo.util.Utils;
import com.huawei.hiai.asr.AsrConstants;    //参数常量的定义类
import com.huawei.hiai.asr.AsrError;        //错误码的定义类
import com.huawei.hiai.asr.AsrEvent;
import com.huawei.hiai.asr.AsrListener;     //错误码的定义类
import com.huawei.hiai.asr.AsrRecognizer;   //加载语音识别类

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    public static final String AUDIO_SOURCE_TYPE_RECORDER = "recorder";
    public static final String AUDIO_SOURCE_TYPE_USER = "user";
    public static final String AUDIO_SOURCE_TYPE_FILE = "file";
    private static final String DEBUG_FILE_PATH = "dbgFilePath";

    private static final int END_AUTO_TEST = 0;
    private final static int INIT_ENGINE = 1;
    private static final int NEXT_FILE_TEST = 2;
    private static final int WRITE_RESULT_SD = 3;
    private static final int DELAYED_SATRT_RECORD = 4;
    private static final int DELAYED_STOP_RECORD = 5;
    private static final int DELAYED_WRITE_PCM = 6;

    private long startTime;
    private long endTime;
    private long waitTime;

    private AsrRecognizer mAsrRecognizer;
    private TextView showResult;
    private EditText input_result;
    private CheckBox right;
    private CheckBox wrong;
    private Button autoTest;
    private Button stopListeningBtn;
    private Button writePcmBtn;
    private Button cancelListeningBtn;
    private Button startRecord;

    private String mResult;
    private String writeResult;
    private boolean isAutoTest = true;
    private boolean isAutoTestEnd = false;
    private boolean isWritePcm = false;
    private int fileTotalCount;
    private int count = 0;

    private List<String> resultList = new ArrayList<>();
    private MyAsrListener mMyAsrListener = new MyAsrListener();
    private List<String> pathList = new ArrayList<>();
    private List<String> writePcmList = new ArrayList<>();

    private String TEST_HIAI_PATH = "/storage/emulated/0/Android/data/com.huawei.hiai";
    private String TEST_FILES_PATH = "/storage/emulated/0/Android/data/com.huawei.hiai/files";
    private String TEST_FILE_PATH = "/storage/emulated/0/Android/data/com.huawei.hiai/files/test";
    private String TEST_RESULT_FILE_PATH = "/storage/emulated/0/Android/data/com.huawei.hiai/files/result";
    private String TEST_PCM_PATH = "/storage/emulated/0/Android/data/com.huawei.hiai/files/pcm";

    /**********************************************************/

    /**
     * 调用过程如下：
     * (1)初始化AsrRecognizer类
     * (2)在线更新词图（此功能暂时未发布）
     * (3)设置引擎参数开始识别
     * (4)开始识别后，可以向SDK发送停止或取消事件
     *
     * 限制与约束：
     * (1)支持的输入文件格式有wav或pcm
     * (2)支持国内各地域普通话，不支持其他语种
     *（3）输入时长不能超过20s
     *（4）采样率固定且默认值是16000
     *（5）多线程调用：由于Android同一时刻只能有一个录音线程在，故本特性不支持多实例
     *（6）语音识别引擎的调用必须在UI的主线程中，且必须调用init和destroy
     * */

    /**********************************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //StoragePermission.getAllPermission(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        makeResDir();
        initView();
        if (isSupportAsr()) {
            initEngine(AsrConstants.ASR_SRC_TYPE_RECORD);
        } else {
            Log.e(TAG, "not support asr!");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() ");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() ");

        reset();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        destroyEngine();
        mAsrRecognizer = null;
    }

    private boolean isSupportAsr() {
        PackageManager packageManager = getPackageManager();

        try {
            PackageInfo packageInfo = packageManager.getPackageInfo("com.huawei.hiai", 0);
            Log.d(TAG, "Engine versionName: " + packageInfo.versionName + " ,versionCode: " + packageInfo.versionCode);
            if (packageInfo.versionCode <= 801000300) {
                return false;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }

        return true;
    }

    private void makeResDir() {
        File root_test = new File(TEST_HIAI_PATH);
        File files_test = new File(TEST_FILES_PATH);
        File test = new File(TEST_FILE_PATH);
        File result = new File(TEST_RESULT_FILE_PATH);
        File pcm = new File(TEST_PCM_PATH);

        if (!root_test.exists()) {
            root_test.mkdir();
        }
        if (!files_test.exists()) {
            files_test.mkdir();
        }
        if (!test.exists()) {
            test.mkdir();
        }
        if (!result.exists()) {
            result.mkdir();
        }
        if (!pcm.exists()) {
            pcm.mkdir();
        }

        Log.d(TAG, "onCreate: " + TEST_FILE_PATH + "==" + TEST_RESULT_FILE_PATH + "==" + TEST_PCM_PATH);
    }

    private void initView() {
        Log.d(TAG, "initView() ");

        cancelListeningBtn = (Button) findViewById(R.id.button_cacelListening);
        writePcmBtn = (Button) findViewById(R.id.button_writePcm);
        autoTest = (Button) findViewById(R.id.auto_test);
        stopListeningBtn = (Button) findViewById(R.id.button_stopListening);
        startRecord = (Button) findViewById(R.id.start_record);
        right = (CheckBox) findViewById(R.id.result_right);
        wrong = (CheckBox) findViewById(R.id.result_wrong);
        input_result = (EditText) findViewById(R.id.input_result);
        Button commit = (Button) findViewById(R.id.submit);
        showResult = (TextView) findViewById(R.id.start_record_show);

        cancelListeningBtn.setOnClickListener(this);
        writePcmBtn.setOnClickListener(this);
        autoTest.setOnClickListener(this);
        stopListeningBtn.setOnClickListener(this);
        startRecord.setOnClickListener(this);
        commit.setOnClickListener(this);

        right.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    wrong.setChecked(false);
                }
            }
        });
        wrong.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    right.setChecked(false);
                }
            }
        });
    }

    /**
     * srcType: ASR_SRC_TYPE_FILE - 语音输入来自文件, ASR_SRC_TYPE_RECORD - 语音输入来自mic录入, ASR_SRC_TYPE_PCM - pcm.
     */
    private void initEngine(int srcType) {
        Log.d(TAG, "initEngine() ");

        // 创建引擎, 必须在UI的主线程中调用引擎
        mAsrRecognizer = AsrRecognizer.createAsrRecognizer(this);
        Intent initIntent = new Intent();
        initIntent.putExtra(AsrConstants.ASR_AUDIO_SRC_TYPE, srcType);
        if (mAsrRecognizer != null) {
            //初始化引擎
            mAsrRecognizer.init(initIntent, mMyAsrListener);
        }
    }

    private void destroyEngine() {
        Log.d(TAG, "destroyEngine() ");

        if (mAsrRecognizer != null) {
            mAsrRecognizer.destroy();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_cacelListening:
                cancelListening();
                break;
            case R.id.button_writePcm:
                //writePcm();
                testPcm2();
                break;
            case R.id.auto_test:
                autoTest();
                break;
            case R.id.button_stopListening:
                stopListening();
                break;
            case R.id.start_record:
                startRecord();
                break;
            case R.id.submit:
//                submit();
                break;
        }
    }

    /**
     * 取消语音识别
     */
    private void cancelListening() {
        Log.d(TAG, "cancelListening() ");

        startRecord.setEnabled(true);

        //取消识别
        if (mAsrRecognizer != null) {
            mAsrRecognizer.cancel();
        }
    }

    /**
     * 写入PCM数据流，进行语音识别
     */
    private void writePcm() {
        Log.d(TAG, "writePcm() ");

        writePcmList.clear();
        getFilePath(TEST_PCM_PATH);

        if (writePcmList.size() == 0) {
            Toast.makeText(this, "请放入PCM文件", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isWritePcm) {
            //引擎释放
            if (mAsrRecognizer != null) {
                mAsrRecognizer.destroy();
            }
        }

        mHandler.sendEmptyMessageDelayed(DELAYED_WRITE_PCM, 1000);
    }

    /**
     * 写入PCM数据流，进行语音识别
     **/
    public void writePcm2(byte[] pcmData) {
        Log.d(TAG, "writePcm2");

        if (pcmData == null || pcmData.length == 0) {
            Log.d(TAG, "writePcm2 pcmData null");
            return;
        }

        if (!isWritePcm) {
            // 引擎释放
            if (mAsrRecognizer != null) {
                mAsrRecognizer.destroy();
            }

            initEngine(AsrConstants.ASR_SRC_TYPE_PCM);
        }
        isWritePcm = true;
        startListening(AsrConstants.ASR_SRC_TYPE_PCM, null);

        try {
            // 如果是从第三方获取到的数据，需要将获取的数据通过writePcm下发给引擎
            mAsrRecognizer.writePcm(pcmData, pcmData.length);
        } catch (Exception e) {
            Log.e(TAG, "writePcm2 writePcm: " + e.getMessage());
        }
    }

    private void testPcm2() {
        AudioRecordThread audioRecordThread = new AudioRecordThread();
        audioRecordThread.start();

        byte[] pcmData = audioRecordThread.getPcmData();
        while (true) {
            writePcm2(pcmData);
        }
    }

    /**
     * 通过递归得到当前文件夹里所有的文件数量和路径
     *
     * @param path
     * @return
     */
    public int getFilePath(String path) {
        Log.d(TAG, "getFilePath: " + path);

        int sum = 0;
        try {
            File file = new File(path);
            File[] list = file.listFiles();
            Log.d(TAG, "list size: " + list.length);
            if (list == null) {
                Log.d(TAG, "getFilePath: fileList is null!");
                return 0;
            }

            for (int i = 0; i < list.length; i++) {
                if (list[i].isFile()) {
                    String[] splitPATH = list[i].toString().split("\\.");
                    if (splitPATH[splitPATH.length - 1].equals("pcm") ||
                            splitPATH[splitPATH.length - 1].equals("wav")) {
                        sum++;
                        pathList.add(list[i].toString());
                        writePcmList.add(list[i].toString());
                    }
                } else {
                    sum += getFilePath(list[i].getPath());
                }
            }
        } catch (NullPointerException ne) {
            Toast.makeText(this, "找不到指定路径！", Toast.LENGTH_SHORT).show();
        }

        return sum;
    }

    private void autoTest() {
        Log.d(TAG, "autoTest() ");

        pathList.clear();
        fileTotalCount = getFilePath(TEST_FILE_PATH);
        Log.d(TAG, "fileTotalCount: " + pathList.toString());

        if (pathList.size() <= 0) {
            Toast.makeText(MainActivity.this, "请放入需要测试语音文件！", Toast.LENGTH_SHORT).show();
            return;
        }

        isAutoTest = true;

        //引擎释放
        if (mAsrRecognizer != null) {
            mAsrRecognizer.destroy();
        }

        mHandler.sendEmptyMessageDelayed(INIT_ENGINE, 1000);
    }

    /**
     * 停止语音识别
     */
    private void stopListening() {
        Log.d(TAG, "stopListening() ");

        //停止识别
        if (mAsrRecognizer != null) {
            mAsrRecognizer.stopListening();
        }
    }

    private void startRecord() {
        Log.d(TAG, "startRecord() ");

        isAutoTest = false;

        startRecord.setEnabled(false);
        showResult.setText("识别中：");

        mHandler.sendEmptyMessage(DELAYED_SATRT_RECORD);
    }

//    private void submit() {
//        boolean isRight = right.isChecked();
//        boolean isWrong = wrong.isChecked();
//        if (TextUtils.isEmpty(mResult) && (!isRight && !isWrong)) {
//            Toast.makeText(this, "请单次说话测试并标注是否正确后点击确认！", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        String audioPath = getWavName();
//        ResultInfo resultInfo = new ResultInfo();
//        resultInfo.setWavName(audioPath);
//        Log.d(TAG, "submit: ");
//
//        if (isRight) {
//            Log.d(TAG, "right.isChecked() " + right.isChecked());
//            writeResult = mResult;
//            resultInfo.setLable(writeResult);
//            resultInfo.setCorrect("TRUE");
//        }
//        if (isWrong) {
//            Log.d(TAG, "wrong.isChecked() " + wrong.isChecked());
//            writeResult = mResult;
//            resultInfo.setLable(input_result.getText().toString().trim());
//            resultInfo.setCorrect("FALSE");
//        }
//
//        resultInfo.setRec(writeResult);
//        Utils.writeNewName(resultInfo, TEST_RESULT_FILE_PATH, "/result.txt");
//    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d(TAG, "handleMessage: " + msg.what);

            switch (msg.what) {
                case END_AUTO_TEST:
                    initEngine(AsrConstants.ASR_SRC_TYPE_RECORD);
                    startListening(-1, null);
                    isAutoTestEnd = false;
                    isWritePcm = false;
                    break;
                case INIT_ENGINE:
                    handleInitEngine();
                    break;
                case NEXT_FILE_TEST:
                    handleNextFileTest();
                    break;
                case WRITE_RESULT_SD:
                    showResult.setText("批量测试结束");
                    isAutoTestEnd = true;
                    setBtEnabled(true);
                    resultList.clear();
                    break;
                case DELAYED_SATRT_RECORD:
                    if (isAutoTestEnd || isWritePcm) {
                        if (mAsrRecognizer != null) {
                            mAsrRecognizer.destroy();
                        }
                        mHandler.sendEmptyMessageDelayed(END_AUTO_TEST, 300);
                    } else {
                        startListening(-1, null);
                    }
                    break;
                case DELAYED_STOP_RECORD:
                    break;
                case DELAYED_WRITE_PCM:
                    handleWritePcm();
                    break;
                default:
                    break;
            }
        }
    };

    private void handleNextFileTest() {
        if (isAutoTest) {
            if (count < fileTotalCount) {
                Log.d(TAG, "handleMessage: " + count + " path :" + pathList.get(count));
                startListening(AsrConstants.ASR_SRC_TYPE_FILE, pathList.get(count));
            }
        }
    }

//    private void handleWriteResultSD() {
//        Log.d(TAG, "writeAllResultInSD: count:" + count + "filePath :" + pathList.toString() + "waitTime :" + waitTime);
//        String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
//        Utils.writeNewName(resultList, TEST_RESULT_FILE_PATH, time + "autoTestResult.txt");
//        showResult.setText("批量测试结束");
//        isAutoTestEnd = true;
//        setBtEnabled(true);
//        resultList.clear();
//    }

    public void setBtEnabled(boolean isEnabled) {
        autoTest.setEnabled(isEnabled);
        stopListeningBtn.setEnabled(isEnabled);
        cancelListeningBtn.setEnabled(isEnabled);
        startRecord.setEnabled(isEnabled);
        writePcmBtn.setEnabled(isEnabled);
    }

    private void handleInitEngine() {
        if (isAutoTest) {
            initEngine(AsrConstants.ASR_SRC_TYPE_FILE);
            setBtEnabled(false);
            Log.d(TAG, "handleMessage: " + count + " path :" + pathList.get(count));
            startListening(AsrConstants.ASR_SRC_TYPE_FILE, pathList.get(count));
        }
    }

    /**
     * 开始语音识别
     */
    private void startListening(int srcType, String filePath) {
        Log.d(TAG, "startListening() " + "src_type:" + srcType);

        if (count == 0) {
            startTime = getTimeMillis();
        }

        Intent intent = new Intent();
        //设置前端静音检测时间
        intent.putExtra(AsrConstants.ASR_VAD_FRONT_WAIT_MS, 4000);
        //设置后端静音检测时间
        intent.putExtra(AsrConstants.ASR_VAD_END_WAIT_MS, 5000);
        //设置超时时间
        intent.putExtra(AsrConstants.ASR_TIMEOUT_THRESHOLD_MS, 20000);
        if (srcType == AsrConstants.ASR_SRC_TYPE_FILE) {
            Log.d(TAG, "startListening() :filePath= " + filePath);
            intent.putExtra(AsrConstants.ASR_SRC_FILE, filePath);
        }
        if (mAsrRecognizer != null) {
            //设置引擎参数开始识别
            mAsrRecognizer.startListening(intent);
        }
    }

    private void handleWritePcm() {
        Log.d(TAG, "handleWritePcm() ");
        if (!isWritePcm) {
            initEngine(AsrConstants.ASR_SRC_TYPE_PCM);
        }
        isWritePcm = true;
        startListening(AsrConstants.ASR_SRC_TYPE_PCM, null);

        ByteArrayOutputStream bos = null;
        BufferedInputStream in = null;
        try {
            File file = new File(writePcmList.get(0));
            if (!file.exists()) {
                throw new FileNotFoundException("file not exists");
            }
            bos = new ByteArrayOutputStream((int) file.length());
            in = new BufferedInputStream(new FileInputStream(file));
            int buf_size = 1280;
            byte[] buffer = new byte[buf_size];
            int len = 0;
            while (-1 != (len = in.read(buffer, 0, buf_size))) {
                bos.reset();
                bos.write(buffer, 0, len);
                //如果是从第三方获取到的数据，需要将获取的数据通过writePcm下发给引擎
                mAsrRecognizer.writePcm(bos.toByteArray(), bos.toByteArray().length);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (bos != null) {
                    bos.close();
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    //在线更新词图（暂未发布）
//    private void updateLexicon() {
//        Log.d(TAG, "updateLexicon() ");
//
//        Intent intent = new Intent();
//        intent.putExtra("key", "updateLexicon");
//        if (mAsrRecognizer != null) {
    // 没有该函数
//            mAsrRecognizer.updateLexicon(intent, mMyAsrListener);
//        }
//    }

//    public String getWavName() {
//        File file = new File(TEST_PCM_PATH);
//        File[] files = file.listFiles();
//        if (files == null) {
//            Toast.makeText(this, "PCM文件或文件夹不存在！", Toast.LENGTH_SHORT).show();
//            return null;
//        }
//        if (files.length == 0) {
//            Log.i(TAG, "getWavName() : file not found!");
//            return null;
//        }
//        return files[files.length - 1].toString();
//    }

    public long getTimeMillis() {
        long time = System.currentTimeMillis();
        return time;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.i(TAG, "onBackPressed() : finish()");
        finish();
    }

    private void reset() {
        cancelListening();
        setBtEnabled(true);
        showResult.setText("");
        input_result.setText("");
    }

    private class MyAsrListener implements AsrListener {

        /**
         * 识别引擎初始化完毕，回调该函数，并返回错误码
         */
        @Override
        public void onInit(Bundle params) {
            Log.d(TAG, "onInit() called with: params = [" + params + "]");
        }

        /**
         * 引擎自录音模式下，录音开启后，用户开始说话回调该函数
         */
        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech() called");
        }

        /**
         * 返回当前输入语音的能量
         */
        @Override
        public void onRmsChanged(float rmsdB) {
            Log.d(TAG, "onRmsChanged() called with: rmsdB = [" + rmsdB + "]");
        }

        /**
         *
         * */
        @Override
        public void onBufferReceived(byte[] buffer) {
            Log.d(TAG, "onBufferReceived() called with: buffer = [" + buffer + "]");
        }

        /**
         * VAD后端人声检测，即引擎检测到用户已经说完了
         */
        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech: ");
        }

        /**
         * 当识别过程出错，触发此回调，错误码说明见错误码表格
         * <p>
         * error code
         * 0 - 成功，1 - 网络超时，2 - 其他网络错误，3 - 录音相关错误，
         * 4 - 服务器相关错误，5 - 其他客户端错误，6 - 没有声音，7 - 未匹配识别结果，
         * 8 - 识别引擎忙，9 - 客户端权限不足，10 - 入参错误，11 - 未知错误，
         * 12 - 服务端权限不足，13 - 引擎模型路径获取失败，14 - AIEngine设置项关闭
         */
        @Override
        public void onError(int error) {
            Log.d(TAG, "onError() called with: error = [" + error + "]");

            if (error == AsrError.SUCCESS) {
                return;
            }
            if (error == AsrError.ERROR_CLIENT_INSUFFICIENT_PERMISSIONS) {
                Toast.makeText(getApplicationContext(), "请在设置中打开麦克风权限!", Toast.LENGTH_LONG).show();
            }

            setBtEnabled(true);
        }

        /**
         * 当识别结束，触发此回调
         */
        @Override
        public void onResults(Bundle results) {
            Log.d(TAG, "onResults() called with: results = [" + results + "]");

            endTime = getTimeMillis();
            waitTime = endTime - startTime;
            mResult = getOnResult(results, AsrConstants.RESULTS_RECOGNITION);

            stopListening();

            if (isAutoTest) {
                resultList.add(pathList.get(count) + "\t" + mResult);

                Log.d(TAG, "isAutoTest: " + waitTime + "count :" + count);

                if (count == fileTotalCount - 1) {
                    resultList.add("\n\nwaittime:\t" + waitTime + "ms");
                    mHandler.sendEmptyMessage(WRITE_RESULT_SD);

                    Log.d(TAG, "waitTime: " + waitTime + "count :" + count);

                    count = 0;
                } else {
                    Log.d(TAG, "isAutoTest: else" + waitTime + "count :" + count);

                    count += 1;
                    mHandler.sendEmptyMessageDelayed(NEXT_FILE_TEST, 1000);
                }
            } else {
                startRecord.setEnabled(true);
            }
        }

        /**
         * 识别过程中，触发此回调，每解码一个chunk，回调一次，可用于实时上屏
         */
        @Override
        public void onPartialResults(Bundle partialResults) {
            Log.d(TAG, "onPartialResults() called with: partialResults = [" + partialResults + "]");

            getOnResult(partialResults, AsrConstants.RESULTS_PARTIAL);
        }

        /**
         * 识别结束回调
         */
        @Override
        public void onEnd() {

        }

        /**
         *
         * */
        private String getOnResult(Bundle partialResults, String key) {
            Log.d(TAG, "getOnResult() called with: getOnResult = [" + partialResults + "]");

            String json = partialResults.getString(key);
            final StringBuilder sb = new StringBuilder();
            try {
                JSONObject result = new JSONObject(json);
                JSONArray items = result.getJSONArray("result");
                for (int i = 0; i < items.length(); i++) {
                    String word = items.getJSONObject(i).getString("word");
                    double confidences = items.getJSONObject(i).getDouble("confidence");
                    sb.append(word);

                    Log.d(TAG, "asr_engine: result str " + word);
                    Log.d(TAG, "asr_engine: confidence " + String.valueOf(confidences));
                }

                Log.d(TAG, "getOnResult: " + sb.toString());

                showResult.setText(sb.toString());
            } catch (JSONException exp) {
                Log.w(TAG, "JSONException: " + exp.toString());
            }

            return sb.toString();
        }

        /**
         * 上报各种事件
         */
        @Override
        public void onEvent(int eventType, Bundle params) {
            Log.d(TAG, "onEvent() called with: eventType = [" + eventType + "], params = [" + params + "]");

            switch (eventType) {
//                case AsrEvent.EVENT_PERMISSION_RESULT:
//                    int result = params.getInt(AsrEvent.EVENT_KEY_PERMISSION_RESULT, PackageManager.PERMISSION_DENIED);
//                    if (result != PackageManager.PERMISSION_GRANTED) {
//                        reset();
//                    }
                default:
                    return;
            }
        }
    }

}
