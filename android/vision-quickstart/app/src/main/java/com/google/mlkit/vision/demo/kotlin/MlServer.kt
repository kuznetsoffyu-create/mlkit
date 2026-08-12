package com.google.mlkit.vision.demo.kotlin

import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject

class MlServer(port: Int) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        // Пример: обрабатываем только запросы на /process
        if (session.uri == "/process") {
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

        return newFixedLengthResponse(
            Response.Status.NOT_FOUND, 
            "text/plain", 
            "Not Found"
        )
    }
}