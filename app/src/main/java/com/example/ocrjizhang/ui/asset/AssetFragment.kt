package com.example.ocrjizhang.ui.asset

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
import com.example.ocrjizhang.databinding.FragmentAssetBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AssetFragment : Fragment() {

    private var _binding: FragmentAssetBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AssetViewModel by viewModels()

    private val adapter by lazy {
        AssetAdapter(
            onEdit = ::showEditDialog,
            onDelete = ::showDeleteDialog,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAssetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.accountList.adapter = adapter
        binding.addAccountFab.setOnClickListener {
            showCreateDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect(::render)
                }
                launch {
                    viewModel.eventFlow.collect { event ->
                        if (event is AssetEvent.Message) {
                            Snackbar.make(binding.root, event.value, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun render(state: AssetUiState) {
        binding.totalAssetValue.text = state.totalAssetLabel
        binding.accountCountValue.text = state.accountCountLabel
        binding.statusValue.text = state.statusLabel
        binding.defaultAccountsHint.text = state.defaultAccountsLabel
        binding.progressBar.isVisible = state.isLoading
        binding.accountList.isVisible = state.accounts.isNotEmpty()
        binding.emptyStateGroup.isVisible = !state.isLoading && state.accounts.isEmpty()
        binding.emptyTitle.text = state.emptyTitle
        binding.emptyBody.text = state.emptyBody
        adapter.submitList(state.accounts)
    }

    private fun showCreateDialog() {
        showAccountDialog(
            titleRes = R.string.assets_dialog_add_account,
            initialName = "",
            initialBalance = "",
            positiveRes = R.string.action_create,
        ) { name, balance ->
            viewModel.addAccount(name, balance)
        }
    }

    private fun showEditDialog(item: AssetAccountItem) {
        showAccountDialog(
            titleRes = R.string.assets_dialog_edit_account,
            initialName = item.name,
            initialBalance = item.balanceLabel.removePrefix("￥").replace(",", ""),
            positiveRes = R.string.action_save,
        ) { name, balance ->
            viewModel.updateAccount(item.id, name, balance)
        }
    }

    private fun showDeleteDialog(item: AssetAccountItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.assets_delete_title)
            .setMessage(getString(R.string.assets_delete_message, item.name))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteAccount(item.id)
            }
            .show()
    }

    private fun showAccountDialog(
        titleRes: Int,
        initialName: String,
        initialBalance: String,
        positiveRes: Int,
        onConfirmed: (String, String) -> Unit,
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_account_form, null)
        val nameLayout = dialogView.findViewById<TextInputLayout>(R.id.accountNameLayout)
        val nameEdit = dialogView.findViewById<EditText>(R.id.accountNameEdit)
        val balanceLayout = dialogView.findViewById<TextInputLayout>(R.id.accountBalanceLayout)
        val balanceEdit = dialogView.findViewById<EditText>(R.id.accountBalanceEdit)

        nameEdit.setText(initialName)
        nameEdit.setSelection(nameEdit.text?.length ?: 0)
        balanceEdit.setText(initialBalance)
        balanceEdit.setSelection(balanceEdit.text?.length ?: 0)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
            .setView(dialogView)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(positiveRes, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = nameEdit.text?.toString().orEmpty().trim()
                val balance = balanceEdit.text?.toString().orEmpty().trim()

                var hasError = false
                if (name.isBlank()) {
                    nameLayout.error = getString(R.string.error_account_name_required)
                    hasError = true
                } else {
                    nameLayout.error = null
                }

                if (balance.isBlank()) {
                    balanceLayout.error = getString(R.string.error_account_balance_required)
                    hasError = true
                } else {
                    balanceLayout.error = null
                }

                if (hasError) return@setOnClickListener

                onConfirmed(name, balance)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.accountList.adapter = null
        _binding = null
    }
}
