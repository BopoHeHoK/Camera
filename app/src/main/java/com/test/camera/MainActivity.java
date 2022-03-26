package com.test.camera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;
import android.widget.ImageView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CAMERA = 68958;
    private ImageView preview;
    private LineChart chartRed, chartGreen, chartBlue;
    ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    YUVtoRGB translator = new YUVtoRGB();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        chartRed = findViewById(R.id.chartRed);
        chartGreen = findViewById(R.id.chartGreen);
        chartBlue = findViewById(R.id.chartBlue);


        preview = findViewById(R.id.preview);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        } else {
            initializeCamera();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSION_REQUEST_CAMERA && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeCamera();
        }
    }

    private void initializeCamera(){
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    final int[] i = {0};
                    final ArrayList<Entry> entriesRed = new ArrayList<>();
                    final ArrayList<Entry> entriesGreen = new ArrayList<>();
                    final ArrayList<Entry> entriesBlue = new ArrayList<>();
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                    ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                            .setTargetResolution(new Size(1080, 2340))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build();

                    CameraSelector cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build();
                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(MainActivity.this), new ImageAnalysis.Analyzer() {

                        @Override
                        public void analyze(@NonNull ImageProxy image) {
                            @SuppressLint("UnsafeOptInUsageError") Image img = image.getImage();
                            Bitmap bitmap = translator.translateYUV(img, MainActivity.this);
                            int size = bitmap.getWidth() * bitmap.getHeight();
                            int[] pixels = new int[size];
                            bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

                            int[] numbs = new int[3];
                            for (int j = 0; j < size; j++) {
                                int color = pixels[j];
                                numbs[0] = color >> 16 & 0xff;
                                numbs[1] = color >> 8 & 0xff;
                                numbs[2] = color & 0xff;
                            }
                            entriesRed.add(new Entry(i[0], numbs[0]));
                            entriesGreen.add(new Entry(i[0], numbs[1]));
                            entriesBlue.add(new Entry(i[0], numbs[2]));

                            System.out.println(i[0] + " " + numbs[0] + " " + numbs[1] + " " + numbs[2]);
                            i[0]++;

                            LineDataSet datasetRed = new LineDataSet(entriesRed, "График R");
                            datasetRed.setDrawFilled(true);
                            datasetRed.setColor(Color.RED);
                            datasetRed.setCircleColor(Color.RED);
                            datasetRed.setCircleHoleColor(Color.RED);
                            datasetRed.setFillColor(Color.RED);
                            LineData dataRed = new LineData(datasetRed);
                            chartRed.setData(dataRed);
                            chartRed.invalidate();

                            LineDataSet datasetGreen = new LineDataSet(entriesGreen, "График G");
                            datasetGreen.setDrawFilled(true);
                            datasetGreen.setColor(Color.GREEN);
                            datasetGreen.setCircleColor(Color.GREEN);
                            datasetGreen.setCircleHoleColor(Color.GREEN);
                            datasetGreen.setFillColor(Color.GREEN);
                            LineData dataGreen = new LineData(datasetGreen);
                            chartGreen.setData(dataGreen);
                            chartGreen.invalidate();

                            LineDataSet datasetBlue = new LineDataSet(entriesBlue, "График B");
                            datasetBlue.setDrawFilled(true);
                            datasetBlue.setColor(Color.BLUE);
                            datasetBlue.setCircleColor(Color.BLUE);
                            datasetBlue.setCircleHoleColor(Color.BLUE);
                            datasetBlue.setFillColor(Color.BLUE);
                            LineData dataBlue = new LineData(datasetBlue);
                            chartBlue.setData(dataBlue);
                            chartBlue.invalidate();

                            preview.setRotation(image.getImageInfo().getRotationDegrees());
                            preview.setImageBitmap(bitmap);
                            image.close();
                        }
                    });
                    cameraProvider.bindToLifecycle(MainActivity.this, cameraSelector, imageAnalysis);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

}