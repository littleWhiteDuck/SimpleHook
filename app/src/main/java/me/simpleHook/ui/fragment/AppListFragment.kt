package me.simpleHook.ui.fragment

import android.annotation.SuppressLint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.simpleHook.R
import me.simpleHook.adapter.AppListAdapter
import me.simpleHook.databinding.FragmentAppListBinding
import me.simpleHook.ui.custom.MyFastScroller


class AppListFragment(private val tagFragment: String = "user") : Fragment() {

    private lateinit var binding: FragmentAppListBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAppListBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initView() {
        val appAdapter =
            if (tagFragment == "user") AppListAdapter.getAppSelectAdapter1() else AppListAdapter.getAppSelectAdapter2()
        binding.recyclerView.apply {
            adapter = appAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val position = parent.getChildAdapterPosition(view)
                    if (position == RecyclerView.NO_POSITION) {
                        return
                    }

                    if (position == parent.adapter!!.itemCount - 1) {
                        outRect.bottom = 100
                    }
                }
            })
        }
 /*       val verticalThumbDrawable =
            resources.getDrawable(R.drawable.thumb_drawable) as StateListDrawable
        val verticalTrackDrawable: Drawable = resources.getDrawable(R.drawable.line_drawable)
        val horizontalThumbDrawable =
            resources.getDrawable(R.drawable.thumb_drawable) as StateListDrawable
        val horizontalTrackDrawable: Drawable = resources.getDrawable(R.drawable.line_drawable)
        MyFastScroller(
            binding.recyclerView,
            verticalThumbDrawable,
            verticalTrackDrawable,
            horizontalThumbDrawable,
            horizontalTrackDrawable,
            resources.getDimensionPixelSize(R.dimen.fastscroll_default_thickness),
            resources.getDimensionPixelSize(R.dimen.fastscroll_minimum_range),
            resources.getDimensionPixelOffset(R.dimen.fastscroll_margin)
        )*/
    }

}