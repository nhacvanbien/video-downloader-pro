/*
 * Copyright (c) 2025. Tevo Global Limited
 *
 * This software and all accompanying documentation is the sole property of
 * Tevo Global Limited and is protected by copyright law and international treaties.
 *
 * Unauthorized copying, distribution, or reproduction of this software, or any
 * portion of it, is strictly prohibited. The software is licensed to you solely for
 * your personal use and may not be used for commercial purposes without
 * a separate license agreement.
 *
 * You may not modify, reverse engineer, decompile, or disassemble this software.
 * You are not permitted to remove or alter any copyright notices or proprietary
 * legends from the software.
 *
 * All rights not expressly granted herein are reserved by Tevo Global Limited.
 *
 * Contact information: hello@tevo.app
 */

package com.vimalcvs.materialrating

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class FeedbackMediaAdapter(
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<FeedbackMediaAdapter.MediaViewHolder>() {

    private val mediaList = mutableListOf<Uri>()

    fun addMedia(uri: Uri) {
        if (mediaList.size < 5) {
            mediaList.add(uri)
            notifyItemInserted(mediaList.size - 1)
        }
    }

    fun removeMedia(position: Int) {
        if (position in mediaList.indices) {
            mediaList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun getMediaList(): List<Uri> = mediaList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.feedback_media_item, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(mediaList[position])
    }

    override fun getItemCount(): Int = mediaList.size

    inner class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageMedia: AppCompatImageView = itemView.findViewById(R.id.image_media)
        private val buttonDelete: AppCompatImageView = itemView.findViewById(R.id.button_delete)

        init {
            buttonDelete.setOnClickListener {
                onDeleteClick(adapterPosition)
            }
        }

        fun bind(uri: Uri) {
            Glide.with(itemView.context)
                .load(uri)
                .centerCrop()
                .into(imageMedia)
        }
    }
}
