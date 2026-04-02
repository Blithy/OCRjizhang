package com.example.ocrjizhang.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.GridLayoutManager
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.ocrjizhang.R
import com.example.ocrjizhang.data.local.entity.RecordType
import com.example.ocrjizhang.databinding.FragmentCategoryBinding
import com.example.ocrjizhang.ui.transaction.TransactionEntryBottomSheet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CategoryFragment : Fragment() {

    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CategoryViewModel by viewModels()

    private val adapter by lazy {
        CategoryAdapter(
            onEdit = ::showEditDialog,
            onDelete = ::showDeleteDialog,
        )
    }
    private val shouldReturnToTransactionEntry: Boolean
        get() = arguments?.getBoolean(ARG_RETURN_TO_TRANSACTION_ENTRY) == true
    private val returnRecordType: RecordType
        get() = runCatching {
            RecordType.valueOf(arguments?.getString(ARG_RETURN_RECORD_TYPE).orEmpty())
        }.getOrDefault(RecordType.EXPENSE)
    private val returnBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            handleExit()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.categoryList.adapter = adapter
        if (shouldReturnToTransactionEntry) {
            returnBackCallback.isEnabled = true
            requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                returnBackCallback,
            )
            requireActivity().findViewById<MaterialToolbar>(R.id.topAppBar)
                .setNavigationOnClickListener {
                    handleExit()
                }
        }
        binding.expenseButton.setOnClickListener {
            viewModel.onTypeSelected(RecordType.EXPENSE)
        }
        binding.incomeButton.setOnClickListener {
            viewModel.onTypeSelected(RecordType.INCOME)
        }
        binding.addCategoryButton.setOnClickListener {
            showCreateDialog(viewModel.uiState.value.selectedType)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect(::render)
                }
                launch {
                    viewModel.eventFlow.collect { event ->
                        if (event is CategoryEvent.Message) {
                            Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun render(state: CategoryUiState) {
        binding.progressBar.isVisible = state.isLoading
        binding.categoryList.isVisible = state.categories.isNotEmpty()
        binding.emptyStateGroup.isVisible = !state.isLoading && state.categories.isEmpty()
        binding.emptyTitle.text = state.emptyTitle
        binding.emptyBody.text = state.emptyBody
        binding.addCategoryButton.text = state.actionLabel
        binding.headerSummary.text = if (state.selectedType == RecordType.EXPENSE) {
            getString(R.string.category_summary_expense)
        } else {
            getString(R.string.category_summary_income)
        }
        binding.typeHint.text = if (state.selectedType == RecordType.EXPENSE) {
            getString(R.string.category_hint_expense)
        } else {
            getString(R.string.category_hint_income)
        }
        binding.typeToggleGroup.check(
            if (state.selectedType == RecordType.EXPENSE) {
                R.id.expenseButton
            } else {
                R.id.incomeButton
            },
        )
        adapter.submitList(state.categories)
    }

    private fun showCreateDialog(type: RecordType) {
        val title = if (type == RecordType.EXPENSE) {
            R.string.category_dialog_add_expense
        } else {
            R.string.category_dialog_add_income
        }
        showCategoryNameDialog(
            titleRes = title,
            type = type,
            initialValue = "",
            initialIconKey = CategoryIconRegistry.defaultKeyForType(type),
            positiveRes = R.string.action_create,
        ) { name, iconKey ->
            viewModel.addCategory(name, iconKey)
        }
    }

    private fun showEditDialog(item: CategoryListItem) {
        showCategoryNameDialog(
            titleRes = R.string.category_dialog_edit,
            type = viewModel.uiState.value.selectedType,
            initialValue = item.name,
            initialIconKey = item.iconKey,
            positiveRes = R.string.action_save,
        ) { name, iconKey ->
            viewModel.updateCategory(item.id, name, iconKey)
        }
    }

    private fun showDeleteDialog(item: CategoryListItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.category_delete_title)
            .setMessage(getString(R.string.category_delete_message, item.name))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteCategory(item.id)
            }
            .show()
    }

    private fun showCategoryNameDialog(
        titleRes: Int,
        type: RecordType,
        initialValue: String,
        initialIconKey: String,
        positiveRes: Int,
        onConfirmed: (String, String) -> Unit,
    ) {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_category_form, null)
        val inputLayout = dialogBinding.findViewById<TextInputLayout>(R.id.categoryNameLayout)
        val inputView = dialogBinding.findViewById<EditText>(R.id.categoryNameEdit)
        val iconRecycler = dialogBinding.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.iconRecycler)
        inputView.setText(initialValue)
        inputView.setSelection(inputView.text?.length ?: 0)
        var selectedIconKey = initialIconKey
            .takeIf { it.isNotBlank() }
            ?: CategoryIconRegistry.defaultKeyForType(type)
        lateinit var iconAdapter: CategoryIconPickerAdapter
        iconAdapter = CategoryIconPickerAdapter { option ->
            selectedIconKey = option.key
            iconAdapter.updateSelectedKey(option.key)
        }
        iconRecycler.layoutManager = GridLayoutManager(requireContext(), 4)
        iconRecycler.adapter = iconAdapter
        iconRecycler.setHasFixedSize(true)
        iconRecycler.isNestedScrollingEnabled = false
        iconAdapter.submitOptions(
            CategoryIconRegistry.selectableOptions(type),
            selectedIconKey,
        )

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
            .setView(dialogBinding)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(positiveRes, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = inputView.text?.toString().orEmpty().trim()
                if (value.isBlank()) {
                    inputLayout.error = getString(R.string.error_category_name_required)
                    return@setOnClickListener
                }
                inputLayout.error = null
                onConfirmed(value, selectedIconKey)
                dialog.dismiss()
            }
        }
        dialog.show()
        dialog.setOnDismissListener {
            iconRecycler.adapter = null
        }
    }

    private fun handleExit() {
        if (shouldReturnToTransactionEntry) {
            findNavController().popBackStack()
            requireActivity().window.decorView.post {
                TransactionEntryBottomSheet.show(
                    fragmentManager = parentFragmentManager,
                    initialType = returnRecordType,
                )
            }
        } else {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (shouldReturnToTransactionEntry) {
            requireActivity().findViewById<MaterialToolbar>(R.id.topAppBar)
                .setNavigationOnClickListener {
                    findNavController().navigateUp()
                }
        }
        binding.categoryList.adapter = null
        _binding = null
    }

    companion object {
        const val ARG_RETURN_TO_TRANSACTION_ENTRY = "returnToTransactionEntry"
        const val ARG_RETURN_RECORD_TYPE = "returnRecordType"
    }
}
