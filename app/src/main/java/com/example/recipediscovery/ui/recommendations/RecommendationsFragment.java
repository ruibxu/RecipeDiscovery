package com.example.recipediscovery.ui.recommendations;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.recipediscovery.BuildConfig;
import com.example.recipediscovery.CardAdapter;
import com.example.recipediscovery.CooldownManager;
import com.example.recipediscovery.R;
import com.example.recipediscovery.Recipe;
import com.example.recipediscovery.RecipeList;
import com.example.recipediscovery.RecommendationList;
import com.example.recipediscovery.Utils;
import com.example.recipediscovery.databinding.FragmentRecommendationsBinding;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RecommendationsFragment extends Fragment {

    private FragmentRecommendationsBinding binding;

    private String TAG = "recommendations";

    private CooldownManager cooldownManager;


    private OkHttpClient client;

    String API_KEY = BuildConfig.API_KEY;

    private List<Recipe> recommendationList = new ArrayList<>();
    private RecyclerView recyclerView;


    private boolean isCloseButtonVisible = false;
    private CircularProgressIndicator progressIndicator1;

    private CircularProgressIndicator progressIndicator2;

    private TextView suggestion;
    private TextView textView;

    private CardAdapter cardAdapter;

    private long lastClickTime = 0;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        RecommendationsViewModel dashboardViewModel =
                new ViewModelProvider(this).get(RecommendationsViewModel.class);

        binding = FragmentRecommendationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();

        suggestion = root.findViewById(R.id.suggestion);
        recyclerView = root.findViewById(R.id.recommendationsRecyclerView);
        textView = root.findViewById(R.id.recommendationMessage);


        progressIndicator1 = root.findViewById(R.id.progressIndicator1);
        progressIndicator1.setVisibility(View.VISIBLE);

        progressIndicator2 = root.findViewById(R.id.progressIndicator2);
        progressIndicator2.setVisibility(View.VISIBLE);

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);
        cardAdapter = new CardAdapter(recommendationList, isCloseButtonVisible);
        recyclerView.setAdapter(cardAdapter);


        cooldownManager = CooldownManager.getInstance(getContext());
        List<Recipe> recipes = RecipeList.getInstance(getContext()).getRecipeList();
        if (recipes.isEmpty()){
            progressIndicator1.setVisibility(View.GONE);
            suggestion.setVisibility(View.VISIBLE);
            suggestion.setText("You don't have any recipes in cook book for now. Discover more for nutrition recommendation!");
            progressIndicator2.setVisibility(View.GONE);
            textView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            textView.setText("You don't have a recipe list for now. Go discover recipes!");
        }
        else {
            if (!cooldownManager.isCooldownActive()) {

                callAPI(recipes);

            } else {
                List<Recipe> recommendationList = RecommendationList.getInstance(getContext()).getRecommendationList();
                String suggestionText = RecommendationList.getInstance(getContext()).getSuggestion();

                showSuggestion(suggestionText);
                showRecommendList(recommendationList);
            }
        }

        return root;
    }

    private void callAPI(List<Recipe> recipes){
        if (!recipes.isEmpty()) {
            JSONObject requestBody1 = createTextRequestJSON(recipes);
            JSONObject requestBody2 = createRequestJSON(recipes);
            RequestBody body1 = RequestBody.create(requestBody1.toString(), MediaType.get("application/json; charset=utf-8"));
            RequestBody body2 = RequestBody.create(requestBody2.toString(), MediaType.get("application/json; charset=utf-8"));
            Request request1 = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + API_KEY)
                    .post(body1)
                    .build();

            Request request2 = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + API_KEY)
                    .post(body2)
                    .build();

            client.newCall(request1).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    suggestion.setText("Failed to load response due to " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonObject = new JSONObject(response.body().string());
                            JSONArray jsonArray = jsonObject.getJSONArray("choices");
                            String result = jsonArray.getJSONObject(0).getJSONObject("message").getString("content");
                            RecommendationList.getInstance(getContext()).setSuggestion(result);
                            requireActivity().runOnUiThread(() -> showSuggestion(result));

                        } catch (JSONException e) {
                            suggestion.setText("Failed to load response due to " + e.getMessage());
                        }
                    } else {
                        Log.d("RecommendationList", "Failed to load response due to " + response.body().string());
                    }
                }
            });


            client.newCall(request2).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.d("RecommendationList", "Failed to load response due to " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonObject = new JSONObject(response.body().string());
                            JSONArray jsonArray = jsonObject.getJSONArray("choices");
                            if (jsonArray.length() > 0) {
                                JSONObject firstChoice = jsonArray.getJSONObject(0);
                                JSONObject message = firstChoice.getJSONObject("message");
                                if (message.has("content")) {
                                    String result = message.getString("content");
                                    Log.d(TAG,result);
                                    List<Recipe> recommendationList = Utils.parseGeneratedText(result);
                                    if (!recommendationList.isEmpty()) {
                                        RecommendationList.getInstance(getContext()).setRecipeList(recommendationList);
                                        requireActivity().runOnUiThread(() ->showRecommendList(recommendationList));
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
                            Log.d("RecommendationList", "Failed to load response due to " + response.body().string());
                        }
                    } else {
                        String errorMessage = response.body().string();
                        Log.e(TAG, "Error message: " + errorMessage);
                    }
                }
            });
        }

    }

    public void showSuggestion(String suggestionText){
        if(suggestionText == null || suggestionText == ""){
            progressIndicator1.setVisibility(View.GONE);
            suggestion.setVisibility(View.VISIBLE);
            suggestion.setText("You don't have any recipes in cook book for now. Discover more for nutrition recommendation!");
        }
        else{
            progressIndicator1.setVisibility(View.GONE);
            suggestion.setVisibility(View.VISIBLE);
            suggestion.setText(suggestionText);
        }
    }

    public void showRecommendList(List<Recipe> recommendationList){
        if(recommendationList.size()!=0){
            progressIndicator2.setVisibility(View.GONE);
            textView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            cooldownManager.startCooldown();
            cardAdapter.setRecipeList(recommendationList);
            cardAdapter.notifyDataSetChanged();

        }
        else{
            progressIndicator2.setVisibility(View.GONE);
            textView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            textView.setText("You don't have a recipe list for now. Go discover recipes!");
        }
    }

    private JSONObject createTextRequestJSON(List<Recipe> recipes){
        JSONObject requestBody = new JSONObject();
        JSONArray messages = new JSONArray();


        try {
            requestBody.put("model","gpt-3.5-turbo" );
            requestBody.put("temperature", 0);

            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", "Craft a concise nutrition suggestion based on what ingredients to eat to have healthier diet. aiming for around 50 words.");
            messages.put(systemMsg);


            JSONObject msg = new JSONObject();
            msg.put("role", "user");
            msg.put("content", Utils.RecipeListToString(recipes));


            messages.put(msg);
            requestBody.put("messages",messages);

        } catch (JSONException e){
            e.printStackTrace();
        }
        return requestBody;
    }

    private JSONObject createRequestJSON(List<Recipe> recipes){
        JSONObject requestBody = new JSONObject();
        JSONArray messages = new JSONArray();


        try {
            requestBody.put("model","gpt-4-turbo" );
            requestBody.put("temperature", 0);

            JSONObject responseFormat = new JSONObject();
            responseFormat.put("type", "json_object"); // Change the format here if needed

            requestBody.put("response_format", responseFormat);

            JSONObject systemMsg = new JSONObject();
            JSONObject assistMsg = new JSONObject();


            systemMsg.put("role", "system");
            systemMsg.put("content", "You are a helpful cooking assistant that generate recommended recipes for healthier nutrients , give 5 recipes that not using precious used ingredients in the given recipe list. the user will provide the recipes in json format, " +
                    "Provide your answer in JSON Structure like this " +
                    "{" +
                    "\"recipes\":[\n" +
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
                    "]" +
                    "}");
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
            userMsg.put("content",Utils.RecipeListToString(recipes));

            messages.put(userMsg);
            requestBody.put("messages",messages);

        } catch (JSONException e){
            e.printStackTrace();
        }
        return requestBody;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (cardAdapter != null) {
            cardAdapter = null;
        }
        if (recommendationList != null) {
            recommendationList = new ArrayList<>();;
        }
        if (client != null) {
            client.dispatcher().cancelAll();
        }
    }
}