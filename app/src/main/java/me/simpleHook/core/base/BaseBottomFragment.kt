package me.simpleHook.core.base

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import me.simpleHook.R
import me.simpleHook.core.extension.inflateBinding
import me.simpleHook.core.ui.WindowPreferencesManager
import me.simpleHook.core.utils.OSUtil
import me.simpleHook.core.utils.WindowUtil


abstract class BaseBottomFragment<VB : ViewBinding> : BottomSheetDialogFragment(), IBinding<VB> {

    private var _binding: VB? = null

    override val binding: VB get() = _binding!!
    open var enableUpdateHeight = true
    private val behavior by lazy { BottomSheetBehavior.from(binding.root.parent as View) }
    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {}

        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }

    override fun onStart() {
        super.onStart()
        behavior.addBottomSheetCallback(bottomSheetCallback)
    }

    override fun onStop() {
        super.onStop()
        behavior.removeBottomSheetCallback(bottomSheetCallback)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        object : BottomSheetDialog(requireContext(), theme) {
            override fun onAttachedToWindow() {
                super.onAttachedToWindow()
                window?.let {
                    if (OSUtil.atLeastS()) {
                        it.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                        it.attributes.blurBehindRadius = 50
                        it.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = inflateBinding(layoutInflater)
        dialog?.window?.let {
            WindowPreferencesManager(requireContext()).applyEdgeToEdgePreference(it)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (enableUpdateHeight) {
            ViewCompat.setOnApplyWindowInsetsListener(requireView()) { _, insets: WindowInsetsCompat ->
                updateBottomSheetHeights()
                insets
            }
        }
        init()
    }

    private fun updateBottomSheetHeights() {
        val params = binding.root.layoutParams
        params.height = WindowUtil.getAppHeight(requireActivity())
        binding.root.layoutParams = params
        behavior.isFitToContents = true
    }

    abstract fun init()


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




