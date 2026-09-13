package com.giftclaimer.app.ui.profiles

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.giftclaimer.app.databinding.FragmentProfilesBinding

class ProfilesFragment : Fragment() {

    private var _binding: FragmentProfilesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfilesViewModel by viewModels()
    private lateinit var profileAdapter: ProfileAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileAdapter = ProfileAdapter(
            onToggle = { viewModel.toggleProfile(it) },
            onLogin = { openLoginWebView(it.id, it.name) },
            onDelete = { viewModel.deleteProfile(it) }
        )

        binding.recyclerProfiles.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = profileAdapter
        }

        binding.fabAddProfile.setOnClickListener { showAddProfileDialog() }

        viewModel.allProfiles.observe(viewLifecycleOwner) { profiles ->
            profileAdapter.submitList(profiles)
            binding.tvEmptyProfiles.visibility = if (profiles.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showAddProfileDialog() {
        val context = requireContext()
        val padding = (16 * resources.displayMetrics.density).toInt()
        val editName = EditText(context).apply {
            hint = "Profile Name (e.g. Account 1)"
            setPadding(padding, padding, padding, padding)
        }

        AlertDialog.Builder(context)
            .setTitle("Add Profile")
            .setView(editName)
            .setPositiveButton("Create & Login") { _, _ ->
                val name = editName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(context, "Name required!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val id = viewModel.addProfile(name)
                // Open login WebView after a short delay to allow DB insert
                binding.root.postDelayed({
                    openLoginWebView(id, name)
                }, 500)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openLoginWebView(profileId: Long, profileName: String) {
        val intent = Intent(requireContext(), ProfileLoginActivity::class.java).apply {
            putExtra("profile_id", profileId)
            putExtra("profile_name", profileName)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
