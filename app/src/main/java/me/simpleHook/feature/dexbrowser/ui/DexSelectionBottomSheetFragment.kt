package me.simpleHook.feature.dexbrowser.ui

import android.content.DialogInterface
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import me.simpleHook.R
import me.simpleHook.core.base.BaseBottomFragment
import me.simpleHook.core.extension.getColorByAttr
import me.simpleHook.data.DexUiState
import me.simpleHook.databinding.LayoutDexBrowserSelectionBinding
import me.simpleHook.feature.dexbrowser.ui.adapter.DexCandidateAdapter
import me.simpleHook.feature.dexbrowser.viewmodel.DexBrowserViewModel
import kotlin.math.max

class DexSelectionBottomSheetFragment :
    BaseBottomFragment<LayoutDexBrowserSelectionBinding>() {

    private val viewModel by activityViewModels<DexBrowserViewModel> { DexBrowserViewModel.Factory }
    private val dexCandidateAdapter by lazy {
        DexCandidateAdapter(onToggle = viewModel::toggleDexCandidateSelection)
    }

    override var enableUpdateHeight: Boolean = false

    private var basePaddingBottom = 0

    override fun init() {
        basePaddingBottom = binding.root.paddingBottom
        initRecyclerView()
        initActions()
        initWindowInsets()
        collectUiState()
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (viewModel.uiState.value.showDexSelectionDialog) {
            viewModel.dismissDexSelectionDialog()
        }
        super.onDismiss(dialog)
    }

    private fun initRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = dexCandidateAdapter
    }

    private fun initActions() {
        binding.selectAllButton.setOnClickListener {
            viewModel.toggleSelectAllDexCandidates()
        }
        binding.cancelButton.setOnClickListener {
            viewModel.dismissDexSelectionDialog()
        }
        binding.confirmButton.setOnClickListener {
            viewModel.confirmDexSelection()
        }
    }

    private fun initWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val navigationInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updatePadding(bottom = basePaddingBottom + max(navigationInsets.bottom, imeInsets.bottom))
            insets
        }
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(uiState: DexUiState) {
        if (!uiState.showDexSelectionDialog) {
            dismissAllowingStateLoss()
            return
        }

        val selectedCount = uiState.selectedDexCandidateIds.size
        val allSelected = uiState.dexCandidates.isNotEmpty() &&
            selectedCount == uiState.dexCandidates.size

        dexCandidateAdapter.submit(uiState.dexCandidates, uiState.selectedDexCandidateIds)
        binding.sourceView.isVisible = !uiState.dexSelectionSource.isNullOrBlank()
        binding.sourceView.text = uiState.dexSelectionSource?.let {
            getString(R.string.dex_browser_select_dialog_source, it)
        }.orEmpty()
        binding.selectedCountView.text = getString(
            R.string.dex_browser_select_dialog_selected_count,
            selectedCount
        )
        binding.selectedCountView.setTextColor(
            requireContext().getColorByAttr(
                if (selectedCount > 0) R.attr.colorPrimary else R.attr.colorError
            )
        )
        binding.selectAllButton.text = getString(
            if (allSelected) R.string.dex_browser_unselect_all else R.string.dex_browser_select_all
        )
        binding.confirmButton.isEnabled = selectedCount > 0
    }

    companion object {
        const val TAG = "DexSelectionBottomSheet"
    }
}
