package com.example.mapsapp.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

fun createCombinedBitmapWithLabel(
    baseBitmap: Bitmap,
    labelText: String,
    fontSize: Float = 36f, // Yazı boyutu büyütüldü
    labelHeight: Int = 60  // Üstteki beyaz alan büyütüldü
): Bitmap {
    val width = baseBitmap.width
    val height = baseBitmap.height + labelHeight
    val combinedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    val canvas = Canvas(combinedBitmap)
    canvas.drawColor(Color.TRANSPARENT)

    // Beyaz arka plan kutusu
    val paintRect = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, width.toFloat(), labelHeight.toFloat(), paintRect)

    // Yazı ayarları
    val paintText = Paint().apply {
        color = Color.BLACK
        textSize = fontSize
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val xPos = width / 2f
    val yPos = (labelHeight / 2f) - ((paintText.descent() + paintText.ascent()) / 2)

    canvas.drawText(labelText, xPos, yPos, paintText)

    // Alttaki profil resmi
    canvas.drawBitmap(baseBitmap, 0f, labelHeight.toFloat(), null)

    return combinedBitmap
}
