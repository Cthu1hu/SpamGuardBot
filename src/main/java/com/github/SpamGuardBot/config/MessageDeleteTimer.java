package com.github.SpamGuardBot.config;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MessageDeleteTimer {
    private final TelegramLongPollingBot bot;
    private final Map<Long, Timer> userTimers = new HashMap<>(); // Хранение таймеров для каждого пользователя

    public MessageDeleteTimer(TelegramLongPollingBot bot) {
        this.bot = bot;
    }

    // Запуск таймера для удаления сообщения и исключения пользователя
    public void startResponseTimer(Message userMessage, Integer botWelcomeMessageId) {
        Long userId = userMessage.getFrom().getId();
        Long chatId = userMessage.getChatId();

        // Создаем новый таймер для каждого пользователя
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    // Удаляем пользователя и приветственное сообщение, если он не ответил
                    kickUser(chatId, userId);
                    deleteMessage(chatId, botWelcomeMessageId);
                    System.out.println("User " + userId + " has been kicked and message deleted due to timeout.");
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                } finally {
                    userTimers.remove(userId); // Удаляем таймер из списка после выполнения
                }
            }
        };

        timer.schedule(task, 30000); // Таймер на 30 секунд
        userTimers.put(userId, timer); // Сохраняем таймер для возможности отмены
    }

    // Метод для отмены таймера по ID пользователя
    public void cancel() {
        Timer timer = userTimers.get(userTimers);
        if (timer != null) {
            timer.cancel(); // Отменяем таймер
            userTimers.remove(userTimers); // Удаляем таймер из списка
        }
    }

    // Исключение пользователя из чата
    public void kickUser(Long chatId, Long userId) throws TelegramApiException {
        KickChatMember kick = new KickChatMember();
        kick.setChatId(chatId.toString());
        kick.setUserId(userId);
        bot.execute(kick);
    }

    // Удаление сообщения бота
    private void deleteMessage(Long chatId, Integer messageId) throws TelegramApiException {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId.toString());
        deleteMessage.setMessageId(messageId);
        bot.execute(deleteMessage);
    }
}
