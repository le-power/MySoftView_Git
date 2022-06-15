package com.justalk.kids.softlibrary;

import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

public interface TranslateDeferringStateCallBack {
    void onProgress(WindowInsetsCompat insets, List<WindowInsetsAnimationCompat> runningAnimations);

    void onEnd(WindowInsetsAnimationCompat animation);

}
