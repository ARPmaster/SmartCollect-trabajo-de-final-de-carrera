// Pantalla "Editar Perfil": cambia el nombre mostrado y la foto de perfil del usuario, con
// bloqueo de navegación mientras se sube una foto y aviso de nombre duplicado. La foto elegida
// se guarda en memoria (pendingPhotoBytes) y solo se sube al pulsar "Guardar cambios", junto con
// el nombre: todo el guardado ocurre a través del botón, sin autoguardado parcial.
package com.example.aicollect.presentation.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.aicollect.R
import com.example.aicollect.databinding.FragmentEditProfileBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditProfileViewModel by viewModels()

    private var pendingPhotoBytes: ByteArray? = null

    private val blockNavigationWhileUploadingCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            Snackbar.make(binding.root, R.string.edit_profile_photo_uploading_wait, Snackbar.LENGTH_SHORT).show()
        }
    }

    private val pickPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        binding.ivAvatar.load(uri)
        viewLifecycleOwner.lifecycleScope.launch {
            val imageBytes = withContext(Dispatchers.IO) { decodeAndCompress(uri) }
            if (imageBytes != null) {
                pendingPhotoBytes = imageBytes
            } else {
                Snackbar.make(binding.root, R.string.edit_profile_photo_read_error, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun decodeAndCompress(uri: Uri): ByteArray? = runCatching {
        val resolver = requireContext().contentResolver
        val bitmap = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return@runCatching null
        val rotationDegrees = resolver.openInputStream(uri)?.use { stream ->
            when (
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
        val uprightBitmap = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
        val scale = MAX_PHOTO_DIMENSION_PX.toFloat() / maxOf(uprightBitmap.width, uprightBitmap.height)
        val scaledBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                uprightBitmap,
                (uprightBitmap.width * scale).toInt().coerceAtLeast(1),
                (uprightBitmap.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            uprightBitmap
        }
        ByteArrayOutputStream().use { output ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, PHOTO_JPEG_QUALITY, output)
            output.toByteArray()
        }
    }.getOrNull()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
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

        binding.etFullName.setText(viewModel.currentDisplayName())
        binding.ivAvatar.load(viewModel.currentPhotoUrl()) {
            placeholder(R.drawable.drawer_avatar)
            error(R.drawable.drawer_avatar)
            fallback(R.drawable.drawer_avatar)
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            blockNavigationWhileUploadingCallback,
        )

        binding.btnBack.setOnClickListener {
            if (blockNavigationWhileUploadingCallback.isEnabled) {
                Snackbar.make(binding.root, R.string.edit_profile_photo_uploading_wait, Snackbar.LENGTH_SHORT).show()
            } else {
                findNavController().popBackStack()
            }
        }
        binding.btnChangePhoto.setOnClickListener {
            pickPhotoLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
        binding.btnSaveChanges.setOnClickListener {
            viewModel.saveDisplayName(binding.etFullName.text?.toString().orEmpty())
            val photoBytes = pendingPhotoBytes
            if (photoBytes != null && viewModel.uiState.value !is EditProfileUiState.Error) {
                pendingPhotoBytes = null
                viewModel.uploadProfilePhoto(photoBytes)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { state -> render(state) } }
                launch { viewModel.photoUploadState.collect { state -> renderPhotoUpload(state) } }
            }
        }
    }

    private fun render(state: EditProfileUiState) {
        val isLoading = state is EditProfileUiState.Loading
        binding.btnSaveChanges.isEnabled = !isLoading
        binding.etFullName.isEnabled = !isLoading
        binding.btnSaveChanges.text = getString(
            if (isLoading) R.string.edit_profile_saving else R.string.edit_profile_save_button,
        )

        when (state) {
            is EditProfileUiState.Success ->
                Snackbar.make(binding.root, R.string.edit_profile_success, Snackbar.LENGTH_LONG).show()
            is EditProfileUiState.Error ->
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            is EditProfileUiState.NameTaken -> showNameTakenFeedback(state.message)
            else -> Unit
        }
    }

    private fun showNameTakenFeedback(message: String) {
        binding.boxFullName.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_auth_input_error)
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.vault_negative))
            .setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            .show()
        viewLifecycleOwner.lifecycleScope.launch {
            delay(NAME_TAKEN_BORDER_MS)
            binding.boxFullName.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_auth_input)
        }
    }

    private fun renderPhotoUpload(state: PhotoUploadUiState) {
        val isUploading = state is PhotoUploadUiState.Loading
        binding.btnChangePhoto.isEnabled = !isUploading
        binding.btnChangePhoto.alpha = if (isUploading) 0.5f else 1f
        blockNavigationWhileUploadingCallback.isEnabled = isUploading

        when (state) {
            is PhotoUploadUiState.Success ->
                Snackbar.make(binding.root, R.string.edit_profile_success, Snackbar.LENGTH_LONG).show()
            is PhotoUploadUiState.Error ->
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            else -> Unit
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val MAX_PHOTO_DIMENSION_PX = 512
        const val PHOTO_JPEG_QUALITY = 85
        const val NAME_TAKEN_BORDER_MS = 2000L
    }
}
