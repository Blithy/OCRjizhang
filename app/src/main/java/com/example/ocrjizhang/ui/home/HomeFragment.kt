package com.example.ocrjizhang.ui.home

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
import com.example.ocrjizhang.databinding.FragmentHomeBinding
import com.example.ocrjizhang.ui.transaction.TransactionAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private val recentAdapter by lazy {
        TransactionAdapter(
            showActions = false,
            onEdit = {},
            onDelete = {},
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recentTransactionList.adapter = recentAdapter
        binding.addRecordCard.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToTransactionEditorFragment())
        }
        binding.ocrCard.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToOcrFragment())
        }
        binding.categoryCard.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToCategoryFragment())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.incomeValue.text = state.incomeLabel
                    binding.expenseValue.text = state.expenseLabel
                    binding.surplusValue.text = state.surplusLabel
                    binding.recentTransactionList.isVisible = state.recentTransactions.isNotEmpty()
                    binding.recentEmptyGroup.isVisible = state.recentTransactions.isEmpty()
                    binding.recentEmptyTitle.text = state.recentEmptyTitle
                    binding.recentEmptyBody.text = state.recentEmptyBody
                    recentAdapter.submitList(state.recentTransactions)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recentTransactionList.adapter = null
        _binding = null
    }
}
