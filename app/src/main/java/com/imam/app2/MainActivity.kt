package com.imam.app2

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.Surface
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.imam.app2.databinding.ActivityMainBinding
import java.io.File
import java.text.DecimalFormat
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentBitmap: Bitmap? = null
    private var currentPhotoFile: File? = null
    private var knownLengthPx: Double? = null
    private var knownLengthCm: Double? = null

    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var maxZoomRatio = 4f

    private val decimalFormat = DecimalFormat("#,##0.00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCommonObjectSpinner()
        setupButtons()
        setupZoomSlider()
        ensureCameraPermissionAndStart()
    }

    private fun setupCommonObjectSpinner() {
        val labels = CommonObjectCatalog.items.map { it.label }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCommonObject.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnCapture.setOnClickListener { capturePhoto() }
        binding.btnSetKnown.setOnClickListener { setKnownReference() }
        binding.btnEstimateUnknown.setOnClickListener { estimateUnknownSize() }
        binding.btnEstimateCommon.setOnClickListener { estimateCommonObjectSize() }
        binding.btnEstimateDistance.setOnClickListener { estimateDistance() }
        binding.btnReset.setOnClickListener {
            binding.measurementView.clearMeasurement()
            binding.tvResult.text = "Garis aktif direset"
        }
    }

    private fun setupZoomSlider() {
        binding.seekZoom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val ratio = 1f + ((maxZoomRatio - 1f) * progress / 100f)
                binding.tvZoomLabel.text = "Zoom kamera: ${decimalFormat.format(ratio)}x"
                if (fromUser) {
                    camera?.cameraControl?.setZoomRatio(ratio)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun ensureCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1001)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            toast("Izin kamera diperlukan untuk mode portrait dan zoom")
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetRotation(Surface.ROTATION_0)
                .build()
                .also {
                    it.surfaceProvider = binding.previewView.surfaceProvider
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(Surface.ROTATION_0)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                camera?.cameraInfo?.zoomState?.observe(this) { zoomState ->
                    maxZoomRatio = max(zoomState.maxZoomRatio, 1f)
                    val currentRatio = zoomState.zoomRatio
                    binding.tvZoomLabel.text = "Zoom kamera: ${decimalFormat.format(currentRatio)}x"
                }
            } catch (e: Exception) {
                toast("Gagal menyalakan kamera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun capturePhoto() {
        val imageCapture = imageCapture ?: run {
            toast("Kamera belum siap")
            return
        }

        val folder = File(cacheDir, "captured")
        folder.mkdirs()
        val photoFile = File.createTempFile("portrait_", ".jpg", folder)
        currentPhotoFile = photoFile

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    loadCapturedImage(photoFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    toast("Gagal mengambil foto: ${exception.message}")
                }
            }
        )
    }

    private fun loadCapturedImage(photoFile: File) {
        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath) ?: run {
            toast("Foto tidak dapat dibaca")
            return
        }
        val rotated = rotateBitmapIfRequired(bitmap, photoFile)
        currentBitmap = rotated
        binding.measurementView.setImageBitmap(rotated)
        binding.measurementView.clearMeasurement()
        binding.tvResult.text = "Foto portrait dimuat. Ketuk dua titik pada objek aktif."
    }

    private fun rotateBitmapIfRequired(bitmap: Bitmap, photoFile: File): Bitmap {
        val exif = ExifInterface(photoFile.absolutePath)
        val rotation = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotation == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
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

    private fun estimateCommonObjectSize() {
        val activePx = binding.measurementView.getActiveLengthPx()
        val bitmap = currentBitmap
        val profile = CommonObjectCatalog.items[binding.spinnerCommonObject.selectedItemPosition]
        binding.etActualSizeForDistanceCm.setText(decimalFormat.format(profile.typicalSizeCm))

        val distancePart = if (activePx != null && bitmap != null) {
            val focalLengthPx = getApproxFocalLengthPixels(bitmap.width)
            if (focalLengthPx != null && focalLengthPx > 0.0) {
                val distanceCm = focalLengthPx * profile.typicalSizeCm / activePx
                "\nEstimasi jarak dengan ukuran tipikal: ${decimalFormat.format(distanceCm)} cm"
            } else {
                ""
            }
        } else {
            ""
        }

        binding.tvResult.text = buildString {
            append("Objek umum: ${profile.label}\n")
            append("Ukuran tipikal kerja: ${decimalFormat.format(profile.typicalSizeCm)} cm\n")
            append("Rentang umum: ${decimalFormat.format(profile.minSizeCm)} - ${decimalFormat.format(profile.maxSizeCm)} cm\n")
            append("Catatan: ${profile.note}")
            append(distancePart)
        }
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

    private fun getApproxFocalLengthPixels(imageWidthPx: Int): Double? {
        val cameraManager = getSystemService(CameraManager::class.java) ?: return null
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val chars = cameraManager.getCameraCharacteristics(id)
            chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: return null
        val chars = cameraManager.getCameraCharacteristics(cameraId)
        val focalLengthMm = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull() ?: return null
        val sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: return null
        val sensorWidthMm = max(sensorSize.width, 0.0001f).toDouble()
        return focalLengthMm.toDouble() * imageWidthPx.toDouble() / sensorWidthMm
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
