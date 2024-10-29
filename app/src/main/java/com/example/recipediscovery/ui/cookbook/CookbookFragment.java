package com.example.recipediscovery.ui.cookbook;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.recipediscovery.CardAdapter;
import com.example.recipediscovery.RecipeList;
import com.example.recipediscovery.PhotoTakingActivity;
import com.example.recipediscovery.R;
import com.example.recipediscovery.Recipe;
import com.example.recipediscovery.databinding.FragmentCookbookBinding;

import java.util.ArrayList;
import java.util.List;

public class CookbookFragment extends Fragment {

    private FragmentCookbookBinding binding;

    private List<Recipe> recipeList = new ArrayList<>();
    private RecyclerView recyclerView;


    private boolean isCloseButtonVisible = true;

    private TextView textView;
    private CardAdapter cardAdapter;
    String jsonString = "[\n" +
            "    {\n" +
            "        \"name\": \"Chocolate Cake\",\n" +
            "        \"ingredients\": [\"flour\", \"sugar\", \"cocoa powder\", \"baking soda\", \"salt\", \"eggs\", \"vegetable oil\", \"vanilla extract\", \"hot water\"],\n" +
            "        \"steps\": [\"Preheat oven to 350°F (175°C) and grease a 9x13 inch baking pan.\", \"In a large bowl, mix together the flour, sugar, cocoa powder, baking soda, and salt.\", \"Add the eggs, vegetable oil, and vanilla extract to the dry ingredients and mix until well combined.\", \"Stir in the hot water until the batter is smooth.\", \"Pour the batter into the prepared baking pan.\", \"Bake in preheated oven for 35 to 40 minutes, or until a toothpick inserted into the center comes out clean.\", \"Allow the cake to cool in the pan for 10 minutes, then remove from pan and transfer to a wire rack to cool completely.\"]\n" +
            "    },\n" +
            "    {\n" +
            "        \"name\": \"Vanilla Cupcakes\",\n" +
            "        \"ingredients\": [\"flour\", \"sugar\", \"baking powder\", \"salt\", \"butter\", \"eggs\", \"milk\", \"vanilla extract\"],\n" +
            "        \"steps\": [\"Preheat oven to 350°F (175°C) and line a cupcake pan with paper liners.\", \"In a medium bowl, mix together flour, sugar, baking powder, and salt.\", \"Add butter to the dry ingredients and mix until mixture resembles coarse sand.\", \"In a separate bowl, whisk together eggs, milk, and vanilla extract.\", \"Pour wet ingredients into dry ingredients and mix until smooth.\", \"Spoon batter into prepared cupcake pan.\", \"Bake in preheated oven for 18-20 minutes, or until a toothpick inserted into the center comes out clean.\", \"Allow cupcakes to cool in pan for a few minutes, then transfer to a wire rack to cool completely.\"]\n" +
            "    },\n" +
            "    {\n" +
            "        \"name\": \"Chocolate Cake111\",\n" +
            "        \"ingredients\": [\"flour\", \"sugar\", \"cocoa powder\", \"baking soda\", \"salt\", \"eggs\", \"vegetable oil\", \"vanilla extract\", \"hot water\"],\n" +
            "        \"steps\": [\"Preheat oven to 350°F (175°C) and grease a 9x13 inch baking pan.\", \"In a large bowl, mix together the flour, sugar, cocoa powder, baking soda, and salt.\", \"Add the eggs, vegetable oil, and vanilla extract to the dry ingredients and mix until well combined.\", \"Stir in the hot water until the batter is smooth.\", \"Pour the batter into the prepared baking pan.\", \"Bake in preheated oven for 35 to 40 minutes, or until a toothpick inserted into the center comes out clean.\", \"Allow the cake to cool in the pan for 10 minutes, then remove from pan and transfer to a wire rack to cool completely.\"]\n" +
            "    },\n" +
            "    {\n" +
            "        \"name\": \"Vanilla Cupcakes2222\",\n" +
            "        \"ingredients\": [\"flour\", \"sugar\", \"baking powder\", \"salt\", \"butter\", \"eggs\", \"milk\", \"vanilla extract\"],\n" +
            "        \"steps\": [\"Preheat oven to 350°F (175°C) and line a cupcake pan with paper liners.\", \"In a medium bowl, mix together flour, sugar, baking powder, and salt.\", \"Add butter to the dry ingredients and mix until mixture resembles coarse sand.\", \"In a separate bowl, whisk together eggs, milk, and vanilla extract.\", \"Pour wet ingredients into dry ingredients and mix until smooth.\", \"Spoon batter into prepared cupcake pan.\", \"Bake in preheated oven for 18-20 minutes, or until a toothpick inserted into the center comes out clean.\", \"Allow cupcakes to cool in pan for a few minutes, then transfer to a wire rack to cool completely.\"]\n" +
            "    }\n" +
            "]";




    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        CookbookViewModel cookbookViewModel =
                new ViewModelProvider(this).get(CookbookViewModel.class);

        binding = FragmentCookbookBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        recyclerView = root.findViewById(R.id.recyclerView);
        textView = root.findViewById(R.id.homeMessage);




        //List<Recipe> recipeList = Utils.parseGeneratedText(recipesJson);
        List<Recipe> recipeList = RecipeList.getInstance(getContext()).getRecipeList();

        if(recipeList.size()!=0){
            textView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
            recyclerView.setLayoutManager(layoutManager);
            cardAdapter = new CardAdapter(recipeList, isCloseButtonVisible);
            recyclerView.setAdapter(cardAdapter);
        }
        else{
            textView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            textView.setText("You don't have a recipe list for now. Go discover recipes!");
        }


        // start discover button
        Button startButton = root.findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), PhotoTakingActivity.class);
                startActivity(intent);
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (cardAdapter != null) {
            cardAdapter = null;
        }
        if (recipeList != null) {
            recipeList = new ArrayList<>();;
        }
    }
}