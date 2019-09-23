package com.example.imagegallerysaver

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.media.MediaScannerConnection

class ImageGallerySaverPlugin(private val registrar: Registrar) : MethodCallHandler {

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "image_gallery_saver")
            channel.setMethodCallHandler(ImageGallerySaverPlugin(registrar))
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result): Unit {
        when {
            call.method == "saveImageToGallery" -> {
                val image = call.arguments as ByteArray
                result.success(saveImageToGallery(BitmapFactory.decodeByteArray(image, 0, image.size)))
            }
            call.method == "saveFileToGallery" -> {
                val path = call.arguments as String
                result.success(saveFileToGallery(path))
            }
            call.method == "getPicsPath" -> {
                result.success(getApplicationPicDirectionary())
            }
            call.method == "notifyScan" -> {
                val path = call.arguments as String
                notifyScan(path,result);
//                result.success(notifyScan(path,name))
            }
            else -> result.notImplemented()
        }

    }

    private fun generateFile(extension: String = ""): File {
        val storePath = Environment.getExternalStorageDirectory().absolutePath + File.separator + getApplicationName()
        val appDir = File(storePath)
        if (!appDir.exists()) {
            appDir.mkdir()
        }
        var fileName = System.currentTimeMillis().toString()
        if (extension.isNotEmpty()) {
            fileName += ("." + extension)
        }
        return File(appDir, fileName)
    }

    private fun saveImageToGallery(bmp: Bitmap): String {
        val context = registrar.activeContext().applicationContext
        val file = generateFile("png")
        try {
            val fos = FileOutputStream(file)
            bmp.compress(Bitmap.CompressFormat.PNG, 60, fos)
            fos.flush()
            fos.close()
            val uri = Uri.fromFile(file)
            context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
            return uri.toString()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ""
    }

    private fun saveFileToGallery(filePath: String): String {
        val context = registrar.activeContext().applicationContext
        return try {
            val originalFile = File(filePath)
            val file = generateFile(originalFile.extension)
            originalFile.copyTo(file)

            val uri = Uri.fromFile(file)
            context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
            return uri.toString()
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }
    }

    public fun getApplicationPicDirectionary(): String {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath;
    }

    public fun notifyScan( filePath:String,result: Result) {
        /* //直接插入
         MediaStore.Images.Media.insertImage(
                 contentResolver,
                 filePath,
                 name,
                 "file");

         val context = registrar.activeContext().applicationContext
         //通过广播去刷新
         val file = File(filePath)
         val uri = Uri.fromFile(file)
         context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))*/

        val context = registrar.activeContext().applicationContext
        //使用MediaScannerConnection的方式
        var suf="";
        if (filePath.endsWith("gif")) {
            suf="image/gif";
        }else{
            suf="image/jpeg"
        }
        MediaScannerConnection.scanFile(context
                , arrayOf(filePath)
                , arrayOf(suf), { path, uri ->
            //刷新成功的回调
            registrar.activity().runOnUiThread(object :Runnable{
                override fun run() {
                    result.success(path);
                }
            });
        })

    }

    private fun getApplicationName(): String {
        val context = registrar.activeContext().applicationContext
        var ai: ApplicationInfo? = null
        try {
            ai = context.packageManager.getApplicationInfo(context.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
        }
        var appName: String
        appName = if (ai != null) {
            val charSequence = context.packageManager.getApplicationLabel(ai)
            StringBuilder(charSequence.length).append(charSequence).toString()
        } else {
            "image_gallery_saver"
        }
        return appName
    }


}
