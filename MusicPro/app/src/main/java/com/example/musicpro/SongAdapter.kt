package com.example.musicpro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SongAdapter(
    private var songList: MutableList<Song>,
    private val onItemClick: (Song) -> Unit,
    private val onItemLongClick: (Song) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songList[position]
        holder.bind(song, onItemClick, onItemLongClick)
    }

    override fun getItemCount(): Int = songList.size

    fun updateData(newSongs: List<Song>) {
        songList.clear()
        songList.addAll(newSongs)
        notifyDataSetChanged()
    }
    
    fun getSongs(): List<Song> {
        return songList
    }
    
    fun onItemDismiss(position: Int) {
        songList.removeAt(position)
        notifyItemRemoved(position)
    }

    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_song_title)
        private val tvArtist: TextView = itemView.findViewById(R.id.tv_artist)
        private val ivCover: ImageView = itemView.findViewById(R.id.iv_cover)
        private val ivFavorite: ImageView = itemView.findViewById(R.id.iv_favorite) // 🌟 获取收藏图标

        fun bind(song: Song, click: (Song) -> Unit, longClick: (Song) -> Unit) {
            tvTitle.text = song.title
            tvArtist.text = song.artist
            if (song.cover != null) {
                ivCover.setImageBitmap(song.cover)
            } else {
                ivCover.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            
            // 🌟 根据收藏状态显示或隐藏图标
            ivFavorite.visibility = if (song.isFavorite) View.VISIBLE else View.GONE

            itemView.setOnClickListener { click(song) }
            itemView.setOnLongClickListener { longClick(song); true }
        }
    }
}
