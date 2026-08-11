package com.google.mlkit.vision.demo.kotlin

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

class HeadlessMlKitActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Мы НЕ вызываем setContentView(), чтобы окно оставалось пустым
		
		// Получаем строку из ml:// ссылки или из ADB:
		val path = when {
			// Приоритет: данные из URI (ml://recognize?path=...)
			intent.data != null -> intent.data?.getQueryParameter("path")
				?: intent.data?.schemeSpecificPart
				?: "default_path_or_null"
			// Fallback: обычный extra (для ADB-вызовов)
			else -> intent.getStringExtra("path") ?: "default_path_or_null"
		}

		Toast.makeText(this, "Получен path: $path", Toast.LENGTH_SHORT).show()
		runMlKitTask(path)	// Запускаем обработку (имитация вызова ML Kit)
    }

    private fun runMlKitTask(imagePath: String) {
        // ВНИМАНИЕ: Здесь вы будете вызывать ваш ImageLabeling, TextRecognition и т.д.
        // Поскольку они асинхронные (возвращают Task), код ниже нужно будет 
        // перенести в .addOnSuccessListener { ... }

        try {
            // Формируем JSON-ответ
            val jsonResult = JSONObject().apply {
                put("status", "success")
                put("input_path", imagePath)
                put("results", JSONArray(listOf("cat", "dog", "table"))) // Тестовый массив
            }

            // 4. "Возвращаем" результат через Logcat со специальным тегом
            Log.i("MLKIT_HEADLESS_RESULT", jsonResult.toString())

        } catch (e: Exception) {
            val errorJson = JSONObject().apply {
                put("status", "error")
                put("message", e.message)
            }
            Log.e("MLKIT_HEADLESS_RESULT", errorJson.toString())
        } finally {
            // 5. Обязательно завершаем Activity после получения результата!
            // Иначе прозрачный экран останется висеть поверх других приложений.
            finish()
        }
    }
}