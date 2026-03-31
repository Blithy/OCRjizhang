package com.example.ocrjizhang.ui.transaction

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.example.ocrjizhang.R
import com.example.ocrjizhang.data.local.entity.RecordType
import com.example.ocrjizhang.databinding.FragmentTransactionEditorBinding
import com.example.ocrjizhang.ui.auth.doAfterTextChangedCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TransactionEditorFragment : Fragment() {

    private var _binding: FragmentTransactionEditorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransactionViewModel by viewModels()
    private val args: TransactionEditorFragmentArgs by navArgs()

    private var categoryOptions: List<CategoryOption> = emptyList()
    private val transactionAdapter by lazy {
        TransactionAdapter(
            showActions = true,
            onEdit = { item ->
                viewModel.startEditing(item.id)
                binding.scrollView.post {
                    binding.scrollView.smoothScrollTo(0, 0)
                }
            },
            onDelete = { item ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.transaction_delete_title)
                    .setMessage(getString(R.string.transaction_delete_message, item.amountLabel))
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.action_delete) { _, _ ->
                        viewModel.deleteTransaction(item.id)
                    }
                    .show()
            },
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTransactionEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.transactionList.adapter = transactionAdapter
        binding.expenseButton.setOnClickListener { viewModel.onTypeSelected(RecordType.EXPENSE) }
        binding.incomeButton.setOnClickListener { viewModel.onTypeSelected(RecordType.INCOME) }
        binding.amountEdit.doAfterTextChangedCompat(viewModel::onAmountChanged)
        binding.merchantEdit.doAfterTextChangedCompat(viewModel::onMerchantChanged)
        binding.remarkEdit.doAfterTextChangedCompat(viewModel::onRemarkChanged)
        binding.categoryDropdown.setOnItemClickListener { _, _, position, _ ->
            categoryOptions.getOrNull(position)?.let { option ->
                viewModel.onCategorySelected(option.id)
            }
        }
        binding.dateButton.setOnClickListener {
            showDatePicker(viewModel.uiState.value.dateMillis)
        }
        binding.saveButton.setOnClickListener {
            viewModel.submit()
        }
        binding.secondaryButton.setOnClickListener {
            viewModel.clearForm()
        }

        viewModel.applyPrefill(
            amount = args.prefillAmount,
            merchant = args.prefillMerchant,
            remark = args.prefillRemark,
            dateMillis = args.prefillDateMillis.takeIf { it > 0L },
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch {
                    viewModel.eventFlow.collect { event ->
                        if (event is TransactionEvent.Message) {
                            Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun render(state: TransactionUiState) {
        binding.amountLayout.error = null
        binding.transactionList.isVisible = state.transactions.isNotEmpty()
        binding.emptyStateGroup.isVisible = state.transactions.isEmpty()
        binding.emptyTitle.text = state.emptyTitle
        binding.emptyBody.text = state.emptyBody
        binding.saveButton.text = state.submitLabel
        binding.secondaryButton.text = state.secondaryLabel
        binding.dateButton.text = state.dateLabel

        binding.typeToggleGroup.check(
            if (state.selectedType == RecordType.EXPENSE) {
                R.id.expenseButton
            } else {
                R.id.incomeButton
            },
        )

        if (binding.amountEdit.text?.toString() != state.amountInput) {
            binding.amountEdit.setText(state.amountInput)
            binding.amountEdit.setSelection(state.amountInput.length)
        }
        if (binding.merchantEdit.text?.toString() != state.merchantInput) {
            binding.merchantEdit.setText(state.merchantInput)
            binding.merchantEdit.setSelection(state.merchantInput.length)
        }
        if (binding.remarkEdit.text?.toString() != state.remarkInput) {
            binding.remarkEdit.setText(state.remarkInput)
            binding.remarkEdit.setSelection(state.remarkInput.length)
        }

        val categoryNames = state.categories.map { it.name }
        if (categoryOptions != state.categories) {
            categoryOptions = state.categories
            binding.categoryDropdown.setAdapter(
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    categoryNames,
                ),
            )
        }
        val selectedCategoryName = state.categories
            .firstOrNull { it.id == state.selectedCategoryId }
            ?.name
            .orEmpty()
        if (binding.categoryDropdown.text?.toString() != selectedCategoryName) {
            binding.categoryDropdown.setText(selectedCategoryName, false)
        }

        transactionAdapter.submitList(state.transactions)
    }

    private fun showDatePicker(currentMillis: Long) {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentMillis }
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                viewModel.onDateSelected(picked.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.transactionList.adapter = null
        _binding = null
    }
}
