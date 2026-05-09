package com.example.suryashakti.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.suryashakti.LoginActivity
import com.example.suryashakti.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("surya_prefs", Context.MODE_PRIVATE)
        binding.etGridRate.setText(prefs.getFloat("grid_rate", 8.0f).toString())
        binding.etExportRate.setText(prefs.getFloat("export_rate", 4.0f).toString())
        binding.etPanelCapacity.setText(prefs.getFloat("panel_capacity", 3.0f).toString())
        binding.switchNotifications.isChecked = prefs.getBoolean("notifications", true)

        binding.btnSaveSettings.setOnClickListener {
            val gridRate = binding.etGridRate.text.toString().toFloatOrNull() ?: 8f
            val exportRate = binding.etExportRate.text.toString().toFloatOrNull() ?: 4f
            val panelCap = binding.etPanelCapacity.text.toString().toFloatOrNull() ?: 3f

            prefs.edit()
                .putFloat("grid_rate", gridRate)
                .putFloat("export_rate", exportRate)
                .putFloat("panel_capacity", panelCap)
                .putBoolean("notifications", binding.switchNotifications.isChecked)
                .apply()

            Toast.makeText(requireContext(), "✅ Settings saved!", Toast.LENGTH_SHORT).show()
        }
        // Show user info
        val user = auth.currentUser
        if (user != null) {
            binding.tvUserName.text = user.displayName ?: "Surya User"
            binding.tvUserEmail.text = user.email ?: ""
            binding.tvAvatarLetter.text = (user.displayName?.firstOrNull() ?: "S").toString().uppercase()
        } else {
            binding.tvUserName.text = "Guest User"
            binding.tvUserEmail.text = "Sign in to sync data"
        }

// Sign out
        binding.btnSignOut.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out") { _, _ ->
                    // Sign out from both Firebase AND Google
                    auth.signOut()
                    com.google.android.gms.auth.api.signin.GoogleSignIn
                        .getClient(
                            requireContext(),
                            com.google.android.gms.auth.api.signin.GoogleSignInOptions
                                .Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .build()
                        )
                        .signOut()
                        .addOnCompleteListener {
                            startActivity(Intent(requireContext(), LoginActivity::class.java))
                            requireActivity().finish()
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}