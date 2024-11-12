package com.example.mapsapp.util

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur

object BlurHelper {

    fun blur(context: Context, image: Bitmap, radius: Float): Bitmap {
        // Girdi bitmap'ini bulanıklaştırmak için yeni bir bitmap oluştur
        val outputBitmap = Bitmap.createBitmap(image)

        // RenderScript oluştur
        val renderScript = RenderScript.create(context)

        // Girdi ve çıktı için Allocation'ları ayarla
        val input = Allocation.createFromBitmap(renderScript, image)
        val output = Allocation.createFromBitmap(renderScript, outputBitmap)

        // Blur efektini uygula
        val script = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
        script.setRadius(radius) // Blur yarıçapını belirle (0f - 25f arası)
        script.setInput(input)
        script.forEach(output)

        // Sonuçları çıktı bitmap'ine kopyala
        output.copyTo(outputBitmap)

        // RenderScript'i serbest bırak
        renderScript.destroy()

        return outputBitmap
    }
}
