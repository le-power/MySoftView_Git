package com.justalk.kids.softlibrary;

import android.view.View;

import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

public interface TranslateTouchStateCallBack {


    void onNestedPreScroll ();

    void onNestedScroll();

    void onNestedFling();

    void onStopNestedScroll(View target, int type);

}
