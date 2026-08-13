package com.google.mlkit.vision.demo.kotlin

import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import java.io.File
import kotlin.concurrent.thread

class MlServer(port: Int) : NanoHTTPD(port)
{

    override fun serve(session: IHTTPSession): Response
	{
		val uri = session.uri
        val method = session.method

        // Пример: обрабатываем только запросы на /process
        if (session.uri == "/process")
		{
            // Получаем параметры запроса (например, ?path=/sdcard/test.jpg)
            val params = session.parameters
            val path = params["path"]?.firstOrNull()

            if (path == null) {
                return newFixedLengthResponse(
                    Response.Status.BAD_REQUEST, 
                    "application/json", 
                    "{\"error\": \"Missing 'path' parameter\"}"
                )
            }

            // ЗДЕСЬ ДОЛЖЕН БЫТЬ ВЫЗОВ ML KIT (или передача задачи)
            // Имитируем успешную обработку:
            val jsonResponse = JSONObject().apply {
                put("status", "success")
                put("file", path)
                put("result", "cat") // Имитация ответа от нейросети
            }

            return newFixedLengthResponse(
                Response.Status.OK,
                "application/json",
                jsonResponse.toString()
            )
        }
		
		// Эндпоинт 1: ЗАПРОС ПРОГРЕССА (GET /progress)
        if (uri == "/progress" && method == Method.GET)
		{
            val json = JSONObject().apply {
                put("status", BatchState.status)
                put("total", BatchState.totalFiles)
                put("processed", BatchState.processedFiles)
                put("current_file", BatchState.currentFile)
            }
            return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString())
        }

        // Эндпоинт 2: СТАРТ ОБРАБОТКИ (POST /start_batch)
        if (uri == "/start_batch" && method == Method.POST)
		{
            if (BatchState.isProcessing) {
                return newFixedLengthResponse(
                    Response.Status.CONFLICT, "application/json", 
                    "{\"error\": \"Processing already running\"}"
                )
            }

            // Читаем POST-параметры. В NanoHTTPD это делается через parseBody
            val map = HashMap<String, String>()
            session.parseBody(map)
            val params = session.parameters
            
            val inputDir = params["input_dir"]?.firstOrNull() ?: "/sdcard/Download/raw_photos"
            val outputDir = params["output_dir"]?.firstOrNull() ?: "/sdcard/Download/processed_photos"

            // Запускаем обработку в отдельном потоке, чтобы не блокировать ответ сервера
            startBatchProcessing(inputDir, outputDir)

            val json = JSONObject().apply {
                put("status", "started")
                put("input_dir", inputDir)
                put("output_dir", outputDir)
            }
            return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString())
        }

		return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
    }
}

object BatchState
{
    var isProcessing = false
    var totalFiles = 0
    var processedFiles = 0
    var currentFile = ""
    var status = "idle" // "idle", "running", "finished", "error"
}

private fun startBatchProcessing (inputPath: String, outputPath: String)
{
	val inputDir = File(inputPath)
	val outputDir = File(outputPath)

	if (!inputDir.exists() || !inputDir.isDirectory) {
		BatchState.status = "error: input directory not found"
		return
	}

	if (!outputDir.exists())
		outputDir.mkdirs() // Создаем папку для результатов, если её нет

	// Ищем только картинки
	val files = inputDir.listFiles { file ->
		file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp")
	} ?: emptyArray()

	BatchState.isProcessing = true
	BatchState.totalFiles = files.size
	BatchState.processedFiles = 0
	BatchState.status = "running"

	// Запускаем фоновый поток (Worker Thread)
	thread(start = true)
	{
		for (file in files) {
			BatchState.currentFile = file.name
			
			// ЗДЕСЬ ПОЗЖЕ БУДЕТ ВЫЗОВ ML KIT
			// Пока просто имитируем задержку (типа обрабатываем фото)
			Thread.sleep(1500) 

			BatchState.processedFiles++
		}
		
		BatchState.isProcessing = false
		BatchState.status = "finished"
	}
}
