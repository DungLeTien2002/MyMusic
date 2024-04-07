package com.example.mymusic.base.baseview

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import androidx.viewbinding.ViewBinding
import com.example.mymusic.base.utils.extension.finishWithSlide
import com.example.mymusic.base.utils.extension.handleBackPressed
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {
    //region variable
    lateinit var binding: VB
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = inflateViewBinding(layoutInflater)
        setContentView(binding.root)
        initView()
        initData()
        initListener()
        handleBackPressed {
            onBack()
        }
    }

    open fun onBack() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            onBackPressedDispatcher.onBackPressed()
        } else {
            finishWithSlide()
        }
    }

    abstract fun initListener()

    abstract fun initData()

    abstract fun initView()

    abstract fun inflateViewBinding(layoutInflater: LayoutInflater): VB
}