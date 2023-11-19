package com.mhsilva.minioncamera.ui.home

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.mhsilva.minioncamera.MainActivity
import com.mhsilva.minioncamera.R
import com.mhsilva.minioncamera.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    // These properties are only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel

    private lateinit var statusTextView: TextView
    private lateinit var messageTextView: TextView
    private lateinit var statusView: View
    private lateinit var connectButton: Button

    companion object {
        const val REQUEST_ENABLE_BT = 42

        private const val TAG = "HomeFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        statusTextView = binding.labelStatus
        messageTextView = binding.labelMessage
        statusView = binding.statusIndicator
        connectButton = binding.connectButton

        viewModel.connectionStatus.observe(viewLifecycleOwner) {
            handleStatusUpdate(it)
        }
        viewModel.mavlinkMode.observe(viewLifecycleOwner) {
            messageTextView.text = it
        }
        connectButton.setOnClickListener {
            when (viewModel.connectionStatus.value) {
                ConnectionStatus.STANDING_BY, ConnectionStatus.ERROR -> viewModel.connect(requireContext())
                else -> viewModel.disconnect()
            }
        }
        return root
    }

    override fun onStart() {
        super.onStart()
        viewModel.setupBluetooth(requireContext())
        viewModel.connect(requireContext()) // TODO connect on button press
    }


    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.disconnect()
        _binding = null
    }

    private val stateToMessageMap = mapOf(
        ConnectionStatus.STANDING_BY to R.string.bluetooth_ready,
        ConnectionStatus.BLUETOOTH_DISABLED to R.string.bluetooth_error_disabled,
        ConnectionStatus.BLUETOOTH_UNAVAILABLE to R.string.bluetooth_error_unavailable,
        ConnectionStatus.CONNECTING to R.string.bluetooth_connecting,
        ConnectionStatus.ERROR to R.string.bluetooth_error_unknown,
        ConnectionStatus.NEED_PERMISSIONS to R.string.bluetooth_error_permissions
    )

    private fun handleStatusUpdate(connectionStatus: ConnectionStatus) {
        Log.d(TAG, "handleStatusUpdate: ${connectionStatus.name}")
        statusTextView.text = getString(R.string.status, connectionStatus.name)
        stateToMessageMap.getOrDefault(connectionStatus, null).let { resId ->
            messageTextView.text = if(resId != null) getString(resId) else ""
        }

        val state = when (connectionStatus) {
            ConnectionStatus.CONNECTED -> android.R.attr.state_first
            ConnectionStatus.ERROR -> android.R.attr.state_last
            else -> android.R.attr.state_middle
        }
        statusView.background.state = intArrayOf(state)

        val buttonText = when (connectionStatus) {
            ConnectionStatus.ERROR, ConnectionStatus.STANDING_BY -> R.string.button_connect
            else -> R.string.button_disconnect
        }
        connectButton.text = getString(buttonText)

        if (connectionStatus == ConnectionStatus.NEED_PERMISSIONS) {
            requestForPermissions()
        }
    }

    private fun requestForPermissions() {
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
}