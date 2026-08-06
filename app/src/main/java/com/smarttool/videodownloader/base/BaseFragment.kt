package com.smarttool.videodownloader.base

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.smarttool.videodownloader.VideoDownloaderApplication
import com.smarttool.videodownloader.feature.tab.presentation.TabViewModel
import com.smarttool.videodownloader.core.SystemUtil

abstract class BaseFragment<T : ViewBinding> : Fragment() {
    protected lateinit var binding: T

    abstract fun getViewBinding(): T

    private var lastClickTime: Long = 0

    val tabViewModels: TabViewModel by lazy {
        (requireContext().applicationContext as VideoDownloaderApplication).globalViewModel
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        SystemUtil.setLocale(requireContext())
        binding = getViewBinding()
        return binding.root
    }

    fun aVoidDoubleClick(): Boolean {
        if (SystemClock.elapsedRealtime() - lastClickTime < 500) {
            return true
        }
        lastClickTime = SystemClock.elapsedRealtime()
        return false
    }
}