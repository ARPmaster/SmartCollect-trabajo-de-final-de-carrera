/* Pantalla de "Nueva Publicación": formulario para dar de alta un ítem, captura o selección de
fotos, reconocimiento automático opcional y publicación (incluyendo el aviso de posible duplicado).*/
package com.example.aicollect.presentation.newpost

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.aicollect.R
import com.example.aicollect.databinding.FragmentNewPostBinding
import com.example.aicollect.databinding.ItemNewPostAddPhotoTileBinding
import com.example.aicollect.databinding.ItemNewPostPhotoThumbnailBinding
import com.example.aicollect.presentation.asString
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewPostFragment : Fragment() {

    private var _binding: FragmentNewPostBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewPostViewModel by activityViewModels()

    private var selectedSport: String? = null
    private var selectedCondition: String? = null
    private var pendingCameraUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingCameraUri?.let { handlePickedImage(it) }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Snackbar.make(binding.root, R.string.new_post_camera_permission_denied, Snackbar.LENGTH_LONG).show()
        }
    }

    private val pickPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) handlePickedImage(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNewPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.updatePadding(top = systemBars.top, bottom = maxOf(systemBars.bottom, ime.bottom))
            insets
        }

        binding.boxUpload.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.btnAddPhoto.setOnClickListener { requestPhotoFromCamera() }
        binding.btnPickGallery.setOnClickListener {
            pickPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.btnPublish.setOnClickListener { onPublishClicked() }

        setUpDropdown(binding.btnSport, binding.tvSportValue, R.array.sport_options) { selectedSport = it }
        setUpDropdown(binding.btnCondition, binding.tvConditionValue, R.array.filter_condition_options) { selectedCondition = it }

        renderPhotos()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.recognitionState.collect { render(it) } }
                launch { viewModel.saveState.collect { renderSave(it) } }
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        prefillFromCandidate()
    }

    private fun renderPhotos() {
        val photos = viewModel.photos
        binding.uploadPlaceholder.visibility = if (photos.isEmpty()) View.VISIBLE else View.GONE
        binding.photoThumbnailsRow.visibility = if (photos.isEmpty()) View.GONE else View.VISIBLE
        binding.photoThumbnailsRow.removeAllViews()

        photos.forEachIndexed { index, bytes ->
            val thumbnailBinding = ItemNewPostPhotoThumbnailBinding.inflate(layoutInflater, binding.photoThumbnailsRow, false)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            thumbnailBinding.ivThumbnail.setImageBitmap(bitmap)
            thumbnailBinding.btnRemove.setOnClickListener {
                viewModel.removePhotoAt(index)
                renderPhotos()
            }
            if (index > 0) applySpacing(thumbnailBinding.root)
            binding.photoThumbnailsRow.addView(thumbnailBinding.root)
        }

        if (viewModel.canAddMorePhotos) {
            val addTileBinding = ItemNewPostAddPhotoTileBinding.inflate(layoutInflater, binding.photoThumbnailsRow, false)
            addTileBinding.root.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            if (photos.isNotEmpty()) applySpacing(addTileBinding.root)
            addTileBinding.root.setOnClickListener { showPhotoSourceMenu(it) }
            binding.photoThumbnailsRow.addView(addTileBinding.root)
        }
    }

    private fun applySpacing(view: View) {
        (view.layoutParams as ViewGroup.MarginLayoutParams).marginStart =
            resources.getDimensionPixelSize(R.dimen.new_post_photo_thumbnail_spacing)
    }

    private fun prefillFromCandidate() {
        val candidate = viewModel.selectedCandidate ?: return
        if (binding.etName.text.isNullOrBlank()) binding.etName.setText(candidate.nombre)
        if (binding.etDescription.text.isNullOrBlank()) {
            val composed = listOfNotNull(candidate.marca, candidate.modelo, candidate.edicion, candidate.procedencia)
                .filter { it.isNotBlank() }
                .joinToString(" • ")
            binding.etDescription.setText(composed)
        }
    }

    private fun requestPhotoFromCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun showPhotoSourceMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, MENU_ITEM_CAMERA, 0, getString(R.string.new_post_add_photo_button))
        popup.menu.add(0, MENU_ITEM_GALLERY, 1, getString(R.string.new_post_pick_gallery_button))
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_ITEM_CAMERA -> requestPhotoFromCamera()
                MENU_ITEM_GALLERY ->
                    pickPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            true
        }
        popup.show()
    }

    private fun launchCamera() {
        val photoFile = File(requireContext().cacheDir, "new_post_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile,
        )
        pendingCameraUri = uri
        takePictureLauncher.launch(uri)
    }

    private fun handlePickedImage(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val imageBytes = withContext(Dispatchers.IO) { decodeAndCompress(uri) }
            if (imageBytes == null) {
                Snackbar.make(binding.root, R.string.new_post_photo_read_error, Snackbar.LENGTH_LONG).show()
                return@launch
            }
            val isFirstPhoto = viewModel.photos.isEmpty()
            if (!viewModel.addPhoto(imageBytes)) {
                Snackbar.make(binding.root, R.string.new_post_max_photos_reached, Snackbar.LENGTH_SHORT).show()
                return@launch
            }
            renderPhotos()
            if (isFirstPhoto) showAnalysisChoiceDialog(imageBytes)
        }
    }

    private fun showAnalysisChoiceDialog(imageBytes: ByteArray) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.new_post_analysis_dialog_title)
            .setMessage(R.string.new_post_analysis_dialog_message)
            .setPositiveButton(R.string.new_post_analysis_dialog_auto) { _, _ -> viewModel.recognize(imageBytes) }
            .setNegativeButton(R.string.new_post_analysis_dialog_manual, null)
            .show()
    }

    private fun decodeAndCompress(uri: Uri): ByteArray? = runCatching {
        val bitmap = requireContext().contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return@runCatching null
        val scale = MAX_PHOTO_DIMENSION_PX.toFloat() / maxOf(bitmap.width, bitmap.height)
        val scaledBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            bitmap
        }
        ByteArrayOutputStream().use { output ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, PHOTO_JPEG_QUALITY, output)
            output.toByteArray()
        }
    }.getOrNull()

    private fun setUpDropdown(button: View, valueLabel: TextView, optionsRes: Int, onSelected: (String) -> Unit) {
        val options = resources.getStringArray(optionsRes)
        button.setOnClickListener {
            val popup = PopupMenu(requireContext(), button)
            options.forEachIndexed { index, option -> popup.menu.add(0, index, index, option) }
            popup.setOnMenuItemClickListener { item ->
                val option = options[item.itemId]
                valueLabel.text = option
                valueLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.auth_input_text))
                onSelected(option)
                true
            }
            popup.show()
        }
    }

    private fun render(state: RecognitionUiState) {
        updateLoadingOverlay()

        when (state) {
            is RecognitionUiState.Success -> {
                viewModel.acknowledgeRecognitionResult()
                findNavController().navigate(R.id.newPostDisambiguationFragment)
            }
            is RecognitionUiState.Error -> {
                viewModel.acknowledgeRecognitionResult()
                Snackbar.make(binding.root, state.message.asString(requireContext()), Snackbar.LENGTH_LONG).show()
            }
            else -> Unit
        }
    }

    private fun onPublishClicked() {
        viewModel.saveItem(
            name = binding.etName.text?.toString().orEmpty(),
            description = binding.etDescription.text?.toString(),
            sport = selectedSport,
            condition = selectedCondition,
        )
    }

    private fun renderSave(state: SaveItemUiState) {
        updateLoadingOverlay()
        val isLoading = state is SaveItemUiState.Loading
        binding.btnPublish.isEnabled = !isLoading
        binding.btnPublish.text = getString(
            if (isLoading) R.string.new_post_publishing else R.string.new_post_publish_button,
        )

        when (state) {
            is SaveItemUiState.Success -> {
                Snackbar.make(binding.root, R.string.new_post_success, Snackbar.LENGTH_LONG).show()
                findNavController().navigate(
                    R.id.homeFragment,
                    null,
                    navOptions {
                        popUpTo(R.id.homeFragment) { inclusive = false }
                        launchSingleTop = true
                    },
                )
            }
            is SaveItemUiState.Error ->
                Snackbar.make(binding.root, state.message.asString(requireContext()), Snackbar.LENGTH_LONG).show()
            is SaveItemUiState.ValidationError -> {
                val messageRes = when (state.field) {
                    RequiredField.NAME -> R.string.new_post_name_required_error
                    RequiredField.SPORT -> R.string.new_post_sport_required_error
                    RequiredField.CONDITION -> R.string.new_post_condition_required_error
                }
                Snackbar.make(binding.root, messageRes, Snackbar.LENGTH_SHORT).show()
            }
            is SaveItemUiState.DuplicateWarning -> showDuplicateWarningDialog(state.existingItemName)
            else -> Unit
        }
    }

    private fun showDuplicateWarningDialog(existingItemName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.new_post_duplicate_title)
            .setMessage(getString(R.string.new_post_duplicate_message, existingItemName))
            .setPositiveButton(R.string.new_post_duplicate_confirm) { _, _ -> viewModel.confirmPublishDespiteDuplicate() }
            .setNegativeButton(R.string.new_post_duplicate_cancel) { _, _ -> viewModel.dismissDuplicateWarning() }
            .setOnCancelListener { viewModel.dismissDuplicateWarning() }
            .show()
    }

    private fun updateLoadingOverlay() {
        val analyzing = viewModel.recognitionState.value is RecognitionUiState.Loading
        val publishing = viewModel.saveState.value is SaveItemUiState.Loading
        binding.overlayLoading.visibility = if (analyzing || publishing) View.VISIBLE else View.GONE
        binding.tvLoadingMessage.text = getString(
            if (publishing) R.string.new_post_publishing_valuation else R.string.new_post_analyzing,
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val MAX_PHOTO_DIMENSION_PX = 1024
        const val PHOTO_JPEG_QUALITY = 85
        const val MENU_ITEM_CAMERA = 0
        const val MENU_ITEM_GALLERY = 1
    }
}
