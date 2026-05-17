package com.example.musicpro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class SongAdapter(
    private var songList: MutableList<Song>,
    private val onItemClick: (Song) -> Unit,
    private val onItemLongClick: (Song) -> Unit,
    private val onSelectionChanged: (Int) -> Unit = {}
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    var isMultiSelectMode: Boolean = false
        set(value) {
            field = value
            if (!value) {
                songList.forEach { it.isSelected = false }
            }
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songList[position]
        holder.bind(song, isMultiSelectMode, onItemClick, onItemLongClick, onSelectionChanged)
    }

    override fun getItemCount(): Int = songList.size

    fun updateData(newSongs: List<Song>) {
        val diffCallback = SongDiffCallback(songList, newSongs)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        songList.clear()
        songList.addAll(newSongs)
        diffResult.dispatchUpdatesTo(this)
        // 🌟 强力刷新，解决 swiped 后扫描显示不出来的问题
        notifyDataSetChanged()
    }
    
    fun getSongs(): List<Song> {
        return songList
    }

    fun getSelectedSongs(): List<Song> {
        return songList.filter { it.isSelected }
    }

    fun selectAll(select: Boolean) {
        songList.forEach { it.isSelected = select }
        notifyDataSetChanged()
        onSelectionChanged(getSelectedSongs().size)
    }
    
    fun onItemDismiss(position: Int) {
        songList.removeAt(position)
        notifyItemRemoved(position)
    }

    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_song_title)
        private val tvArtist: TextView = itemView.findViewById(R.id.tv_artist)
        private val ivCover: ImageView = itemView.findViewById(R.id.iv_cover)
        private val ivFavorite: ImageView = itemView.findViewById(R.id.iv_favorite)
        private val cbSelect: CheckBox = itemView.findViewById(R.id.cb_select)

        fun bind(
            song: Song,
            isMultiSelect: Boolean,
            click: (Song) -> Unit,
            longClick: (Song) -> Unit,
            selectionChanged: (Int) -> Unit
        ) {
            tvTitle.text = song.title
            tvArtist.text = song.artist
            
            // 异步加载逻辑：先设占位图，再在后台加载
            if (song.cover != null) {
                ivCover.setImageBitmap(song.cover)
            } else {
                ivCover.setImageResource(android.R.drawable.ic_menu_gallery)
                // 启动后台线程加载缩略图
                val context = itemView.context
                Thread {
                    try {
                        val uri = song.uri
                        val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            context.contentResolver.loadThumbnail(uri, android.util.Size(120, 120), null)
                        } else null
                        
                        if (bitmap != null) {
                            ivCover.post {
                                ivCover.setImageBitmap(bitmap)
                            }
                        }
                    } catch (e: Exception) {}
                }.start()
            }
            
            ivFavorite.visibility = if (song.isFavorite && !isMultiSelect) View.VISIBLE else View.GONE

            cbSelect.visibility = if (isMultiSelect) View.VISIBLE else View.GONE
            cbSelect.isChecked = song.isSelected

            cbSelect.setOnClickListener {
                song.isSelected = cbSelect.isChecked
                selectionChanged(-1) // Signal that something changed, count will be recalculated
            }

            itemView.setOnClickListener {
                if (isMultiSelect) {
                    song.isSelected = !song.isSelected
                    cbSelect.isChecked = song.isSelected
                    selectionChanged(-1)
                } else {
                    click(song)
                }
            }
            itemView.setOnLongClickListener { 
                if (!isMultiSelect) {
                    longClick(song)
                }
                true 
            }
        }
    }

    class SongDiffCallback(
        private val oldList: List<Song>,
        private val newList: List<Song>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldSong = oldList[oldItemPosition]
            val newSong = newList[newItemPosition]
            return oldSong.title == newSong.title &&
                    oldSong.artist == newSong.artist &&
                    oldSong.isFavorite == newSong.isFavorite
        }
    }
}
