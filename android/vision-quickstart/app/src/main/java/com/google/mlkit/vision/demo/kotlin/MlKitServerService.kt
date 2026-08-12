package com.google.mlkit.vision.demo.kotlin

import com.google.mlkit.vision.demo.R	// импорт ресурсов

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.util.Log

class MlKitServerService : Service()
{
    private var server: MlServer? = null
    private val PORT = 8000 // Порт, на котором будет работать сервер
    private val CHANNEL_ID = "MlKitServerChannel"
	
	// Константа для распознавания нажатия на кнопку "Остановить"
    companion object { const val ACTION_STOP_SERVER = "ACTION_STOP_SERVER" }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
	{
		// Проверяем, пришла ли команда на остановку от кнопки
        if (intent?.action == ACTION_STOP_SERVER)
		{
            Log.d("ML_SERVER_DEBUG", "Получена команда на остановку сервера.")
            stopServerAndService()
            return START_NOT_STICKY // Не перезапускать сервис после остановки
        }
		// Intent для кнопки "Остановить"
        val stopIntent = Intent(this, MlKitServerService::class.java).apply {
            action = ACTION_STOP_SERVER
        }
		// Оборачиваем его в PendingIntent (флаг IMMUTABLE обязателен для Android 12+)
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT )
		
		try {
			// Создаем уведомление, чтобы система не убила сервис
			val notification = NotificationCompat.Builder(this, CHANNEL_ID)
				.setContentTitle("ML Kit Server")
				.setContentText("Сервер работает на порту $PORT...")
				.setSmallIcon(R.drawable.logo_mlkit) // Замените на иконку вашего приложения
				// кнопка останова сервера:
				.addAction(
					android.R.drawable.ic_menu_close_clear_cancel, // Стандартная иконка крестика
					"Stop", // Текст на кнопке
					stopPendingIntent // Действие
				)
				.build()

			// Запускаем Foreground Service (с указанием типа для Android 14+)
			startForeground(1, notification)
			Log.d("ML_SERVER_DEBUG", "Уведомление создано и прикреплено.")

			// Запускаем NanoHTTPD
			if (server == null)
			{
				server = MlServer(PORT)
				server?.start()
				Log.d("ML_SERVER_DEBUG", "NanoHTTPD успешно стартовал на порту 8000!")
			}
		} catch (e: java.net.BindException) {
			Log.e("ML_SERVER_DEBUG", "ОШИБКА: Порт 8000 уже занят!", e)
		} catch (e: Exception) {
			Log.e("ML_SERVER_DEBUG", "КРИТИЧЕСКАЯ ОШИБКА при запуске сервера: ${e.message}", e)
		}

        return START_STICKY // Перезапустить сервис, если система его убьет
    }
	
	// чистая остановка NanoHTTPD+Foreground
    private fun stopServerAndService()
	{
        // 1. Останавливаем NanoHTTPD
        server?.stop()
        server = null
        Log.d("ML_SERVER_DEBUG", "NanoHTTPD остановлен.")

        // 2. Убираем сервис из Foreground (уведомление исчезнет)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        // 3. Полностью уничтожаем сервис
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
        server = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Не используем привязку (bound service)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ML Server Service",
                NotificationManager.IMPORTANCE_LOW // LOW, чтобы не было звука каждый раз
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}