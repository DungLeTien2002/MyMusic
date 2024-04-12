package com.example.mymusic.base.adapter.playlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.base.data.db.entities.LocalPlaylistEntity
import com.example.mymusic.base.utils.extension.setEnabledAll
import com.example.mymusic.databinding.ItemLocalPlaylistBinding

class AddToAPlaylistAdapter(private val list: ArrayList<LocalPlaylistEntity>): RecyclerView.Adapter<AddToAPlaylistAdapter.ViewHolder>() {

    lateinit var mListener: OnItemClickListener

    private var videoId: String? = null

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    fun setVideoId(videoId: String) {
        val oldVideoId = videoId
        this.videoId = videoId
        list.forEach { playlist ->
            if (playlist.tracks?.contains(videoId) == true && !playlist.tracks.contains(oldVideoId)) {
                notifyItemChanged(list.indexOf(playlist))
            } else if (playlist.tracks?.contains(oldVideoId) == true && !playlist.tracks.contains(
                    videoId
                )
            ) {
                notifyItemChanged(list.indexOf(playlist))
            }
        }
    }

    inner class ViewHolder(
        val binding: ItemLocalPlaylistBinding,
        val listener: OnItemClickListener
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition)
            }
        }

        fun bind(item: LocalPlaylistEntity) {
            binding.tvLocalPlaylistTitle.text = item.title
            if (item.tracks?.contains(videoId) == true) {
                binding.ivAdded.visibility = View.VISIBLE
                setEnabledAll(binding.root, false)
            } else {
                binding.ivAdded.visibility = View.GONE
                setEnabledAll(binding.root, true)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemLocalPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false), mListener)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        return holder.bind(list[position])
    }

    fun updateList(newList: List<LocalPlaylistEntity>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}