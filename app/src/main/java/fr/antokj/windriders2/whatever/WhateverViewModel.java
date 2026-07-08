package fr.antokj.windriders2.whatever;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class WhateverViewModel extends ViewModel {
    private final MutableLiveData<String> mText;

    public WhateverViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Page 'Whatever'");
    }

    public LiveData<String> getText() {
        return mText;
    }
}