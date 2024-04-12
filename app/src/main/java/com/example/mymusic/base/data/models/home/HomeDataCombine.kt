package com.example.mymusic.base.data.models.home

import com.example.mymusic.base.data.models.home.chart.Chart
import com.example.mymusic.base.utils.Resource
import com.example.mymusic.base.data.models.explore.mood.Mood


data class HomeDataCombine(
    val home: Resource<ArrayList<HomeItem>>,
    val mood: Resource<Mood>,
    val chart: Resource<Chart>,
    val newRelease: Resource<ArrayList<HomeItem>>,
) {
}