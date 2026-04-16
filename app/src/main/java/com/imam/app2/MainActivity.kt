package com.imam.app2

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.imam.app2.databinding.ActivityMainBinding
import java.io.File
import java.text.DecimalFormat
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentPhotoUri: Uri? = null
    private var currentBitmap: Bitmap? = null
    private var knownLengthPx: Double? = null
    private var knownLengthCm: Double? = null
    private val decimalFormat = DecimalFormat("#,##0.00")

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera() else toast("Izin kamera diperlukan")
    }

    private val capturePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) loadCapturedImage() else toast("Pengambilan foto dibatalkan")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCapture.setOnClickListener { ensureCameraAndCapture() }
        binding.btnSetKnown.setOnClickListener { setKnownReference() }
        binding.btnEstimateUnknown.setOnClickListener { estimateUnknownSize() }
        binding.btnEstimateDistance.setOnClickListener { estimateDistance() }
        binding.btnReset.setOnClickListener {
            binding.measurementView.clearMeasurement()
            binding.tvResult.text = "Garis aktif direset"
        }
    }

    private fun setKnownReference() {
        val activePx = binding.measurementView.getActiveLengthPx()
        val knownCm = binding.etKnownSizeCm.text?.toString()?.replace(",", ".")?.toDoubleOrNull()
        if (activePx == null || knownCm == null || knownCm <= 0.0) {
            toast("Buat garis aktif dan isi ukuran referensi nyata")
            return
        }
        knownLengthPx = activePx
        knownLengthCm = knownCm
        binding.tvResult.text = "Referensi tersimpan: ${decimalFormat.format(knownCm)} cm"
    }

    private fun estimateUnknownSize() {
        val activePx = binding.measurementView.getActiveLengthPx()
        val refPx = knownLengthPx
        val refCm = knownLengthCm
        if (activePx == null || refPx == null || refCm == null) {
            toast("Tetapkan referensi dulu")
            return
        }
        val estimatedCm = activePx / refPx * refCm
        binding.etActualSizeForDistanceCm.setText(decimalFormat.format(estimatedCm))
        binding.tvResult.text = "Estimasi ukuran objek: ${decimalFormat.format(estimatedCm)} cm"
    }

    private fun estimateDistance() {
        val bitmap = currentBitmap
        val activePx = binding.measurementView.getActiveLengthPx()
        val actualCm = binding.etActualSizeForDistanceCm.text?.toString()?.replace(",", ".")?.toDoubleOrNull()
        if (bitmap == null || activePx == null || actualCm == null || actualCm <= 0.0) {
            toast("Foto, garis aktif, dan ukuran nyata wajib ada")
            return
        }
        val focalLengthPx = getApproxFocalLengthPixels(bitmap.width)
        if (focalLengthPx == null || focalLengthPx <= 0.0) {
            toast("Data kamera tidak tersedia")
            return
        }
        val distanceCm = focalLengthPx * actualCm / activePx
        binding.tvResult.text = "Estimasi jarak: ${decimalFormat.format(distanceCm)} cm"
    }

    private fun ensureCameraAndCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val folder = File(cacheDir, "images")
        folder.mkdirs()
        val imageFile = File.createTempFile("measure_", ".jpg", folder)
        currentPhotoUri = FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", imageFile)
        capturePhoto.launch(currentPhotoUri)
    }

    private fun loadCapturedImage() {
        val uri = currentPhotoUri ?: return
        contentResolver.openInputStream(uri)?.use { input ->
            val bitmap = BitmapFactory.decodeStream(input)
            currentBitmap = bitmap
            binding.measurementView.setImageBitmap(bitmap)
            binding.measurementView.clearMeasurement()
            binding.tvResult.text = "Foto dimuat. Ketuk dua titik untuk membuat garis aktif."
        }
    }

    private fun getApproxFocalLengthPixels(imageWidthPx: Int): Double? {
        val cameraManager = getSystemService(CameraManager::class.java) ?: return null
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val chars = cameraManager.getCameraCharacteristics(id)
            chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: return null
        val chars = cameraManager.getCameraCharacteristics(cameraId)
        val focalLengthMm = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull() ?: return null
        val sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: return null
        val sensorWidthMm = max(sensorSize.width, 0.0001f)
        return focalLengthMm * imageWidthPx / sensorWidthMm
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
