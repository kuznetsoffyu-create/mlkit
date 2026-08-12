package com.google.mlkit.vision.demo.kotlin

import android.app.NotificationChannel
import android.app.NotificationManager
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

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
	{
		Log.d("ML_SERVER_DEBUG", "Run Foreground Service...")
		
		try {
			// Создаем уведомление, чтобы система не убила сервис
			val notification = NotificationCompat.Builder(this, CHANNEL_ID)
				.setContentTitle("ML Kit Server")
				.setContentText("Сервер работает на порту $PORT...")
				.setSmallIcon(android.R.drawable.ic_menu_info_details) // Замените на иконку вашего приложения
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