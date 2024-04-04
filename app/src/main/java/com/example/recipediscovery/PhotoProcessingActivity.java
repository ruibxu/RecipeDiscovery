package com.example.recipediscovery;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import com.example.recipediscovery.ml.Model;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PhotoProcessingActivity extends AppCompatActivity {

    String TAG = "Object detection";




    ImageView imageView;
    Bitmap imageBitmap;

    TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_processing);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        String photoURIString = getIntent().getStringExtra("PHOTO_URI");

        if (photoURIString != null) {
            Uri photoURI = Uri.parse(photoURIString);

            try {
                // Load the image from URI and set it to the ImageView
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), photoURI);
                imageBitmap = ImageDecoder.decodeBitmap(source);
                imageView.setImageBitmap(imageBitmap);
                getPrediction();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void getPrediction(){
        try {

            Bitmap bitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true);
            TensorImage image = TensorImage.fromBitmap(bitmap);

            Model model = Model.newInstance(this);

            Model.Outputs outputs = model.process(image);


            HashMap<String, Integer> objectCounts = new HashMap<>();

            StringBuilder stringBuilder = new StringBuilder();
            List<Model.DetectionResult> detectionResults = new ArrayList<>(outputs.getDetectionResultList());
            for (Model.DetectionResult detectionResult : detectionResults) {
                String category = detectionResult.getCategoryAsString();
                String text = category + ", " + Math.round(detectionResult.getScoreAsFloat() * 100) + "%";
                objectCounts.put(category, objectCounts.getOrDefault(category, 0) + 1);
                Log.d("Object Score", text);

            }


            for (Map.Entry<String, Integer> entry : objectCounts.entrySet()) {
                stringBuilder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }


            textView.setText(stringBuilder.toString());

            //debugPrint(detectionResultsList);
            Bitmap imgWithResult=drawBoundingBox(bitmap, detectionResults);
            imageView.setImageBitmap(imgWithResult);

            model.close();

        } catch (IOException e) {
            // TODO Handle the exception
        }
    }

    private Bitmap  drawBoundingBox(Bitmap bitmap, List<Model.DetectionResult> detectionResults) {
        Bitmap outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(outputBitmap);
        Paint pen = new Paint();
        pen.setTextAlign(Paint.Align.LEFT);

        for (Model.DetectionResult result : detectionResults) {
            String category = result.getCategoryAsString();
            // Draw bounding box
            pen.setColor(Color.RED);
            pen.setStrokeWidth(8F);
            pen.setStyle(Paint.Style.STROKE);
            RectF box = result.getLocationAsRectF();
            canvas.drawRect(box, pen);

            // Calculate the right font size
            pen.setStyle(Paint.Style.FILL_AND_STROKE);
            pen.setColor(Color.YELLOW);
            pen.setStrokeWidth(1F);

            pen.setTextSize(96F);
            Rect tagSize = new Rect();
            pen.getTextBounds(category, 0, category.length(), tagSize);
            float fontSize = pen.getTextSize() * box.width() / tagSize.width();

            // Adjust the font size so texts are inside the bounding box
            if (fontSize < pen.getTextSize()) pen.setTextSize(fontSize);

            float margin = (box.width() - tagSize.width()) / 2.0F;
            if (margin < 0F) margin = 0F;
            canvas.drawText(
                    category, box.left + margin,
                    box.top + tagSize.height() * 1F, pen
            );
        }
        return outputBitmap;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}