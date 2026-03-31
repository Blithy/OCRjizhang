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
import com.example.ocrjizhang.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginButton.setOnClickListener {
            viewModel.login()
        }
        binding.goRegisterButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
        binding.usernameEdit.doAfterTextChangedCompat(viewModel::onUsernameChanged)
        binding.passwordEdit.doAfterTextChangedCompat(viewModel::onPasswordChanged)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect(::render)
                }
                launch {
                    viewModel.eventFlow.collect { event ->
                        when (event) {
                            AuthEvent.LoginSuccess -> {
                                Snackbar.make(binding.root, R.string.message_login_success, Snackbar.LENGTH_SHORT).show()
                                findNavController().navigateToHomeClearingAuth()
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

    private fun render(state: LoginUiState) {
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
        binding.loginButton.isEnabled = !state.isLoading
        binding.progressBar.isVisible = state.isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
