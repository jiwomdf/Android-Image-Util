package com.programmergabut.easyimage.manage

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.programmergabut.easyimage.EasyImage.Companion.TAG
import com.programmergabut.easyimage.Extension
import com.programmergabut.easyimage.domain.ManageImage
import com.programmergabut.easyimage.setExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.lang.IllegalArgumentException

class ManageImageImpl(
    private val context: Context,
    private val fileName: String,
    directory: String?,
    private val fileExtension: Extension
): ManageImage {

    private val ABSOLUTE_PATH = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath

    private val FIX_DIRECTORY = if(directory.isNullOrEmpty()) "" else directory.trim()

    override fun load(): Bitmap? {
        return try {
            validateFileName(fileName)
            validateReadPermission()
            val extension = setExtension(fileExtension)
            val directory = File("${ABSOLUTE_PATH}/$FIX_DIRECTORY")
            validateDirectory(directory)

            val file = File(directory, "$fileName$extension")
            BitmapFactory.decodeFile(file.path)
        } catch (ex: Exception){
            Log.e(TAG, "load: ${ex.message}")
            null
        }
    }

    override fun load(callBack: IManageImage.LoadCallBack){
        CoroutineScope(Dispatchers.Default).launch {
            try {
                validateFileName(fileName)
                validateReadPermission()
                val extension = setExtension(fileExtension)
                val directory = File("${ABSOLUTE_PATH}/$FIX_DIRECTORY")
                validateDirectory(directory)

                val file = File(directory, "$fileName$extension")
                val result = BitmapFactory.decodeFile(file.path)
                withContext(Dispatchers.Main){
                    callBack.onResult(result)
                }
            } catch (ex: Exception){
                Log.e(TAG, "load: ${ex.message}")
                withContext(Dispatchers.Main){
                    callBack.onResult(null)
                }
            }
        }
    }

    override fun delete(): Boolean {
        return try {
            val extension = setExtension(fileExtension)
            val directory = File("${ABSOLUTE_PATH}/$FIX_DIRECTORY")
            validateDirectory(directory)

            val file = File(directory, "$fileName$extension")
            file.delete()
        } catch (ex: Exception){
            Log.e(TAG, "delete: ${ex.message}")
            true
        }
    }

    override fun delete(callBack: IManageImage.DeleteCallBack){
        CoroutineScope(Dispatchers.Default).launch {
            val extension = setExtension(fileExtension)
            val directory = File("${ABSOLUTE_PATH}/$FIX_DIRECTORY")
            validateDirectory(directory)

            try {
                val file = File(directory, "$fileName$extension")
                if(file.delete()){
                    withContext(Dispatchers.Main) { callBack.onSuccess() }
                } else {
                    throw Exception("file not deleted")
                }
            } catch (ex: Exception){
                Log.e(TAG, "delete: ${ex.message}")
                withContext(Dispatchers.Main) {
                    callBack.onFailed(ex)
                }
            }
        }
    }

    override fun save(bitmap: Bitmap, quality: Int): Boolean {
        return try {
            validateImageQuality(quality)
            validateStoragePermission()
            val extension = setExtension(fileExtension)
            val expectedDir = File("${ABSOLUTE_PATH}/$FIX_DIRECTORY")
            val directory = getOrCreateDirectoryIfEmpty(expectedDir)

            val file = File(directory, "$fileName$extension")
            compressBitmap(file, bitmap, quality)
            true
        } catch (ex: Exception){
            Log.e(TAG, "save: ${ex.message}")
            false
        }
    }

    override fun save(bitmap: Bitmap, quality: Int, callBack: IManageImage.SaveBitMapCallBack){
        CoroutineScope(Dispatchers.Default).launch {
            try {
                validateImageQuality(quality)
                validateStoragePermission()
                val extension = setExtension(fileExtension)
                val expectedDir = File("${ABSOLUTE_PATH}/$FIX_DIRECTORY")
                val directory = getOrCreateDirectoryIfEmpty(expectedDir)

                val file = File(directory, "$fileName$extension")
                compressBitmap(file, bitmap, quality)
                withContext(Dispatchers.Main){
                    callBack.onSuccess()
                }
            } catch (ex: Exception){
                Log.e(TAG, "save: ${ex.message}")
                withContext(Dispatchers.Main){
                    callBack.onFailed(ex)
                }
            }
        }

    }

    override fun save(base64: String, quality: Int): Boolean {
        return try {
            validateImageQuality(quality)
            validateBase64String(base64)
            validateStoragePermission()
            val bitmap = decodeByteArray(base64)
            val extension = setExtension(fileExtension)
            val expectedDir = File("${ABSOLUTE_PATH}/$FIX_DIRECTORY")
            val directory = getOrCreateDirectoryIfEmpty(expectedDir)

            val file = File(directory, "$fileName$extension")
            compressBitmap(file, bitmap, quality)
            true
        } catch (ex: Exception){
            Log.e(TAG, "save: ${ex.message}")
            false
        }
    }

    override fun save(base64: String, quality: Int, callBack: IManageImage.SaveBase64CallBack){
        CoroutineScope(Dispatchers.Default).launch{
            try {
                validateImageQuality(quality)
                validateBase64String(base64)
                validateStoragePermission()
                val bitmap = decodeByteArray(base64)
                val extension = setExtension(fileExtension)
                val expectedDir = File("${ABSOLUTE_PATH}/$FIX_DIRECTORY")
                val directory = getOrCreateDirectoryIfEmpty(expectedDir)

                val file = File(directory, "$fileName$extension")
                compressBitmap(file, bitmap, quality)
                withContext(Dispatchers.Main){
                    callBack.onSuccess()
                }
            } catch (ex: Exception){
                Log.e(TAG, "save: ${ex.message}")
                withContext(Dispatchers.Main){
                    callBack.onFailed(ex)
                }
            }
        }
    }

    override fun save(drawable: Drawable, quality: Int): Boolean {
        return try {
            validateImageQuality(quality)
            validateStoragePermission()
            val bitmap = drawableToBitmap(drawable)
            val extension = setExtension(fileExtension)
            val expectedDir = File("${ABSOLUTE_PATH}/$FIX_DIRECTORY")
            val directory = getOrCreateDirectoryIfEmpty(expectedDir)

            val file = File(directory, "$fileName$extension")
            compressBitmap(file, bitmap, quality)
            true
        } catch (ex: Exception){
            Log.e(TAG, "save: ${ex.message}")
            false
        }
    }

    override fun save(drawable: Drawable, quality: Int,
                      callBack: IManageImage.SaveDrawableCallBack){
        CoroutineScope(Dispatchers.Default).launch {
            try {
                validateImageQuality(quality)
                validateStoragePermission()
                val bitmap = drawableToBitmap(drawable)
                val extension = setExtension(fileExtension)
                val expectedDir = File("${ABSOLUTE_PATH}/$FIX_DIRECTORY")
                val directory = getOrCreateDirectoryIfEmpty(expectedDir)

                val file = File(directory, "$fileName$extension")
                compressBitmap(file, bitmap, quality)
                withContext(Dispatchers.Main){
                    callBack.onSuccess()
                }
            } catch (ex: Exception){
                Log.e(TAG, "save: ${ex.message}")
                withContext(Dispatchers.Main){
                    callBack.onFailed(ex)
                }
            }
        }
    }

    private fun compressBitmap(file: File, bitmap: Bitmap, quality: Int) {
        val outStream = FileOutputStream(file)

        when (fileExtension) {
            Extension.PNG -> {
                bitmap.compress(Bitmap.CompressFormat.PNG, quality, outStream)
            }
            Extension.JPEG -> {
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outStream)
            }
            Extension.WEBP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, quality, outStream)
                } else {
                    bitmap.compress(Bitmap.CompressFormat.WEBP, quality, outStream)
                }
            }
            Extension.JPG -> {
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outStream)
            }
        }

        outStream.flush()
        outStream.close()
    }

    private fun decodeByteArray(base64: String): Bitmap {
        val decodedString: ByteArray = Base64.decode(base64, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            ?: throw NullPointerException("Failed decode base64 string to bitmap")
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable)
            return drawable.bitmap

        try {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            return bitmap
        } catch (ex: Exception) {
            throw Exception(ex.message.toString())
        }
    }

    private fun getOrCreateDirectoryIfEmpty(directory: File): File {
        if (!directory.exists())
            directory.mkdirs()
        return directory
    }

    private fun validateDirectory(directory: File){
        if (!directory.exists()){
            throw Exception("directory is not exists")
        }
    }

    private fun validateBase64String(base64: String) {
        if (base64.isEmpty()) {
            throw IllegalArgumentException("Base64 encoded cannot be empty")
        }
    }

    private fun validateImageQuality(quality: Int) {
        if (quality < 1 || quality > 100) {
            throw IllegalArgumentException("Quality must be between 1 to 100")
        }
    }

    private fun validateFileName(fileName: String): String {
        val fixFileName = fileName.trim()
        if (fixFileName.isEmpty())
            throw IllegalArgumentException("File name cannot be empty")

        return fixFileName
    }

    private fun validateStoragePermission() {
        if (!writePermissionGranted()) {
            throw SecurityException("Write external storage permission is not granted")
        }
    }

    private fun validateReadPermission() {
        if (!readPermissionGranted())
            throw SecurityException("Read external storage permission is not granted")
    }

    private fun writePermissionGranted(): Boolean {
        if (ContextCompat.checkSelfPermission(
                context.applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PermissionChecker.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun readPermissionGranted(): Boolean {
        if (ContextCompat.checkSelfPermission(
                context.applicationContext,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) ==
            PermissionChecker.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }
}

