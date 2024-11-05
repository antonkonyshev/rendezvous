package com.github.antonkonyshev.tryst.presentation.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import java.io.File
import kotlin.math.min

fun changeAvatar(context: Context, uid: String, uri: Uri?): Boolean {
    if (uri != null) {
        try {
            val srcBitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
            val targetWidth = min(srcBitmap.width, srcBitmap.height)
            val resultBitmap = Bitmap.createBitmap(
                targetWidth, targetWidth, Bitmap.Config.ARGB_8888
            ).apply {
                Canvas(this).apply {
                    val paint = Paint().apply {
                        isAntiAlias = true
                    }
                    drawOval(
                        RectF(
                            Rect(
                                10, 10, targetWidth - 10, targetWidth - 10
                            )
                        ), paint
                    )
                    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                    drawBitmap(
                        srcBitmap,
                        ((targetWidth - srcBitmap.width) / 2).toFloat(),
                        ((targetWidth - srcBitmap.height) / 2).toFloat(),
                        paint
                    )
                    drawOval(
                        RectF(
                            Rect(
                                5, 5, targetWidth - 5, targetWidth - 5
                            )
                        ), Paint().apply {
                            isAntiAlias = true
                            style = Paint.Style.STROKE
                            strokeWidth = 10f
                            color = Color.DKGRAY
                        })
                }
                srcBitmap.recycle()
            }

            val avatarFile = File(context.filesDir, "${uid}.jpg").apply {
                outputStream().apply {
                    resultBitmap.compress(
                        Bitmap.CompressFormat.PNG, 90, this
                    )
                    flush()
                }.close()
            }
            context.getSharedPreferences("avatars", 0).edit()
                .putString(uid, avatarFile.path).commit()
            return true
        } catch (err: Exception) {
            Log.e("AvatarPicker", "Error on avatar change: ${err.toString()}")
        }
    }
    return false
}
