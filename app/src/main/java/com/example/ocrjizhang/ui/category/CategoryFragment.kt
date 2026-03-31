package com.example.ocrjizhang.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ocrjizhang.R
import com.example.ocrjizhang.data.local.entity.RecordType
import com.example.ocrjizhang.databinding.FragmentCategoryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
            initialValue = "",
            positiveRes = R.string.action_create,
        ) { name ->
            viewModel.addCategory(name)
        }
    }

    private fun showEditDialog(item: CategoryListItem) {
        showCategoryNameDialog(
            titleRes = R.string.category_dialog_edit,
            initialValue = item.name,
            positiveRes = R.string.action_save,
        ) { name ->
            viewModel.updateCategory(item.id, name)
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
        initialValue: String,
        positiveRes: Int,
        onConfirmed: (String) -> Unit,
    ) {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_category_form, null)
        val inputLayout = dialogBinding.findViewById<TextInputLayout>(R.id.categoryNameLayout)
        val inputView = dialogBinding.findViewById<EditText>(R.id.categoryNameEdit)
        inputView.setText(initialValue)
        inputView.setSelection(inputView.text?.length ?: 0)

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
                onConfirmed(value)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.categoryList.adapter = null
        _binding = null
    }
}
