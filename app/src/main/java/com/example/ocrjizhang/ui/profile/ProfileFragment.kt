package com.example.ocrjizhang.ui.profile

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.ocrjizhang.R
import com.example.ocrjizhang.databinding.DialogUserManagementFormBinding
import com.example.ocrjizhang.databinding.FragmentProfileBinding
import com.example.ocrjizhang.ui.auth.navigateToLoginClearingBackStack
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.openUserManageButton.setOnClickListener {
            showUserManagementDialog(viewModel.uiState.value)
        }

        binding.categoryManageCard.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_categoryFragment)
        }

        binding.syncEntryCard.setOnClickListener {
            viewModel.syncNow()
        }

        binding.logoutButton.setOnClickListener {
            viewModel.logout()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        val nickname = state.nickname.ifBlank { getString(R.string.profile_header_nickname_placeholder) }
                        binding.profileNicknameTitle.text = nickname
                        binding.profileAvatarInitial.text = state.nickname
                            .takeIf { it.isNotBlank() }
                            ?.take(1)
                            ?: getString(R.string.profile_avatar_default)
                        binding.profileContactSubtitle.text = buildContactSubtitle(
                            email = state.email,
                            phone = state.phone,
                        )

                        binding.openUserManageButton.isEnabled = !state.isUpdatingUser
                        binding.openUserManageButton.alpha = if (state.isUpdatingUser) 0.65f else 1f

                        binding.syncEntryCard.isEnabled = !state.isSyncing
                        binding.syncEntryCard.alpha = if (state.isSyncing) 0.65f else 1f
                        binding.syncEntryTitle.text = getString(
                            if (state.isSyncing) R.string.profile_sync_action_loading
                            else R.string.profile_sync_action,
                        )
                    }
                }

                launch {
                    viewModel.eventFlow.collect { event ->
                        when (event) {
                            ProfileEvent.LogoutSuccess -> {
                                Snackbar.make(binding.root, R.string.message_logout_success, Snackbar.LENGTH_SHORT).show()
                                findNavController().navigateToLoginClearingBackStack()
                            }

                            ProfileEvent.UserUpdated -> {
                                Snackbar.make(binding.root, R.string.profile_user_update_success, Snackbar.LENGTH_SHORT).show()
                            }

                            is ProfileEvent.Message -> {
                                Snackbar.make(binding.root, event.value, Snackbar.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showUserManagementDialog(state: ProfileUiState) {
        val dialogBinding = DialogUserManagementFormBinding.inflate(layoutInflater)
        dialogBinding.nicknameEdit.setText(state.nickname)
        dialogBinding.emailEdit.setText(state.email)
        dialogBinding.phoneEdit.setText(state.phone)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile_user_dialog_title)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save, null)
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                dialogBinding.passwordLayout.error = null
                val password = dialogBinding.passwordEdit.text?.toString().orEmpty()
                if (password.isNotBlank() && password.length < 6) {
                    dialogBinding.passwordLayout.error = getString(R.string.error_password_length)
                    return@setOnClickListener
                }

                viewModel.updateUserProfile(
                    nickname = dialogBinding.nicknameEdit.text?.toString().orEmpty(),
                    email = dialogBinding.emailEdit.text?.toString().orEmpty(),
                    phone = dialogBinding.phoneEdit.text?.toString().orEmpty(),
                    password = password,
                )
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun buildContactSubtitle(email: String, phone: String): String {
        val emailValue = email.ifBlank { null }
        val phoneValue = phone.ifBlank { null }
        return when {
            emailValue != null && phoneValue != null -> "$emailValue  |  $phoneValue"
            emailValue != null -> emailValue
            phoneValue != null -> phoneValue
            else -> getString(R.string.profile_header_contact_placeholder)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
