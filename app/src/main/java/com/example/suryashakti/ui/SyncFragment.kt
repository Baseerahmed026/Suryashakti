package com.example.suryashakti.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.suryashakti.data.EnergyLog
import com.example.suryashakti.databinding.FragmentSyncBinding
import com.example.suryashakti.viewmodel.EnergyViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class SyncFragment : Fragment() {

    private var _binding: FragmentSyncBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EnergyViewModel by activityViewModels()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSyncBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = auth.currentUser

        // Show user info
        if (user != null) {
            binding.tvLocalRecords.text = "Signed in as ${user.email}"
        } else {
            binding.tvLocalRecords.text = "Guest mode — sign in to sync"
            binding.btnSync.isEnabled = false
            binding.btnSync.text = "Sign in to Sync"
        }

        // Show local record count
        viewModel.totalDays.observe(viewLifecycleOwner) { days ->
            if (user != null) {
                binding.tvLocalRecords.text = "$days local records • ${user.email}"
            }
        }

        binding.btnSync.setOnClickListener {
            if (user == null) {
                Toast.makeText(requireContext(), "Please sign in to sync", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startSync(user.uid)
        }
    }

    private fun startSync(userId: String) {
        binding.btnSync.isEnabled = false
        binding.btnSync.text = "⏳ Syncing..."
        Toast.makeText(requireContext(), "☁️ Sync started...", Toast.LENGTH_SHORT).show()

        // Step 1: Upload all local logs to Firestore
        viewModel.allLogs.value?.let { logs ->
            uploadLogs(userId, logs)
        } ?: run {
            // Observe once if not yet loaded
            viewModel.allLogs.observe(viewLifecycleOwner) { logs ->
                if (logs != null) {
                    uploadLogs(userId, logs)
                }
            }
        }
    }

    private fun uploadLogs(userId: String, logs: List<EnergyLog>) {
        if (logs.isEmpty()) {
            downloadLogs(userId)
            return
        }

        val userRef = db.collection("users").document(userId).collection("energy_logs")
        var uploadCount = 0

        logs.forEach { log ->
            val logData = hashMapOf(
                "userId" to userId,
                "date" to log.date,
                "generatedKwh" to log.generatedKwh,
                "consumedKwh" to log.consumedKwh,
                "weatherCondition" to log.weatherCondition,
                "pricePerUnit" to log.pricePerUnit,
                "exportRate" to log.exportRate,
                "panelCapacityKw" to log.panelCapacityKw,
                "co2SavedKg" to log.co2SavedKg
            )

            userRef.document(log.date)
                .set(logData, SetOptions.merge())
                .addOnSuccessListener {
                    uploadCount++
                    if (uploadCount == logs.size) {
                        // All uploaded → now download
                        downloadLogs(userId)
                    }
                }
                .addOnFailureListener { e ->
                    resetSyncButton()
                    Toast.makeText(requireContext(),
                        "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun downloadLogs(userId: String) {
        val userRef = db.collection("users").document(userId).collection("energy_logs")

        userRef.get()
            .addOnSuccessListener { documents ->
                var downloadCount = 0
                if (documents.isEmpty) {
                    syncComplete(0, 0)
                    return@addOnSuccessListener
                }

                documents.forEach { doc ->
                    try {
                        val log = EnergyLog(
                            userId = userId,
                            date = doc.getString("date") ?: "",
                            generatedKwh = (doc.getDouble("generatedKwh") ?: 0.0).toFloat(),
                            consumedKwh = (doc.getDouble("consumedKwh") ?: 0.0).toFloat(),
                            weatherCondition = doc.getString("weatherCondition") ?: "Sunny",
                            pricePerUnit = (doc.getDouble("pricePerUnit") ?: 8.0).toFloat(),
                            exportRate = (doc.getDouble("exportRate") ?: 4.0).toFloat(),
                            panelCapacityKw = (doc.getDouble("panelCapacityKw") ?: 3.0).toFloat(),
                            co2SavedKg = (doc.getDouble("co2SavedKg") ?: 0.0).toFloat()
                        )
                        viewModel.insertLog(log)
                        downloadCount++
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                syncComplete(viewModel.allLogs.value?.size ?: 0, downloadCount)
            }
            .addOnFailureListener { e ->
                resetSyncButton()
                Toast.makeText(requireContext(),
                    "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun syncComplete(uploaded: Int, downloaded: Int) {
        resetSyncButton()
        Toast.makeText(requireContext(),
            "✅ Sync complete! ↑$uploaded uploaded ↓$downloaded downloaded",
            Toast.LENGTH_LONG).show()
        binding.tvLocalRecords.text = "Last synced just now ✅"
    }

    private fun resetSyncButton() {
        binding.btnSync.isEnabled = true
        binding.btnSync.text = "☁️  SYNC NOW"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}