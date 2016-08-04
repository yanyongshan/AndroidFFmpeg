package com.wlanjie.ffmpeg.library;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.io.FileNotFoundException;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MainActivity extends Activity {

    private static final int COMPRESS = 0;
    private static final int CROP = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.rotation)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        getRotation("/sdcard/DCIM/Camera/compress.mp4");
                        getRotation("/sdcard/crop.mp4");
                    }
                });

        findViewById(R.id.crop)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        startRecoderVideoIntent(CROP);
                        crop("/sdcard/Download/a.mp4");
                    }
                });

        findViewById(R.id.compress)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        compress("/sdcard/Download/a.mp4");
                    }
                });
        findViewById(R.id.player)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        player("/sdcard/crop.mp4");
                    }
                });
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                FFmpeg.getInstance().setSurface(holder.getSurface());
                FFmpeg.getInstance().player("rtmp://live.hkstv.hk.lxdns.com/live/hks");
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    private void startRecoderVideoIntent(int requestCode) {
        Intent taskVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (taskVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(taskVideoIntent, requestCode);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CROP:
                crop(getIntent().getData().toString());
                break;
            case COMPRESS:
                break;
        }
    }

    private void getRotation(final String path) {
        Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try {
                    FFmpeg ffmpeg = FFmpeg.getInstance();
                    ffmpeg.openInput(path);
                    double rotation = ffmpeg.getRotation();
                    System.out.println("rotation = " + rotation);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {

                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
    }
    
    private void crop(final String path) {
        Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try {
                    long start = System.currentTimeMillis();
                    FFmpeg ffmpeg = FFmpeg.getInstance();
                    int result = ffmpeg.openInput(path);
                    if (result < 0) {
                        return;
                    }
                    double rotation = ffmpeg.getRotation();
                    final int width = ffmpeg.getVideoWidth();
                    final int height = ffmpeg.getVideoHeight();
                    int newWidth;
                    int newHeight;
                    if (rotation == 90) {
                        newWidth = height;
                        newHeight = width;
                    } else {
                        newWidth = width;
                        newHeight = height;
                    }
                    //裁剪视频正中间部分
                    result = ffmpeg.crop("/sdcard/crop.mp4", newWidth / 2 / 2, newHeight / 2 / 2 , newWidth / 2, newHeight / 2);
                    long end = System.currentTimeMillis();
                    System.out.println("time = " + ((end - start) / 1000) + " width = " + width + " height = " + height + " rotation = " + rotation);
                    if (result < 0) {
                        subscriber.onNext(false);
                    }
                    ffmpeg.release();
                    subscriber.onNext(true);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        if (aBoolean) {
                            Toast.makeText(MainActivity.this, "裁剪完成", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
    }
    
    private void compress(final String path) {
        Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try {
                    long start = System.currentTimeMillis();
                    FFmpeg ffmpeg = FFmpeg.getInstance();
                    int result = ffmpeg.openInput(path);
                    if (result < 0) {
                        Toast.makeText(MainActivity.this, "open input error", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int width = ffmpeg.getVideoWidth();
                    int height = ffmpeg.getVideoHeight();
                    double rotation = ffmpeg.getRotation();
                    System.out.println("width = " + width + " height = " + height);
                    int newWidth;
                    int newHeight;
                    if (rotation == 90) {
                        newWidth = height / 2;
                        newHeight = width / 2;
                    } else {
                        newWidth = width / 2;
                        newHeight = height / 2;
                    }
                    result = ffmpeg.compress("/sdcard/compress.mp4", 360, -1);
                    ffmpeg.release();
                    long end = System.currentTimeMillis();
                    if (result >= 0) {
                        subscriber.onNext(true);
                        subscriber.onCompleted();
                        System.out.println(((end - start) / 1000));
                    }
                    subscriber.onNext(false);
                } catch (Exception e) {
                    e.printStackTrace();
                    subscriber.onNext(false);
                }
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        if (aBoolean) {
                            Toast.makeText(MainActivity.this, "压缩完成", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
    }

    private void player(final String url) {
        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                FFmpeg ffmpeg = FFmpeg.getInstance();
                ffmpeg.player(url);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object o) {

                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {

                    }
                });
    }
}
