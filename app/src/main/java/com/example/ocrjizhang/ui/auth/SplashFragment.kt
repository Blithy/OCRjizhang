package com.example.ocrjizhang.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.ocrjizhang.databinding.FragmentSplashBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SplashViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.destinationFlow.collect { destination ->
                    when (destination) {
                        SplashDestination.Home -> findNavController().navigateToHomeClearingAuth()
                        SplashDestination.Login -> {
                            val options = NavOptions.Builder()
                                .setPopUpTo(findNavController().graph.id, true)
                                .build()
                            findNavController().navigate(
                                SplashFragmentDirections.actionSplashFragmentToLoginFragment(),
                                options,
                            )
                        }
                    }
                }
            }
        }

        viewModel.decideStartDestination()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
