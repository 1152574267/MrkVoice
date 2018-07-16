package com.mrk.mrkvoice;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 音频录制线程
 */
public class AudioRecordThread extends Thread {
    private static final String TAG = AudioRecordThread.class.getSimpleName();

    private AudioRecord mAudioRecord;
    // 采样率固定且默认值是16000
    private int mSampleRate = 16000;
    private int mMinBufferSize;
    private byte[] sampleBuffer = new byte[1280];
    Deque<byte[]> mPcmDataDeque = new ArrayDeque<>();

    public byte[] getPcmData() {
        if (mPcmDataDeque.size() > 0) {
            return mPcmDataDeque.pop();
        }

        return null;
    }

    @Override
    public void run() {
        if (mSampleRate != 16000) {
            Log.e(TAG, "sampleRate not support.");
            return;
        }

        mMinBufferSize = AudioRecord.getMinBufferSize(mSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (AudioRecord.ERROR_BAD_VALUE == mMinBufferSize) {
            Log.e(TAG, "mMinBufferSize parameters are not supported by the hardware.");
            return;
        }

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, mSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, mMinBufferSize);
        if (null == mAudioRecord) {
            Log.e(TAG, "new AudioRecord failed.");
            return;
        }

        try {
            mAudioRecord.startRecording();
        } catch (IllegalStateException e) {
            Log.e(TAG, "startRecording failed.");
            return;
        }

        try {
            while (!Thread.currentThread().isInterrupted()) {
                int result = mAudioRecord.read(sampleBuffer, 0, 1280);
                if (result > 0) {
                    mPcmDataDeque.push(sampleBuffer);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "mAudioRecord read: " + e.getMessage());
        }

        mAudioRecord.stop();
        mAudioRecord.release();
        mAudioRecord = null;
    }

}
