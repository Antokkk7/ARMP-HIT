package fr.antokj.windriders2.mreseau;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MreseauViewModel extends ViewModel {
    private final MutableLiveData<String> mText;

    public MreseauViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Page 'Mon réseau'");
    }

    public LiveData<String> getText() {
        return mText;
    }
}