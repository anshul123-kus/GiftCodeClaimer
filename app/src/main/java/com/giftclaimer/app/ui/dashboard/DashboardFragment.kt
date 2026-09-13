package com.giftclaimer.app.ui.dashboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.giftclaimer.app.R
import com.giftclaimer.app.databinding.FragmentDashboardBinding
import com.giftclaimer.app.service.ClipboardMonitorService
import com.giftclaimer.app.util.CodeDetector
import com.giftclaimer.app.util.Constants

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var logAdapter: LogAdapter

    private val serviceStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isRunning = intent?.getBooleanExtra("is_running", false) ?: false
            viewModel.setServiceRunning(isRunning)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeData()
        registerReceivers()
    }

    private fun setupUI() {
        logAdapter = LogAdapter()
        binding.recyclerLogs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = logAdapter
        }

        // START/STOP button
        binding.btnStartStop.setOnClickListener {
            val isRunning = viewModel.isServiceRunning.value ?: false
            if (isRunning) {
                stopService()
            } else {
                startService()
            }
        }

        // Manual paste & claim button
        binding.btnPasteClaim.setOnClickListener {
            val text = binding.editPasteCode.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(requireContext(), "Paste a code first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val codes = CodeDetector.findCodes(text)
            if (codes.isEmpty()) {
                Toast.makeText(requireContext(), "No valid 32-char hex code found!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            for (code in codes) {
                val intent = Intent(requireContext(), ClipboardMonitorService::class.java).apply {
                    action = Constants.ACTION_CODE_DETECTED
                    putExtra(Constants.EXTRA_CODE, code)
                }
                requireContext().startService(intent)
            }

            binding.editPasteCode.text?.clear()
            Toast.makeText(requireContext(), "Submitting ${codes.size} code(s)...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeData() {
        viewModel.isServiceRunning.observe(viewLifecycleOwner) { isRunning ->
            binding.btnStartStop.text = if (isRunning) "STOP" else "START"
            binding.btnStartStop.setBackgroundColor(
                resources.getColor(
                    if (isRunning) R.color.error_red else R.color.success_green,
                    null
                )
            )
            binding.tvStatus.text = if (isRunning) "Running" else "Stopped"
            binding.tvStatus.setTextColor(
                resources.getColor(
                    if (isRunning) R.color.success_green else R.color.error_red,
                    null
                )
            )
        }

        viewModel.activeSiteCount.observe(viewLifecycleOwner) {
            binding.tvSiteCount.text = "Sites: $it active"
        }

        viewModel.activeProfileCount.observe(viewLifecycleOwner) {
            binding.tvProfileCount.text = "Profiles: $it active"
        }

        viewModel.todaySuccessCount.observe(viewLifecycleOwner) { success ->
            val total = viewModel.todayTotalCount.value ?: 0
            binding.tvClaimedToday.text = "Claimed today: $success/$total"
        }

        viewModel.todayTotalCount.observe(viewLifecycleOwner) { total ->
            val success = viewModel.todaySuccessCount.value ?: 0
            binding.tvClaimedToday.text = "Claimed today: $success/$total"
        }

        viewModel.recentLogs.observe(viewLifecycleOwner) { logs ->
            logAdapter.submitList(logs)
            if (logs.isNotEmpty()) {
                binding.recyclerLogs.scrollToPosition(0)
            }
        }
    }

    private fun startService() {
        val intent = Intent(requireContext(), ClipboardMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
        viewModel.setServiceRunning(true)
    }

    private fun stopService() {
        val intent = Intent(requireContext(), ClipboardMonitorService::class.java).apply {
            action = Constants.ACTION_STOP_SERVICE
        }
        requireContext().startService(intent)
        viewModel.setServiceRunning(false)
    }

    private fun registerReceivers() {
        val filter = IntentFilter("com.giftclaimer.SERVICE_STATUS_CHANGED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(serviceStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            requireContext().registerReceiver(serviceStatusReceiver, filter)
        }
    }

    override fun onDestroyView() {
        try {
            requireContext().unregisterReceiver(serviceStatusReceiver)
        } catch (_: Exception) {}
        _binding = null
        super.onDestroyView()
    }
}
