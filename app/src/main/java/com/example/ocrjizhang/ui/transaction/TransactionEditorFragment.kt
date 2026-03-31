package com.example.ocrjizhang.ui.transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.ocrjizhang.databinding.FragmentTransactionEditorBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TransactionEditorFragment : Fragment() {

    private var _binding: FragmentTransactionEditorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTransactionEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
