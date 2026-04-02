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
import com.example.ocrjizhang.R
import com.example.ocrjizhang.databinding.FragmentHomeBinding
import com.example.ocrjizhang.ui.transaction.TransactionAdapter
import com.google.android.material.snackbar.Snackbar
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
            onOpen = { item ->
                openTransactionEditor(item.id)
            },
            onEdit = { item ->
                openTransactionEditor(item.id)
            },
            onDelete = { },
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
        binding.ocrCard.setOnClickListener(::openOcr)
        binding.ocrStartButton.setOnClickListener(::openOcr)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.welcomeTitle.text = state.welcomeTitle
                        binding.welcomeSubtitle.text = state.welcomeSubtitle
                        binding.summaryPeriodLabel.text = state.summaryPeriodLabel
                        binding.incomeValue.text = state.incomeLabel
                        binding.expenseValue.text = state.expenseLabel
                        binding.surplusValue.text = state.surplusLabel
                        binding.statusAccountValue.text = state.statusAccountValue
                        binding.statusRecordsValue.text = state.statusRecordsValue
                        binding.statusLatestValue.text = state.statusLatestValue
                        binding.recentTransactionList.isVisible = state.recentTransactions.isNotEmpty()
                        binding.recentEmptyGroup.isVisible = state.recentTransactions.isEmpty()
                        binding.recentEmptyTitle.text = state.recentEmptyTitle
                        binding.recentEmptyBody.text = state.recentEmptyBody
                        recentAdapter.submitList(state.recentTransactions)
                    }
                }
                launch {
                    viewModel.eventFlow.collect { event ->
                        if (event is HomeEvent.Message) {
                            Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun openTransactionEditor(transactionId: Long) {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeFragmentToTransactionEditorFragment(
                editTransactionId = transactionId,
            ),
        )
    }

    private fun openOcr(@Suppress("UNUSED_PARAMETER") view: View) {
        findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToOcrFragment())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recentTransactionList.adapter = null
        _binding = null
    }
}
