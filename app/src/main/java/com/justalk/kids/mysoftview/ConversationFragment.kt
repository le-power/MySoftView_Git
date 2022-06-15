/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.justalk.kids.mysoftview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.justalk.kids.mysoftview.databinding.FragmentConversationBinding
import com.justalk.kids.softlibrary.*
import kotlin.math.abs


/**
 * The main entry point for the sample. See [onViewCreated] for more information on how
 * the sample works.
 */
class ConversationFragment : Fragment(), FragmentBackHandler, View.OnClickListener {
    private var _binding: FragmentConversationBinding? = null
    private val binding: FragmentConversationBinding get() = _binding!!
    lateinit var emotionKeyboard: EmotionKeyboard
    var softViewSizeToolViewCallback: TranslateDeferringInsetsAnimationCallback? = null
    var softViewSize = 0f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConversationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Set our conversation adapter on the RecyclerView
        binding.conversationRecyclerview.adapter = ConversationAdapter()

        // There are three steps to WindowInsetsAnimations:

        /**
         * 1) Since our Activity has declared `window.setDecorFitsSystemWindows(false)`, we need to
         * handle any [WindowInsetsCompat] as appropriate.
         *
         * Our [RootViewDeferringInsetsCallback] will update our attached view's padding to match
         * the combination of the [WindowInsetsCompat.Type.systemBars], and selectively apply the
         * [WindowInsetsCompat.Type.ime] insets, depending on any ongoing WindowInsetAnimations
         * (see that class for more information).
         */
        val deferringInsetsListener = RootViewDeferringInsetsCallback(
            persistentInsetTypes = WindowInsetsCompat.Type.systemBars(),
            deferredInsetTypes = WindowInsetsCompat.Type.ime()
        )
        // RootViewDeferringInsetsCallback is both an WindowInsetsAnimation.Callback and an
        // OnApplyWindowInsetsListener, so needs to be set as so.
        ViewCompat.setWindowInsetsAnimationCallback(binding.root, deferringInsetsListener)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, deferringInsetsListener)

        /**
         * 2) The second step is reacting to any animations which run. This can be system driven,
         * such as the user focusing on an EditText and on-screen keyboard (IME) coming on screen,
         * or app driven (more on that in step 3).
         *
         * To react to animations, we set an [android.view.WindowInsetsAnimation.Callback] on any
         * views which we wish to react to inset animations. In this example, we want our
         * EditText holder view, and the conversation RecyclerView to react.
         *
         * We use our [TranslateDeferringInsetsAnimationCallback] class, bundled in this sample,
         * which will automatically move each view as the IME animates.
         *
         * Note about [TranslateDeferringInsetsAnimationCallback], it relies on the behavior of
         * [RootViewDeferringInsetsCallback] on the layout's root view.
         */
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.messageContainer,
            TranslateDeferringInsetsAnimationCallback(
                view = binding.messageContainer,
                persistentInsetTypes = WindowInsetsCompat.Type.systemBars(),
                deferredInsetTypes = WindowInsetsCompat.Type.ime(),
                // We explicitly allow dispatch to continue down to binding.messageHolder's
                // child views, so that step 2.5 below receives the call
                dispatchMode = WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE,
            )
        )

        softViewSizeToolViewCallback = TranslateDeferringInsetsAnimationCallback(
            view = binding.conversationRecyclerview,
            persistentInsetTypes = WindowInsetsCompat.Type.systemBars(),
            deferredInsetTypes = WindowInsetsCompat.Type.ime(),
        )

        ViewCompat.setWindowInsetsAnimationCallback(
            binding.conversationRecyclerview,
            softViewSizeToolViewCallback
        )


        /**
         * 2.5) We also want to make sure that our EditText is focused once the IME
         * is animated in, to enable it to accept input. Similarly, if the IME is animated
         * off screen and the EditText is focused, we should clear that focus.
         *
         * The bundled [ControlFocusInsetsAnimationCallback] callback will automatically request
         * and clear focus for us.
         *
         * Since `binding.messageEdittext` is a child of `binding.messageHolder`, this
         * [WindowInsetsAnimationCompat.Callback] will only work if the ancestor view's callback uses the
         * [WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE] dispatch mode, which
         * we have done above.
         */
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.messageEdittext,
            ControlFocusInsetsAnimationCallback(binding.messageEdittext)
        )

        /**
         * 3) The third step is when the app wants to control and drive an inset animation.
         * This is an optional step, but suits many types of input UIs. The example scenario we
         * use in this sample is that the user can drag open the IME, by over-scrolling the
         * conversation RecyclerView. To enable this, we use a [InsetsAnimationLinearLayout] as a
         * root view in our layout which handles this automatically for scrolling views,
         * through nested scrolling.
         *
         * Alternatively, this sample also contains [InsetsAnimationTouchListener],
         * which is a [android.view.View.OnTouchListener] which does similar for non-scrolling
         * views, detecting raw drag events rather than scroll events to open/close the IME.
         *
         * Internally, both [InsetsAnimationLinearLayout] & [InsetsAnimationTouchListener] use a
         * class bundled in this sample called [SimpleImeAnimationController], which simplifies
         * much of the mechanics for controlling a [WindowInsetsAnimationCompat].
         */
        handleClickEvent()
        binding.messageEdittext.onFocusChangeListener =
            View.OnFocusChangeListener { _, p1 ->
                if (p1) {
                    if (binding.bottomcontainer.isVisible) {
                        emotionKeyboard.handlerClickEvent(false)
                    }
                    hideEmotionLayout()
                }
            }

        binding.imageBtn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                // 还在播放动画, 软键盘正在弹出,不让点
                if (EmotionKeyboard.startAnimation || EmotionKeyboard.startSoft) {
                    return
                }
                val currentTimeMillis = System.currentTimeMillis()
                //加一个最小点击时间间隔
                if (currentTimeMillis - EmotionKeyboard.tempCurrentTimeMillis < 500) {
                    return
                }
                EmotionKeyboard.tempCurrentTimeMillis = currentTimeMillis
                emotionKeyboard.handlerClickEvent(true)
            }
        })

        //软键盘已经完全弹出
        softViewSizeToolViewCallback!!.setTranslateDeferringStateCallBack(object :
            TranslateDeferringStateCallBack {
            override fun onProgress(
                insets: WindowInsetsCompat,
                runningAnimations: MutableList<WindowInsetsAnimationCompat>
            ) {
                val typesInset = insets.getInsets(WindowInsetsCompat.Type.ime())
                // Then we get the persistent inset types which are applied as padding during layout
                val otherInset = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                // Now that we subtract the two insets, to calculate the difference. We also coerce
                // the insets to be >= 0, to make sure we don't use negative insets.
                val diff = Insets.subtract(typesInset, otherInset).let {
                    Insets.max(it, Insets.NONE)
                }
                softViewSize = (diff.top - diff.bottom).toFloat()
                val tempSoftSize = abs(softViewSize).toInt()
                EmotionKeyboard.startSoft =
                    tempSoftSize != 0 && tempSoftSize != emotionKeyboard.keyBoardHeight
            }

            override fun onEnd(animation: WindowInsetsAnimationCompat?) {
                //注册必须要有对应的事件,多出来的回调会导致布局错乱
                binding.conversationRecyclerview.suppressLayout(false)
                EmotionKeyboard.translationEnable = true
                EmotionKeyboard.startSoft = false
                //存储软键盘高度数据
                val imeHeight = softViewSizeToolViewCallback!!.getImeHeight()
                emotionKeyboard.keyBoardHeight = imeHeight

                if (abs(softViewSize).toInt() == emotionKeyboard.keyBoardHeight) {
                    if (binding.gifEditContainer.isShown) {
                        binding.gifSearchEdit.requestFocus()
                    }
                }
            }
        })

        //rootView 手势监听回调
        binding.layoutRoot.setTouchCallBack(object :
            TranslateTouchStateCallBack {
            override fun onNestedPreScroll() {

            }

            override fun onNestedScroll() {

            }

            override fun onNestedFling() {

            }

            override fun onStopNestedScroll(target: View?, type: Int) {

            }
        })

        //以下为gif按钮点击处理
        binding.gifBtn.setOnClickListener(this)
        binding.gifBtnTop.setOnClickListener(this)
        binding.emoji0.setOnClickListener(this)
        binding.emoji0Top.setOnClickListener(this)
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.gif_btn, R.id.gif_btn_top -> {
                handleGifClickEvent()
            }
            R.id.emoji0, R.id.emoji0_top -> {
                resetGifView()
            }
        }
    }

    private fun hideEmotionLayout() {
        binding.bottomcontainer.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 点击按钮:
     * 如果未弹出软键盘,直接弹出底部view
     * 已经弹出软键盘,隐藏软键盘,显示底部view
     *
     * 软键盘弹出:
     * 如果没有底部view,直接弹出
     * 如果底部view已经弹出,底部view消失,软键盘弹出
     **/
    private fun handleClickEvent() {
        emotionKeyboard = EmotionKeyboard.with(activity)
        emotionKeyboard.bindToContent(binding.conversationRecyclerview)
        emotionKeyboard.bindToEditText(binding.messageEdittext)
        emotionKeyboard.setEmotionView(binding.bottomcontainer)
        emotionKeyboard.setMessageHolder(binding.messageHolder)
//        emotionKeyboard.bindToEmotionButton(binding.imageBtn)
//        emotionKeyboard.bindToEmotionButton(binding.emojiBtn)
    }

    override fun onBackPressed(): Boolean {
        if (EmotionKeyboard.bottomContainerShowFlag) { //底部显示出来
            if (EmotionKeyboard.startAnimation) { //底部消失动画正在进行
                return false
            }

//            emotionKeyboard.changeEmotionLayout(false)
            emotionKeyboard.changeEmotionLayoutByThread(false)
            return true
        }
        return false
    }


    /**
     * 点击GIF按钮:
     * 显示gif,显示gif输入框以及搜索结果
     */
    private fun handleGifClickEvent() {
        if (binding.gifSearchEdit.hasFocus()) {
            emotionKeyboard.showSoftInput(binding.gifSearchEdit)
            return
        }
        if (EmotionKeyboard.startAnimation || EmotionKeyboard.startSoft) {
            return
        }
        binding.messageHolder.visibility = View.GONE
        binding.gifEditContainer.visibility = View.VISIBLE
        emotionKeyboard.handlerClickEvent(true)
    }

    /**
     * 点击其他按钮:
     * 恢复显示gif之前的效果
     */
    private fun resetGifView() {
        if (EmotionKeyboard.bottomContainerShowFlag) {
            return
        }
        if (EmotionKeyboard.startAnimation || EmotionKeyboard.startSoft) {
            return
        }

        binding.messageHolder.visibility = View.VISIBLE
        binding.gifEditContainer.visibility = View.GONE
        emotionKeyboard.handlerClickEvent(true)
        binding.gifSearchEdit.clearFocus()
    }

}


