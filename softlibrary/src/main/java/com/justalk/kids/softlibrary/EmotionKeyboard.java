package com.justalk.kids.softlibrary;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class EmotionKeyboard {
    private static final String SHARE_PREFERENCE_NAME = "EmotionKeyboard";
    private static final String SHARE_PREFERENCE_SOFT_INPUT_HEIGHT = "soft_input_height";
    private Activity mActivity;
    private InputMethodManager mInputManager;//软键盘管理类
    private SharedPreferences sp;
    private View mEmotionLayout;//表情布局
    private View mMessageHolder;//底部bar布局
    private EditText mEditText;//
    private RecyclerView mContentView;//内容布局view,即除了表情布局或者软键盘布局以外的布局，用于固定bar的高度，防止跳闪
    public static boolean bottomContainerShowFlag = false;
    public static boolean startSoft = false;
    public static boolean startAnimation = false;  //动画执行标记
    public static boolean translationEnable = true; //视图是否需要跟随软键盘高度变化标记
    private EmotionKeyboard() {
    }

    /**
     * 外部静态调用
     */
    public static EmotionKeyboard with(Activity activity) {
        EmotionKeyboard emotionInputDetector = new EmotionKeyboard();
        emotionInputDetector.mActivity = activity;
        emotionInputDetector.mInputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        emotionInputDetector.sp = activity.getSharedPreferences(SHARE_PREFERENCE_NAME, Context.MODE_PRIVATE);
        return emotionInputDetector;
    }

    /**
     * 绑定内容view，此view用于固定bar的高度，防止跳闪
     */
    public EmotionKeyboard bindToContent(RecyclerView contentView) {
        mContentView = contentView;
        return this;
    }

    /**
     * 绑定编辑框
     */
    public EmotionKeyboard bindToEditText(EditText editText) {
        mEditText = editText;
        return this;
    }

    /**
     * 设置编辑栏的layout
     */
    public EmotionKeyboard setMessageHolder(View messageHolder) {
        mMessageHolder = messageHolder;
        return this;
    }

    /**
     * 设置表情内容布局
     */
    public EmotionKeyboard setEmotionView(View emotionView) {
        mEmotionLayout = emotionView;
        return this;
    }

    public EmotionKeyboard build() {//设置软件盘的模式：SOFT_INPUT_ADJUST_RESIZE  这个属性表示Activity的主窗口总是会被调整大小，从而保证软键盘显示空间。
        //从而方便我们计算软件盘的高度
        mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN |
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);            //隐藏软件盘
        hideSoftInput();
        return this;
    }

    /**
     * 点击返回键时先隐藏表情布局
     */
    public boolean interceptBackPress() {
        if (mEmotionLayout.isShown()) {
            hideEmotionLayout(false);
            return true;
        }
        return false;
    }

    private void showEmotionLayout() {
        int softInputHeight = getKeyBoardHeight();
        hideSoftInput();
        mEmotionLayout.getLayoutParams().height = softInputHeight;
        mEmotionLayout.setVisibility(View.VISIBLE);
        startOpenAnimation();
    }


    /**
     * 开启底部view的展示动画
     */
    public void startOpenAnimation() {
        TranslateAnimation animation = new TranslateAnimation(
                Animation.ABSOLUTE,
                0f,
                Animation.ABSOLUTE,
                0f,
                Animation.ABSOLUTE,
                mEmotionLayout.getLayoutParams().height,
                Animation.ABSOLUTE,
                0f
        );


        animation.setDuration(300);
        animation.setFillAfter(true);
        mEmotionLayout.startAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                startAnimation = false;
                bottomContainerShowFlag = true;
                //重置状态
                mContentView.suppressLayout(false);
                mEditText.setEnabled(true);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    /**
     * 关闭底部view的展示动画
     **/
    public void startCloseAnimation() {
        TranslateAnimation animation = new TranslateAnimation(
                Animation.ABSOLUTE,
                0f,
                Animation.ABSOLUTE,
                0f,
                Animation.ABSOLUTE,
                0f,
                Animation.ABSOLUTE,
                mEmotionLayout.getLayoutParams().height
        );

        animation.setDuration(300);
        animation.setFillAfter(false);
        mEmotionLayout.startAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mEmotionLayout.setVisibility(View.GONE);
                startAnimation = false;
                bottomContainerShowFlag = false;
                //重置状态
                mContentView.suppressLayout(false);
                mEditText.setEnabled(true);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

    }

    /**
     * 隐藏表情布局
     *
     * @param showSoftInput 是否显示软件盘
     */
    public void hideEmotionLayout(boolean showSoftInput) {
        if (mEmotionLayout.isShown()) {
            startCloseAnimation();
            if (showSoftInput) {
                if (isClick){
                    mEditText.clearFocus();  //重置状态
                    mEditText.requestFocus();
                }else {
                    showSoftInput();
                }
            }
        }
    }

    /**
     * 锁定内容高度，防止跳闪
     */
    public void lockContentHeight() {
        translationEnable = false;
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mContentView.getLayoutParams();
        params.height = mContentView.getHeight();
        params.weight = 0.0F;
    }

    /**
     * 释放被锁定的内容高度
     */
    public void unlockContentHeightDelayed() {
        mEditText.postDelayed(() -> ((LinearLayout.LayoutParams) mContentView.getLayoutParams()).weight = 1.0F, 200L);
    }

    /**
     * 编辑框获取焦点，并显示软件盘
     */
    public void showSoftInput() {
        mEditText.post(() -> mInputManager.showSoftInput(mEditText,InputMethodManager.SHOW_FORCED));
    }

    /**
     * 编辑框获取焦点，并显示软件盘
     */
    public void showSoftInput(EditText editText) {
        editText.post(() -> mInputManager.showSoftInput(editText,InputMethodManager.SHOW_FORCED));
    }

    /**
     * 隐藏软件盘
     */
    public void hideSoftInput() {
        mInputManager.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
    }

    /**
     * 是否显示软件盘
     */
    public boolean isSoftInputShown() {
        return getSupportSoftInputHeight() > getStatusBarHeight(mActivity);
    }

    /**
     * 获取软件盘的高度
     * decorView是window中的最顶层view，可以从window中通过getDecorView获取到decorView。
     * 通过decorView获取到程序显示的区域，包括标题栏，但不包括状态栏。
     * 某些Android版本下，没有显示软键盘时减出来的高度总是144，而不是零，
     * 这是因为高度是包括了虚拟按键栏的(例如华为系列)，所以在API Level高于20时，
     * 我们需要减去底部虚拟按键栏的高度（如果有的话）
     */
    private int getSupportSoftInputHeight() {
        Rect r = new Rect();
        mActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(r);            //获取屏幕的高度
        int screenHeight = mActivity.getWindow().getDecorView().getRootView().getHeight();     //计算软件盘的高度
        int statusBarHeight = getStatusBarHeight(mActivity);   //获取状态栏高度

        int softInputHeight = screenHeight - r.bottom + statusBarHeight;

        softInputHeight = softInputHeight - getSoftButtonsBarHeight();
        return softInputHeight;
    }

    /**
     * 底部虚拟按键栏的高度
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private int getSoftButtonsBarHeight() {
        DisplayMetrics metrics = new DisplayMetrics();            //这个方法获取可能不是真实屏幕的高度
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int usableHeight = metrics.heightPixels;            //获取当前屏幕的真实高度
        mActivity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        int realHeight = metrics.heightPixels;
        if (realHeight > usableHeight) {
            return realHeight - usableHeight;
        } else {
            return 0;
        }

    }

    /**
     * 设置软键盘高度
     */
    public void setKeyBoardHeight(int keyBoardHeight) {
        //存一份到本地
        if (keyBoardHeight > 0) {
            sp.edit().putInt(SHARE_PREFERENCE_SOFT_INPUT_HEIGHT, keyBoardHeight).apply();
        }
    }

    /**
     * 获取软键盘高度
     */
    public int getKeyBoardHeight() {
        return sp.getInt(SHARE_PREFERENCE_SOFT_INPUT_HEIGHT, 825);
    }

    private int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static long tempCurrentTimeMillis = 0;
    public boolean isClick = false;
    public void handlerClickEvent(boolean isClick) {
        this.isClick = isClick;
        startAnimation = true;
        mContentView.suppressLayout(true);
        if (mEmotionLayout.isShown()) { //隐藏底部布局
            lockContentHeight();//显示软件盘时，锁定内容高度，防止跳闪。
            hideEmotionLayout(true);//隐藏表情布局，显示软件盘
            unlockContentHeightDelayed();//软件盘显示后，释放内容高度
        } else {
            mEditText.setEnabled(false);
            if (isSoftInputShown()) {//同上 显示底部布局
                lockContentHeight();
                showEmotionLayout();
                unlockContentHeightDelayed();
            } else { //直接显示表情布局
//                changeEmotionLayout(true);
                changeEmotionLayoutByThread(true);
            }
        }
    }

    private static boolean needUpdate = true;
    private static boolean threadStartFlag = false;
    private int duration = 2;
    private int animationDuration = 150;
    Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            int keyBoardHeight = getKeyBoardHeight();
            super.handleMessage(msg);
            int interval = keyBoardHeight / (animationDuration / duration);
            ViewGroup.LayoutParams layoutParams = mEmotionLayout.getLayoutParams();
            int height = layoutParams.height;

            needUpdate = true;
            //底部view隐藏后重置view的高度
            if (!mEmotionLayout.isShown() && height == keyBoardHeight){
                height = 0;
            }

            switch (msg.what){
                case 100: //展开底部view
                    if (height >= (keyBoardHeight - interval)){
                        height = keyBoardHeight;
                        needUpdate = false;
                        threadStartFlag = false;
                        bottomContainerShowFlag = true;
                        startAnimation = false;
                    }else {
                        height = height + interval;
                        mEditText.setEnabled(true);
                        mContentView.suppressLayout(false);
                    }
                    mEmotionLayout.setVisibility(View.VISIBLE);
                    layoutParams.height = height;
                    break;
                case 200: //关闭底部view
                    if (height <=  interval){
                        height = 0;
                        needUpdate = false;
                        threadStartFlag = false;
                        bottomContainerShowFlag = false;
                        mEmotionLayout.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mEmotionLayout.setVisibility(View.GONE);
                                startAnimation = false;
                                mContentView.suppressLayout(false);
                            }
                        },200);
                    }else {
                        height = height - interval;
                    }
                    layoutParams.height = height;
                    break;
                default:
            }
            mEmotionLayout.setLayoutParams(layoutParams);
        }
    };

    //改变底部container的动画方式二
    public void changeEmotionLayoutByThread(boolean isOpen) {
        if (threadStartFlag){
            return;
        }
        needUpdate = true;
        new Thread(() -> {
            while (needUpdate){
                startAnimation = true;
                SystemClock.sleep(duration);
                int what;
                if (isOpen){
                    what = 100;
                }else {
                    what = 200;
                }
                handler.sendEmptyMessage(what);
                threadStartFlag = true;
            }
        }).start();
    }
}

