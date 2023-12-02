package com.mhsilva.minioncamera.ui.home

import android.Manifest
import android.content.ContentValues
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface.ROTATION_90
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.TextView
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.mhsilva.minioncamera.MainActivity
import com.mhsilva.minioncamera.R
import com.mhsilva.minioncamera.databinding.FragmentHomeBinding
import com.mhsilva.minioncamera.mavlink.MinionState
import com.mhsilva.minioncamera.utils.toast
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HomeFragment : Fragment() {

    companion object {
        private const val TAG = "HomeFragment"

        private const val DATE_FORMAT = "yyyy-MM-dd-HH-mm-ss"
        private const val PHOTO_TYPE = "image/jpeg"
        private const val FLASH_ANIMATION_DURATION = 100L
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var connectionStatusTextView: TextView
    private lateinit var minionPictureTextView: TextView
    private lateinit var minionArmedTextView: TextView
    private lateinit var minionMissionTextView: TextView
    private lateinit var minionModeTextView: TextView
    private lateinit var minionStateLayout: ViewGroup
    private lateinit var statusView: View
    private lateinit var connectButton: Button

    // Camera and preview
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraExecutor: ExecutorService? = null

    private val stateToMessageMap = mapOf(
        ConnectionStatus.STANDING_BY to R.string.bluetooth_ready,
        ConnectionStatus.BLUETOOTH_DISABLED to R.string.bluetooth_error_disabled,
        ConnectionStatus.BLUETOOTH_UNAVAILABLE to R.string.bluetooth_error_unavailable,
        ConnectionStatus.CONNECTING to R.string.bluetooth_connecting,
        ConnectionStatus.CONNECTED to R.string.bluetooth_connected,
        ConnectionStatus.ERROR to R.string.bluetooth_error_unknown,
        ConnectionStatus.NEED_PERMISSIONS to R.string.bluetooth_error_permissions
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        connectionStatusTextView = binding.labelConnectionStatus
        statusView = binding.statusIndicator
        connectButton = binding.connectButton
        minionPictureTextView = binding.labelMinionPictures
        minionArmedTextView = binding.labelMinionArmed
        minionMissionTextView = binding.labelMinionMission
        minionModeTextView = binding.labelMinionMode
        minionStateLayout = binding.minionStateLayout

        binding.videoContainer.apply { // setup video round border
            outlineProvider  = ViewOutlineProvider.BACKGROUND
            binding.videoContainer.clipToOutline = true
        }

        cameraExecutor = Executors.newSingleThreadExecutor()


        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        viewModel.connectionStatus.observe(viewLifecycleOwner) {
            handleStatusUpdate(it)
        }
        viewModel.minionState.observe(viewLifecycleOwner) {
            it?.first?.let { state ->
                handleMinionStateUpdate(minionState=state)
                if (it.second) {  // Should take picture?
                    takePicture(state.pictureSequence)
                }
            }
        }
        connectButton.setOnClickListener {
            when (viewModel.connectionStatus.value) {
                ConnectionStatus.STANDING_BY, ConnectionStatus.ERROR -> viewModel.connect(requireContext())
                else -> viewModel.disconnect()
            }
        }
        binding.takePictureButton.setOnClickListener{
            takePicture(-1)
        }
        binding.root.post {
            lifecycleScope.launch {
                cameraProvider = ProcessCameraProvider.getInstance(requireContext()).await()
                bindCameraUseCases()
            }
        }
        return binding.root
    }

    private fun handleMinionStateUpdate(minionState: MinionState) {
        minionStateLayout.visibility = View.VISIBLE
        minionPictureTextView.text = minionState.pictureSequence.toString()
        minionArmedTextView.text = getString(if (minionState.isArmed) R.string.armed else R.string.disarmed)
        minionMissionTextView.text = minionState.missionSequence.toString()
        minionModeTextView.text = minionState.flightMode.toString()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setupBluetooth(requireContext())
    }

    override fun onResume() {
        super.onResume()
        viewModel.connectionStatus.value?.let { handleStatusUpdate(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.disconnect()
        cameraExecutor?.shutdown()
        _binding = null
    }

    private fun handleStatusUpdate(connectionStatus: ConnectionStatus) {
        Log.d(TAG, "handleStatusUpdate: ${connectionStatus.name}")
        stateToMessageMap.getOrDefault(connectionStatus, null).let { resId ->
            connectionStatusTextView.text = if(resId != null) getString(resId) else ""
        }

        val state = when (connectionStatus) {
            ConnectionStatus.CONNECTED -> android.R.attr.state_first  // green
            ConnectionStatus.ERROR -> android.R.attr.state_last  // red
            else -> android.R.attr.state_middle  // blue
        }
        statusView.background.state = intArrayOf(state)

        val buttonText = when (connectionStatus) {
            ConnectionStatus.ERROR, ConnectionStatus.STANDING_BY -> R.string.button_connect
            ConnectionStatus.CONNECTING -> R.string.button_cancel
            else -> R.string.button_disconnect
        }
        connectButton.text = getString(buttonText)

        if (connectionStatus == ConnectionStatus.NEED_PERMISSIONS) {
            requestForPermissions()
        }
    }

    private fun requestForPermissions() {
        // TODO: ask for permission when needed: android.permission.CAMERA
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MainActivity.PERMISSION_REQUEST_CODE
            )

        } else {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT
                ), MainActivity.PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        val selector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY).build()

        preview = Preview.Builder()
            .setResolutionSelector(selector)
            .setTargetRotation(ROTATION_90)
            .build()
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(selector)
            .setTargetRotation(ROTATION_90)
            .build()

        // Must unbind the use-cases before binding new ones
        cameraProvider.unbindAll()

        if (camera != null) {
            // Must remove observers from the previous camera instance
            removeCameraStateObservers(camera!!.cameraInfo)
        }

        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            camera?.cameraInfo?.cameraState?.observe(viewLifecycleOwner) { cameraState ->
                when (cameraState.type) {
                    CameraState.Type.OPEN -> {
                        requireContext().toast("camera open!")
                    }
                    CameraState.Type.CLOSED -> {
                        requireContext().toast("camera closed")
                    }
                    else -> {/* do nothing */}
                }
            }
        } catch (exc: Exception) {
            requireContext().toast("Failed to start camera")
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun takePicture(pictureSequence: Int) {
        val imageCapture = imageCapture
        val executor = cameraExecutor
        if (imageCapture == null) {
            Log.e(TAG, "(takePicture) failed since imageCapture is null")
            return
        }
        if (executor == null) {
            Log.e(TAG, "(takePicture) failed since executor is null")
            return
        }

        // Create time stamped name and MediaStore entry.
        val formattedDate = SimpleDateFormat(DATE_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val name = "minion-$formattedDate-seq-$pictureSequence"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, PHOTO_TYPE)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                val appName = requireContext().resources.getString(R.string.app_name)
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${appName}")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                requireContext().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        // Setup image capture listener which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    requireContext().toast("Failed to take picture")
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri
                    Log.d(TAG, "Photo capture succeeded: $savedUri")
                    lifecycleScope.launch {
                        requireContext().toast("Picture captured!")
                    }
                }
            })
        animateScreenFlash()
    }

    private fun removeCameraStateObservers(cameraInfo: CameraInfo) {
        cameraInfo.cameraState.removeObservers(viewLifecycleOwner)
    }
    private fun animateScreenFlash() {
        binding.root.postDelayed({
            binding.root.foreground = ColorDrawable(Color.WHITE)
            binding.root.postDelayed(
                { binding.root.foreground = null }, FLASH_ANIMATION_DURATION)
        }, FLASH_ANIMATION_DURATION)
    }
}