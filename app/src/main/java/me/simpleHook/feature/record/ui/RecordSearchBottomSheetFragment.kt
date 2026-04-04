package me.simpleHook.feature.record.ui

import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import me.simpleHook.core.base.BaseBottomFragment
import me.simpleHook.databinding.FragmentRecordSearchSheetBinding
import me.simpleHook.feature.record.viewmodel.RecordSearchState
import kotlin.math.max

class RecordSearchBottomSheetFragment : BaseBottomFragment<FragmentRecordSearchSheetBinding>() {
    override var enableUpdateHeight: Boolean = false

    private var basePaddingBottom = 0

    var initialState: RecordSearchState = RecordSearchState()
    var onSearchConfirmed: ((RecordSearchState) -> Unit)? = null

    override fun init() {
        basePaddingBottom = binding.root.paddingBottom
        binding.queryEdit.setText(initialState.query)
        binding.caseSensitiveSwitch.isChecked = initialState.caseSensitive
        binding.caseSensitiveRow.setOnClickListener {
            binding.caseSensitiveSwitch.isChecked = !binding.caseSensitiveSwitch.isChecked
        }
        binding.cancelButton.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.confirmButton.setOnClickListener {
            onSearchConfirmed?.invoke(
                RecordSearchState(
                    query = binding.queryEdit.text?.toString()?.trim().orEmpty(),
                    caseSensitive = binding.caseSensitiveSwitch.isChecked
                )
            )
            dismissAllowingStateLoss()
        }
        binding.queryEdit.doAfterTextChanged {
            binding.queryInputLayout.isErrorEnabled = false
        }
    }

    override fun onApplyWindowInsets(insets: WindowInsetsCompat) {
        val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
        val navigationInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
        binding.root.updatePadding(bottom = basePaddingBottom + max(navigationInsets.bottom, imeInsets.bottom))
    }

    companion object {
        const val TAG = "RecordSearchBottomSheet"
    }
}
