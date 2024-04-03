package com.example.mymusic.base.ui.fragment.library

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mymusic.R
import com.example.mymusic.base.baseview.BaseFragment
import com.example.mymusic.databinding.FragmentLibraryBinding


class LibraryFragment : BaseFragment<FragmentLibraryBinding>() {
    override fun inflateLayout(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLibraryBinding {
        return FragmentLibraryBinding.inflate(inflater,container,false)
    }

    override fun initListener() {

    }

    override fun initData() {

    }

    override fun initView() {

    }
}