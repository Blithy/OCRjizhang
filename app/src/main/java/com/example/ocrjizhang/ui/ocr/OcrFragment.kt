package com.example.ocrjizhang.ui.ocr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.ocrjizhang.databinding.FragmentOcrBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OcrFragment : Fragment() {

    private var _binding: FragmentOcrBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOcrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
