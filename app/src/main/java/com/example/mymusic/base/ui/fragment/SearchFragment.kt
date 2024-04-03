package com.example.mymusic.base.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mymusic.R
import com.example.mymusic.base.baseview.BaseFragment
import com.example.mymusic.databinding.FragmentSearchBinding


class SearchFragment : BaseFragment<FragmentSearchBinding>() {
    override fun inflateLayout(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSearchBinding {
        return  FragmentSearchBinding.inflate(inflater,container,false)
    }

    override fun initListener() {

    }

    override fun initData() {

    }

    override fun initView() {

    }
}