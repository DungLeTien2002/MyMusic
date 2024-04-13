package com.example.mymusic.base.adapter.moodandgenre.genre

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.R
import com.example.mymusic.base.data.models.explore.mood.genre.Content
import com.example.mymusic.base.utils.extension.navigateSafe
import com.example.mymusic.databinding.ItemMoodMomentPlaylistBinding
import com.maxrave.simpmusic.data.model.explore.mood.genre.ItemsPlaylist


class GenreItemAdapter(private var genreList: ArrayList<ItemsPlaylist>, val context: Context, val navController: NavController): RecyclerView.Adapter<GenreItemAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemMoodMomentPlaylistBinding): RecyclerView.ViewHolder(binding.root)

    fun updateData(newList: ArrayList<ItemsPlaylist>){
        genreList.clear()
        genreList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemMoodMomentPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
    return genreList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val genre = genreList[position]
        with(holder){
            binding.tvTitle.text = genre.header
            val playlistContent: ArrayList<Content> = genre.contents as ArrayList<Content>
            val playlistAdapter = GenreContentAdapter(playlistContent)
            val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            binding.childRecyclerview.apply {
                adapter = playlistAdapter
                layoutManager = linearLayoutManager
            }
            playlistAdapter.setOnClickListener(object : GenreContentAdapter.OnClickListener {
                override fun onClick(position: Int) {
                    val args = Bundle()
                    args.putString("id", playlistContent[position].playlistBrowseId)
                    navController.navigateSafe(R.id.action_global_playlistFragment, args)
                }
            })
        }
    }
}