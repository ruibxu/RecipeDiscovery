package com.example.recipediscovery;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.android.material.progressindicator.CircularProgressIndicator;


import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
public class PhotoProcessingActivity extends AppCompatActivity {

    String TAG = "Object detection";

    private OkHttpClient client;
    Bitmap imageBitmap;
    TextView textView;

    String base64Image;

    private RecyclerView recyclerView;

    Button backButton;

    private CircularProgressIndicator progressIndicator;

    private CardAdapter cardAdapter;

    String API_KEY = BuildConfig.API_KEY;

    private boolean isCloseButtonVisible = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_processing);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();

        textView = findViewById(R.id.processResult);
        progressIndicator = findViewById(R.id.progressIndicator);
        progressIndicator.setVisibility(View.VISIBLE);



        recyclerView = findViewById(R.id.processingRecyclerView);


        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);
        cardAdapter = new CardAdapter(new ArrayList<>(), isCloseButtonVisible);
        recyclerView.setAdapter(cardAdapter);



        backButton= findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PhotoProcessingActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();

            }
        });

        String photoURIString = getIntent().getStringExtra("PHOTO_URI");



        if (photoURIString != null) {
            Uri photoURI = Uri.parse(photoURIString);

            try {
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), photoURI);
                imageBitmap = ImageDecoder.decodeBitmap(source);
                base64Image = convertBitmapToBase64(imageBitmap);
                callAPI();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            displayMessage("No image found, please try again");
        }



    }

    private String convertBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }


    public void callAPI(){

        JSONObject requestBody = createRequestJSON();
        RequestBody body = RequestBody.create(requestBody.toString(),MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization","Bearer " + API_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback(){
            @Override
            public  void onFailure(@NonNull Call call, @NonNull IOException e){
                Log.d(TAG,"No");
                runOnUiThread(() -> displayMessage(e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG,"yes");
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        if (jsonArray.length() > 0) {
                            JSONObject firstChoice = jsonArray.getJSONObject(0);
                            JSONObject message = firstChoice.getJSONObject("message");
                            if (message.has("content")) {
                                String result = message.getString("content");
                                Log.d(TAG, result);
                                List<Recipe> recipeList = Utils.parseGeneratedText(result);
                                if (!recipeList.isEmpty()) {
                                    Log.d(TAG, recipeList.get(0).getName());
                                    runOnUiThread(() -> generateCard(recipeList));
                                } else {
                                    Log.e(TAG, "Recipe list is empty");
                                }
                            } else {
                                Log.e(TAG, "Key 'content' not found in message object");
                            }
                        } else {
                            Log.e(TAG, "JsonArray is empty");
                        }
                    } catch (JSONException e) {
                        runOnUiThread(() -> displayMessage(e.getMessage()));
                    }
                }
                else {
                    String errorMessage = response.body().string();
                    Log.e(TAG, "Error message: " + errorMessage);
                }
            }
        });
    }

    public void generateCard(List<Recipe> recipeList){
        progressIndicator.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        textView.setVisibility(View.GONE);
        cardAdapter.setRecipeList(recipeList);
        cardAdapter.notifyDataSetChanged();
    }

    public void displayMessage(String message){
        progressIndicator.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        textView.setVisibility(View.VISIBLE);
        textView.setText("Failed to load response due to "+message);
    }

    private JSONObject createRequestJSON(){
        JSONObject requestBody = new JSONObject();
        JSONArray messages = new JSONArray();


        try {
            requestBody.put("model","gpt-4-turbo" );
            requestBody.put("temperature", 0);

            JSONObject responseFormat = new JSONObject();
            responseFormat.put("type", "json_object");

            requestBody.put("response_format", responseFormat);

            JSONObject systemMsg = new JSONObject();
            JSONObject assistMsg = new JSONObject();


            systemMsg.put("role", "system");
            systemMsg.put("content", "You are a helpful cooking assistant that generate recipes based on a image of ingredients. Respond with 5 recipes includes the " +
                    "ingredients you identified in the image. Provide your answer in JSON Structure like this " +
                    "[\n" +
                    "    {\n" +
                    "        \"name\": \"<The name of the first recipe>\",\n" +
                    "        \"ingredients\": [\"<ingredients 1>\", \"<ingredients 2>\", \"<ingredients 3>\", \"<ingredients 4>\" ],\n" +
                    "        \"steps\": [\"<step 1>\", \"<step 2>\", \"<step 3>\", \"<step 4>\", \"<step 5>\", \"<step 6>\", \"<step 7>\"]\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"name\": \"<The name of the second recipe>\",\n" +
                    "        \"ingredients\": [\"<ingredients 1>\", \"<ingredients 2>\", \"<ingredients 3>\", \"<ingredients 4>\", \"<ingredients 5>\"  ],\n" +
                    "        \"steps\": [\"<step 1>\", \"<step 2>\", \"<step 3>\", \"<step 4>\", \"<step 5>\"]\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"name\": \"<The name of the third recipe>\",\n" +
                    "        \"ingredients\": [\"<ingredients 1>\", \"<ingredients 2>\", \"<ingredients 3>\", \"<ingredients 4>\" ],\n" +
                    "        \"steps\": [\"<step 1>\", \"<step 2>\", \"<step 3>\", \"<step 4>\", \"<step 5>\", \"<step 6>\", \"<step 7>\"]\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"name\": \"<The name of the fourth recipe>\",\n" +
                    "        \"ingredients\": [\"<ingredients 1>\", \"<ingredients 2>\", \"<ingredients 3>\" ],\n" +
                    "        \"steps\": [\"<step 1>\", \"<step 2>\", \"<step 3>\", \"<step 4>\", \"<step 5>\", \"<step 6>\"]\n" +
                    "    },\n" +
                    "    {\n" +
                    "        \"name\": \"<The name of the fifth recipe>\",\n" +
                    "        \"ingredients\": [\"<ingredients 1>\", \"<ingredients 2>\", \"<ingredients 3>\", \"<ingredients 4>\", \"<ingredients 5>\" ],\n" +
                    "        \"steps\": [\"<step 1>\", \"<step 2>\", \"<step 3>\", \"<step 4>\", \"<step 5>\", \"<step 6>\", \"<step 7>\", \"<step 8>\"]\n" +
                    "    }\n" +
                    "]");
            messages.put(systemMsg);


            assistMsg.put("role","assistant");
            assistMsg.put("content",
                    "{" +
                            "\"recipes\":[\n" +
                            "    {\n" +
                            "        \"name\": \"Chocolate Cake\",\n" +
                            "        \"ingredients\": [\"flour\", \"sugar\", \"cocoa powder\", \"baking soda\", \"salt\", \"eggs\", \"vegetable oil\", \"vanilla extract\", \"hot water\"],\n" +
                            "        \"steps\": [\"Preheat oven to 350°F (175°C) and grease a 9x13 inch baking pan.\", \"In a large bowl, mix together the flour, sugar, cocoa powder, baking soda, and salt.\", \"Add the eggs, vegetable oil, and vanilla extract to the dry ingredients and mix until well combined.\", \"Stir in the hot water until the batter is smooth.\", \"Pour the batter into the prepared baking pan.\", \"Bake in preheated oven for 35 to 40 minutes, or until a toothpick inserted into the center comes out clean.\", \"Allow the cake to cool in the pan for 10 minutes, then remove from pan and transfer to a wire rack to cool completely.\"]\n" +
                            "    },\n" +
                            "    {\n" +
                            "        \"name\": \"Vanilla Cupcakes\",\n" +
                            "        \"ingredients\": [\"flour\", \"sugar\", \"baking powder\", \"salt\", \"butter\", \"eggs\", \"milk\", \"vanilla extract\"],\n" +
                            "        \"steps\": [\"Preheat oven to 350°F (175°C) and line a cupcake pan with paper liners.\", \"In a medium bowl, mix together flour, sugar, baking powder, and salt.\", \"Add butter to the dry ingredients and mix until mixture resembles coarse sand.\", \"In a separate bowl, whisk together eggs, milk, and vanilla extract.\", \"Pour wet ingredients into dry ingredients and mix until smooth.\", \"Spoon batter into prepared cupcake pan.\", \"Bake in preheated oven for 18-20 minutes, or until a toothpick inserted into the center comes out clean.\", \"Allow cupcakes to cool in pan for a few minutes, then transfer to a wire rack to cool completely.\"]\n" +
                            "    }\n" +
                            "]" +
                            "}");

            messages.put(assistMsg);

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");

            JSONArray imageObjectArray = new JSONArray();
            JSONObject imageObject = new JSONObject();
            JSONObject imageUrlObject = new JSONObject();
            imageUrlObject.put("url", "data:image/jpeg;base64," + base64Image);

            imageObject.put("type", "image_url");
            imageObject.put("image_url", imageUrlObject);
            imageObjectArray.put(imageObject);

            userMsg.put("content",imageObjectArray);

            messages.put(userMsg);
            requestBody.put("messages",messages);

        } catch (JSONException e){
            e.printStackTrace();
        }
        return requestBody;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}