package com.example.ocrjizhang.ui.ocr

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.ocrjizhang.R
import com.example.ocrjizhang.databinding.FragmentOcrBinding
import com.example.ocrjizhang.utils.ImageFileUtils
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OcrFragment : Fragment() {

    private var _binding: FragmentOcrBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OcrViewModel by viewModels()
    private val historyAdapter by lazy {
        OcrHistoryAdapter(
            onUseRecord = viewModel::fillHistoryRecord,
        )
    }
    private var pendingCameraUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let(viewModel::onImageSelected)
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val capturedUri = pendingCameraUri
        pendingCameraUri = null
        if (success && capturedUri != null) {
            viewModel.onImageSelected(capturedUri)
        } else if (!success) {
            Snackbar.make(
                binding.root,
                getString(R.string.ocr_message_camera_cancelled),
                Snackbar.LENGTH_SHORT,
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOcrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.historyList.adapter = historyAdapter
        binding.captureImageButton.setOnClickListener {
            runCatching {
                ImageFileUtils.createCameraCaptureUri(requireContext())
            }.onSuccess { uri ->
                pendingCameraUri = uri
                takePictureLauncher.launch(uri)
            }.onFailure { throwable ->
                Snackbar.make(
                    binding.root,
                    throwable.message ?: getString(R.string.ocr_message_camera_open_failed),
                    Snackbar.LENGTH_SHORT,
                ).show()
            }
        }
        binding.pickImageButton.setOnClickListener {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
        binding.recognizeButton.setOnClickListener {
            viewModel.recognizeSelectedImage()
        }
        binding.fillTransactionButton.setOnClickListener {
            viewModel.fillCurrentResult()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch {
                    viewModel.eventFlow.collect { event ->
                        when (event) {
                            is OcrEvent.Message -> {
                                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                            }
                            is OcrEvent.NavigateToTransactionEditor -> {
                                findNavController().navigate(
                                    OcrFragmentDirections.actionOcrFragmentToTransactionEditorFragment(
                                        prefillAmount = event.payload.amount,
                                        prefillMerchant = event.payload.merchant,
                                        prefillRemark = event.payload.remark,
                                        prefillDateMillis = event.payload.dateMillis ?: -1L,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun render(state: OcrUiState) {
        binding.imageHint.text = state.selectedImageHint
        binding.progressIndicator.isVisible = state.isProcessing || state.isImagePreparing
        binding.recognizeButton.isEnabled = state.canRecognize
        binding.fillTransactionButton.isEnabled = state.canFillTransaction
        binding.resultCard.isVisible = state.rawText.isNotBlank()
        binding.previewCard.isVisible = state.selectedImagePath != null
        binding.historyList.isVisible = state.history.isNotEmpty()
        binding.historyEmptyGroup.isVisible = state.history.isEmpty()
        binding.historyEmptyTitle.text = state.historyEmptyTitle
        binding.historyEmptyBody.text = state.historyEmptyBody

        binding.amountValue.text = state.parsedAmount
        binding.dateValue.text = state.parsedDate
        binding.merchantValue.text = state.parsedMerchant
        binding.rawTextValue.text = state.rawText.ifBlank { "识别结果会展示在这里" }

        if (state.selectedImagePath != null) {
            val bitmap = BitmapFactory.decodeFile(state.selectedImagePath)
            binding.previewImage.setImageBitmap(bitmap)
        } else {
            binding.previewImage.setImageDrawable(null)
        }

        historyAdapter.submitList(state.history)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.historyList.adapter = null
        _binding = null
    }
}
