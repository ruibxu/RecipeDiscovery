package com.example.recipediscovery.ui.cookbook;

import android.widget.TextView;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.recyclerview.widget.RecyclerView;

import com.example.recipediscovery.CardAdapter;
import com.example.recipediscovery.Recipe;

import java.util.ArrayList;
import java.util.List;

public class CookbookViewModel extends ViewModel     {

    private final MutableLiveData<String> mText;

    private CardAdapter cardAdapter;

    public CookbookViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is home fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}