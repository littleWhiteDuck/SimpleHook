package me.simpleHook.ui.fragment.config

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import me.simpleHook.R
import me.simpleHook.extension.dp
import me.simpleHook.ui.WindowPreferencesManager
import me.simpleHook.ui.view.config.HookModeView

class HookModeFragment(val items: Array<String>, onItemClick: (String) -> Unit) :
    BottomSheetDialogFragment() {
    private var _root: RecyclerView? = null
    private val root get() = _root!!
    private val mAdapter by lazy {
        HookModeAdapter {
            onItemClick(it)
            dismiss()
        }
    }
    private val behavior by lazy { BottomSheetBehavior.from(root.parent as View) }
    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {
                BottomSheetBehavior.STATE_EXPANDED -> bottomSheet.background =
                    createMaterialShapeDrawable(bottomSheet)
                else -> {}
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        mAdapter.items = items
        _root = RecyclerView(requireContext()).apply {
            setPadding(5.dp, 0, 5.dp, 20.dp)
            layoutParams =
                MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                    setMargins(0, 10.dp, 0, 0)
                }
            clipToPadding = false
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = mAdapter
        }
        dialog?.window?.let {
            WindowPreferencesManager(requireContext()).applyEdgeToEdgePreference(it)
        }
        return root
    }

    override fun onStart() {
        super.onStart()
        behavior.addBottomSheetCallback(bottomSheetCallback)
    }

    override fun onStop() {
        super.onStop()
        behavior.removeBottomSheetCallback(bottomSheetCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        _root = null
    }

    private fun createMaterialShapeDrawable(bottomSheet: View): MaterialShapeDrawable {
        //Create a ShapeAppearanceModel with the same shapeAppearanceOverlay used in the style
        val shapeAppearanceModel =
            ShapeAppearanceModel.builder(context, 0, R.style.CustomShapeAppearanceBottomSheetDialog)
                .build()

        //Create a new MaterialShapeDrawable (you can't use the original MaterialShapeDrawable in the BottoSheet)
        val currentMaterialShapeDrawable = bottomSheet.background as MaterialShapeDrawable
        val newMaterialShapeDrawable = MaterialShapeDrawable(shapeAppearanceModel)

        //Copy the attributes in the new MaterialShapeDrawable
        newMaterialShapeDrawable.initializeElevationOverlay(context)
        newMaterialShapeDrawable.fillColor = currentMaterialShapeDrawable.fillColor
        newMaterialShapeDrawable.tintList = currentMaterialShapeDrawable.tintList
        newMaterialShapeDrawable.elevation = currentMaterialShapeDrawable.elevation
        newMaterialShapeDrawable.strokeWidth = currentMaterialShapeDrawable.strokeWidth
        newMaterialShapeDrawable.strokeColor = currentMaterialShapeDrawable.strokeColor
        return newMaterialShapeDrawable
    }

}


class HookModeAdapter(val onClick: (String) -> Unit) :
    RecyclerView.Adapter<HookModeAdapter.ViewHolder>() {
    var items: Array<String> = emptyArray()

    inner class ViewHolder(view: HookModeView) : RecyclerView.ViewHolder(view) {
        val title = view.title
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val hookModeView = HookModeView(parent.context)
        hookModeView.setOnClickListener {
            val str = it.getTag(R.id.item_hook_mode) as String
            onClick(str)
        }
        return ViewHolder(hookModeView)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val str = items[position]
        holder.itemView.setTag(R.id.item_hook_mode, str)
        holder.title.text = str
    }
}