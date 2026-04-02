package com.example.ocrjizhang.ui.transaction

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import com.example.ocrjizhang.R
import com.example.ocrjizhang.data.local.entity.RecordType
import com.example.ocrjizhang.databinding.BottomSheetTransactionEntryBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TransactionEntryBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTransactionEntryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransactionViewModel by viewModels()
    private var latestState = TransactionUiState()

    private val categoryAdapter by lazy {
        QuickCategoryAdapter { option ->
            viewModel.onCategorySelected(option.id)
        }
    }

    override fun getTheme(): Int = R.style.ThemeOverlay_OCRjizhang_TransactionEntryBottomSheet

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            behavior.skipCollapsed = true
            behavior.isDraggable = true
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            setCanceledOnTouchOutside(true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetTransactionEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val bottomSheet =
            (dialog as? BottomSheetDialog)?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
        bottomSheet?.setBackgroundResource(android.R.color.transparent)
        bottomSheet?.requestLayout()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            skipCollapsed = true
            isDraggable = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.categoryGrid.layoutManager = GridLayoutManager(requireContext(), 5)
        binding.categoryGrid.adapter = categoryAdapter

        binding.cancelButton.setOnClickListener { dismiss() }
        binding.expenseButton.setOnClickListener { viewModel.onTypeSelected(RecordType.EXPENSE) }
        binding.incomeButton.setOnClickListener { viewModel.onTypeSelected(RecordType.INCOME) }
        binding.dateButton.setOnClickListener {
            showDatePicker(viewModel.uiState.value.dateMillis)
        }
        binding.detailButton.setOnClickListener { showDetailDialog() }
        binding.accountButton.setOnClickListener { showAccountPicker() }
        binding.manageCategoryButton.setOnClickListener {
            rootNavController().navigate(R.id.categoryFragment)
            dismiss()
        }
        binding.ocrAssistButton.setOnClickListener {
            val navController = rootNavController()
            if (navController.currentDestination?.id != R.id.ocrFragment) {
                navController.navigate(R.id.ocrFragment)
            }
            dismiss()
        }
        binding.deleteCurrentButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.transaction_delete_title)
                .setMessage(R.string.transaction_delete_current_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete) { _, _ ->
                    viewModel.deleteCurrentTransaction()
                }
                .show()
        }
        binding.saveButton.setOnClickListener { viewModel.saveAndClose() }
        binding.secondaryButton.setOnClickListener { viewModel.saveAndContinue() }
        binding.deleteButton.setOnClickListener { viewModel.removeLastAmountChar() }
        binding.clearButton.setOnClickListener { viewModel.clearAmount() }

        bindKeypad()

        viewModel.applyPrefill(
            amount = requireArguments().getString(ARG_PREFILL_AMOUNT).orEmpty(),
            merchant = requireArguments().getString(ARG_PREFILL_MERCHANT).orEmpty(),
            remark = requireArguments().getString(ARG_PREFILL_REMARK).orEmpty(),
            dateMillis = requireArguments().getLong(ARG_PREFILL_DATE_MILLIS).takeIf { it > 0L },
        )
        viewModel.requestEdit(
            requireArguments().getLong(ARG_EDIT_TRANSACTION_ID).takeIf { it > 0L },
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch {
                    viewModel.eventFlow.collect { event ->
                        when (event) {
                            is TransactionEvent.Message -> {
                                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                            }

                            TransactionEvent.SavedAndClose -> dismiss()
                        }
                    }
                }
            }
        }
    }

    private fun render(state: TransactionUiState) {
        latestState = state
        binding.ocrPrefillCard.isVisible = state.showOcrPrefillHint
        binding.ocrPrefillTitle.text = state.ocrPrefillTitle
        binding.ocrPrefillBody.text = state.ocrPrefillBody
        binding.deleteCurrentButton.isVisible = state.showDeleteButton
        binding.saveButton.text = state.submitLabel
        binding.secondaryButton.text = state.secondaryLabel
        binding.dateButton.text = state.dateLabel
        binding.detailButton.text = state.detailLabel
        binding.accountButton.text = state.accountLabel
        binding.amountValue.text = state.amountDisplay
        binding.amountValue.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (state.selectedType == RecordType.EXPENSE) {
                    R.color.negative
                } else {
                    R.color.positive
                },
            ),
        )
        binding.typeToggleGroup.check(
            if (state.selectedType == RecordType.EXPENSE) {
                R.id.expenseButton
            } else {
                R.id.incomeButton
            },
        )
        categoryAdapter.submitList(state.categories)
    }

    private fun bindKeypad() {
        val digitMap = mapOf(
            binding.key1 to "1",
            binding.key2 to "2",
            binding.key3 to "3",
            binding.key4 to "4",
            binding.key5 to "5",
            binding.key6 to "6",
            binding.key7 to "7",
            binding.key8 to "8",
            binding.key9 to "9",
            binding.key0 to "0",
            binding.key00 to "00",
        )
        digitMap.forEach { (button, token) ->
            button.setOnClickListener { viewModel.appendAmountDigit(token) }
        }
        binding.keyDot.setOnClickListener { viewModel.appendDecimalPoint() }
    }

    private fun showAccountPicker() {
        val accounts = latestState.accounts
        if (accounts.isEmpty()) {
            Snackbar.make(binding.root, R.string.transaction_account_empty, Snackbar.LENGTH_SHORT).show()
            return
        }

        val selectedIndex = accounts.indexOfFirst { it.isSelected }.coerceAtLeast(0)
        val labels = accounts.map { "${it.symbol} ${it.name}" }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.transaction_account_dialog_title)
            .setSingleChoiceItems(labels, selectedIndex) { dialog, which ->
                viewModel.onAccountSelected(accounts[which].id)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showDatePicker(currentMillis: Long) {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentMillis }
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance().apply {
                    timeInMillis = currentMillis
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                showTimePicker(picked)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    private fun showTimePicker(calendar: Calendar) {
        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                viewModel.onDateSelected(calendar.timeInMillis)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true,
        ).show()
    }

    private fun showDetailDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_transaction_details, null)
        val merchantEdit = dialogView.findViewById<EditText>(R.id.merchantEdit)
        val remarkEdit = dialogView.findViewById<EditText>(R.id.remarkEdit)

        merchantEdit.setText(latestState.merchantInput)
        merchantEdit.setSelection(merchantEdit.text?.length ?: 0)
        remarkEdit.setText(latestState.remarkInput)
        remarkEdit.setSelection(remarkEdit.text?.length ?: 0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.transaction_detail_dialog_title)
            .setView(dialogView)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save) { _, _ ->
                viewModel.onMerchantChanged(merchantEdit.text?.toString().orEmpty())
                viewModel.onRemarkChanged(remarkEdit.text?.toString().orEmpty())
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.categoryGrid.adapter = null
        _binding = null
    }

    private fun rootNavController(): NavController =
        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)

    companion object {
        private const val TAG = "transaction_entry_bottom_sheet"
        private const val ARG_EDIT_TRANSACTION_ID = "edit_transaction_id"
        private const val ARG_PREFILL_AMOUNT = "prefill_amount"
        private const val ARG_PREFILL_MERCHANT = "prefill_merchant"
        private const val ARG_PREFILL_REMARK = "prefill_remark"
        private const val ARG_PREFILL_DATE_MILLIS = "prefill_date_millis"

        fun show(
            fragmentManager: FragmentManager,
            editTransactionId: Long? = null,
            prefillAmount: String = "",
            prefillMerchant: String = "",
            prefillRemark: String = "",
            prefillDateMillis: Long? = null,
        ) {
            if (fragmentManager.isStateSaved) return
            if (fragmentManager.findFragmentByTag(TAG) != null) return

            TransactionEntryBottomSheet().apply {
                arguments = bundleOf(
                    ARG_EDIT_TRANSACTION_ID to (editTransactionId ?: -1L),
                    ARG_PREFILL_AMOUNT to prefillAmount,
                    ARG_PREFILL_MERCHANT to prefillMerchant,
                    ARG_PREFILL_REMARK to prefillRemark,
                    ARG_PREFILL_DATE_MILLIS to (prefillDateMillis ?: -1L),
                )
            }.show(fragmentManager, TAG)
        }
    }
}
