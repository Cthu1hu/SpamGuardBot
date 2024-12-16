package com.github.SpamGuardBot.config;

import org.apache.hc.client5.http.fluent.Content;
import org.apache.hc.client5.http.fluent.Request;
import org.json.JSONObject;

public class MathQuestionGenerator {

    private static final String API_URL = "https://api.openai.com/v1/embeddings";
    private static final String API_KEY = "YOUR_API_KEY_HERE";

    public String generateMathQuestion(String topic) {
        try {
            // Формируем запрос к API
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "text-embedding-ada-002");
            requestBody.put("input", "Создай сложный математический вопрос по теме: " + topic);

            // Отправляем запрос
            Content response = Request.post(API_URL)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .bodyString(requestBody.toString(), org.apache.hc.core5.http.ContentType.APPLICATION_JSON)
                    .execute()
                    .returnContent();

            // Обрабатываем JSON-ответ
            JSONObject jsonResponse = new JSONObject(response.asString());

            // Извлекаем вектор и преобразуем его в строку (или используем как нужно)
            // Здесь берём input (ваш исходный текст, обработанный API)
            return jsonResponse.getJSONArray("data").getJSONObject(0).getString("input");

        } catch (Exception e) {
            e.printStackTrace();
            return "Ошибка генерации вопроса.";
        }
    }

    public static boolean checkAnswerWithAI(String question, String userAnswer) {
        try {
            // Формируем запрос для проверки ответа
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "text-embedding-ada-002");
            requestBody.put("input", String.format(
                    "Вопрос: %s. Ответ: %s. Ответ правильный? Если да, напиши 'Да', иначе 'Нет'.",
                    question, userAnswer));

            // Отправляем запрос
            Content response = Request.post(API_URL)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .bodyString(requestBody.toString(), org.apache.hc.core5.http.ContentType.APPLICATION_JSON)
                    .execute()
                    .returnContent();

            // Обрабатываем JSON-ответ
            JSONObject jsonResponse = new JSONObject(response.asString());

            // Получаем ответ модели
            String result = jsonResponse.getJSONArray("data").getJSONObject(0).getString("input").trim();

            return result.equalsIgnoreCase("Да");

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
