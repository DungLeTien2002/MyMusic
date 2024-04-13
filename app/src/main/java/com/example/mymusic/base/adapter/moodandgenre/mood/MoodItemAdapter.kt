package com.example.mymusic.base.adapter.moodandgenre.mood

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.R
import com.example.mymusic.base.data.models.explore.mood.moodmoments.Content
import com.example.mymusic.base.utils.extension.navigateSafe
import com.example.mymusic.databinding.ItemMoodMomentPlaylistBinding
import com.maxrave.simpmusic.data.model.explore.mood.moodmoments.Item


class MoodItemAdapter(private var itemList: ArrayList<Item>, val context: Context, val navController: NavController): RecyclerView.Adapter<MoodItemAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemMoodMomentPlaylistBinding): RecyclerView.ViewHolder(binding.root)

    fun updateData(newList: ArrayList<Item>){
        itemList.clear()
        itemList.addAll(newList)
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemMoodMomentPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]
        with(holder){
            binding.tvTitle.text = item.header
            val playlistContent: ArrayList<Content> = item.contents as ArrayList<Content>
            val contentAdapter = MoodContentAdapter(playlistContent)
            val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            binding.childRecyclerview.apply {
                adapter = contentAdapter
                layoutManager = linearLayoutManager
            }
            contentAdapter.setOnClickListener(object : MoodContentAdapter.OnClickListener {
                override fun onClick(position: Int) {
                    val args = Bundle()
                    args.putString("id", playlistContent[position].playlistBrowseId)
                    navController.navigateSafe(R.id.action_global_playlistFragment, args)
                }
            })
        }
    }
}