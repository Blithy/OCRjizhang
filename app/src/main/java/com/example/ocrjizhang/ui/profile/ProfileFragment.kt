package com.example.ocrjizhang.ui.profile

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
import com.example.ocrjizhang.databinding.FragmentProfileBinding
import com.example.ocrjizhang.ui.auth.navigateToLoginClearingBackStack
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

        binding.syncButton.setOnClickListener {
            viewModel.syncNow()
        }

        binding.manageCategoryButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_categoryFragment)
        }

        binding.logoutButton.setOnClickListener {
            viewModel.logout()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.syncButton.isEnabled = !state.isSyncing
                        binding.syncButton.text = getString(
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

                            is ProfileEvent.Message -> {
                                Snackbar.make(binding.root, event.value, Snackbar.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
