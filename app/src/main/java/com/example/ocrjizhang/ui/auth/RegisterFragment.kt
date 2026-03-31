package com.example.ocrjizhang.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.ocrjizhang.R
import com.example.ocrjizhang.databinding.FragmentRegisterBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.registerButton.setOnClickListener {
            viewModel.register()
        }
        binding.goLoginButton.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.usernameEdit.doAfterTextChangedCompat(viewModel::onUsernameChanged)
        binding.passwordEdit.doAfterTextChangedCompat(viewModel::onPasswordChanged)
        binding.nicknameEdit.doAfterTextChangedCompat(viewModel::onNicknameChanged)
        binding.emailEdit.doAfterTextChangedCompat(viewModel::onEmailChanged)
        binding.phoneEdit.doAfterTextChangedCompat(viewModel::onPhoneChanged)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch {
                    viewModel.eventFlow.collect { event ->
                        when (event) {
                            AuthEvent.RegisterSuccess -> {
                                Snackbar.make(binding.root, R.string.message_register_success, Snackbar.LENGTH_SHORT).show()
                                findNavController().popBackStack()
                            }
                            is AuthEvent.Error -> {
                                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_LONG).show()
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun render(state: RegisterUiState) {
        binding.usernameLayout.error = state.username.error
        binding.passwordLayout.error = state.password.error
        if (binding.usernameEdit.text?.toString() != state.username.value) {
            binding.usernameEdit.setText(state.username.value)
            binding.usernameEdit.setSelection(state.username.value.length)
        }
        if (binding.passwordEdit.text?.toString() != state.password.value) {
            binding.passwordEdit.setText(state.password.value)
            binding.passwordEdit.setSelection(state.password.value.length)
        }
        if (binding.nicknameEdit.text?.toString() != state.nickname.value) {
            binding.nicknameEdit.setText(state.nickname.value)
            binding.nicknameEdit.setSelection(state.nickname.value.length)
        }
        if (binding.emailEdit.text?.toString() != state.email.value) {
            binding.emailEdit.setText(state.email.value)
            binding.emailEdit.setSelection(state.email.value.length)
        }
        if (binding.phoneEdit.text?.toString() != state.phone.value) {
            binding.phoneEdit.setText(state.phone.value)
            binding.phoneEdit.setSelection(state.phone.value.length)
        }
        binding.progressBar.isVisible = state.isLoading
        binding.registerButton.isEnabled = !state.isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
