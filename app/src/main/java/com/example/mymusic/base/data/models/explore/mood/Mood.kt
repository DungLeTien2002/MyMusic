package com.example.mymusic.base.data.models.explore.mood


import com.google.gson.annotations.SerializedName
import com.maxrave.simpmusic.data.model.explore.mood.Genre

data class Mood(
    @SerializedName(value = "Genres", alternate = ["Thể loại", "Gatunki", "Per te"])
    val genres: ArrayList<Genre>,
    @SerializedName(value = "Moods & moments", alternate = ["Tâm trạng và khoảnh khắc", "Nastroje i momenty", "Mood e momenti"])
    val moodsMoments: ArrayList<MoodsMoment>
)