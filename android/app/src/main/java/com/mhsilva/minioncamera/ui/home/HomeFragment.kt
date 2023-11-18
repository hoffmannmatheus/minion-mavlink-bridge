package com.mhsilva.minioncamera.ui.home

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        viewModel.connectionStatus.observe(viewLifecycleOwner) {
            handleStatusUpdate(it)
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
        _binding = null
    }

    private fun handleStatusUpdate(connectionStatus: ConnectionStatus) {
        Log.d(TAG, "handleStatusUpdate: ${connectionStatus.name}")
        statusTextView.text = getString(R.string.status, connectionStatus.name)
        when (connectionStatus) {
            ConnectionStatus.SETTING_UP -> {
                /* cool */
            }

            ConnectionStatus.STANDING_BY -> {
                messageTextView.text = getString(R.string.bluetooth_ready)
            }

            ConnectionStatus.BLUETOOTH_DISABLED -> {
                messageTextView.text = getString(R.string.bluetooth_error_disabled)
            }

            ConnectionStatus.BLUETOOTH_UNAVAILABLE -> {
                messageTextView.text = getString(R.string.bluetooth_error_unavailable)
            }

            ConnectionStatus.CONNECTING -> {
                messageTextView.text = getString(R.string.bluetooth_connecting)
            }

            ConnectionStatus.CONNECTED -> {
                messageTextView.text = getString(R.string.bluetooth_connected)
            }

            ConnectionStatus.ERROR -> {
                messageTextView.text = getString(R.string.bluetooth_error_unknown)
            }

            ConnectionStatus.NEED_PERMISSIONS -> {
                messageTextView.text = getString(R.string.bluetooth_error_permissions)
                requestForPermissions()
            }
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