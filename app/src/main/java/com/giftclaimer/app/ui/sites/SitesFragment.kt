package com.giftclaimer.app.ui.sites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.giftclaimer.app.data.models.Site
import com.giftclaimer.app.databinding.FragmentSitesBinding

class SitesFragment : Fragment() {

    private var _binding: FragmentSitesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SitesViewModel by viewModels()
    private lateinit var siteAdapter: SiteAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSitesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        siteAdapter = SiteAdapter(
            onToggle = { viewModel.toggleSite(it) },
            onEdit = { showSiteDialog(it) },
            onDelete = { viewModel.deleteSite(it) }
        )

        binding.recyclerSites.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = siteAdapter
        }

        binding.fabAddSite.setOnClickListener { showSiteDialog(null) }

        viewModel.allSites.observe(viewLifecycleOwner) { sites ->
            siteAdapter.submitList(sites)
            binding.tvEmptySites.visibility = if (sites.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showSiteDialog(existingSite: Site?) {
        val context = requireContext()
        val padding = (16 * resources.displayMetrics.density).toInt()

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        val editName = EditText(context).apply {
            hint = "Site Name (e.g. Jai Club)"
            setText(existingSite?.name ?: "")
        }
        val editUrl = EditText(context).apply {
            hint = "Site URL"
            setText(existingSite?.url ?: "")
        }
        val editInput = EditText(context).apply {
            hint = "Input CSS Selector"
            setText(existingSite?.inputSelector ?: "input[placeholder='Please enter gift code']")
        }
        val editButton = EditText(context).apply {
            hint = "Button Text (e.g. Receive)"
            setText(existingSite?.buttonSelector ?: "Receive")
        }
        val editPopup = EditText(context).apply {
            hint = "Popup Dismiss Selector"
            setText(existingSite?.popupSelector ?: ".van-dialog__confirm")
        }
        val editSuccess = EditText(context).apply {
            hint = "Success Text"
            setText(existingSite?.successText ?: "Successfully received")
        }

        layout.addView(editName)
        layout.addView(editUrl)
        layout.addView(editInput)
        layout.addView(editButton)
        layout.addView(editPopup)
        layout.addView(editSuccess)

        val scrollView = ScrollView(context).apply { addView(layout) }

        AlertDialog.Builder(context)
            .setTitle(if (existingSite != null) "Edit Site" else "Add Site")
            .setView(scrollView)
            .setPositiveButton("Save") { _, _ ->
                val name = editName.text.toString().trim()
                val url = editUrl.text.toString().trim()

                if (name.isEmpty() || url.isEmpty()) {
                    Toast.makeText(context, "Name and URL required!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val site = Site(
                    id = existingSite?.id ?: 0,
                    name = name,
                    url = url,
                    inputSelector = editInput.text.toString().trim(),
                    buttonSelector = editButton.text.toString().trim(),
                    popupSelector = editPopup.text.toString().trim(),
                    successText = editSuccess.text.toString().trim()
                )

                if (existingSite != null) {
                    viewModel.updateSite(site)
                } else {
                    viewModel.addSite(site)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
