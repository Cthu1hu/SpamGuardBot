package com.github.SpamGuardBot.config;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.groupadministration.BanChatMember;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;

import java.awt.desktop.SystemEventListener;
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
    public void startResponseTimer(Message userMessage, Integer botWelcomeMessageId,long newUserId) {
        long userId = userMessage.getFrom().getId();
        long chatId = userMessage.getChatId();

        // Создаем новый таймер
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    System.out.println("Timer expired for user " + newUserId + ". Kicking and deleting messages.");
                    kickUser(chatId, newUserId); // Удаляем пользователя
                    deleteMessage(chatId, botWelcomeMessageId); // Удаляем приветственное сообщение
                } catch (TelegramApiException e) {
                    System.out.println("Error kicking user " + newUserId + ": " + e.getMessage());
                }
                finally {
                    //timer.cancel();
                }
            }
        };

        // Запускаем таймер на 30 секунд
        timer.schedule(task, 8000);
        userTimers.put(newUserId, timer); // Сохраняем таймер
    }

    public void cancel(Long userId) {
        Timer timer = userTimers.get(userId);
        if (timer != null) {
            System.out.println("Cancelling timer for user " + userId);
            timer.cancel(); // Отменяем таймер
            userTimers.remove(userId); // Удаляем запись о таймере
        }
    }
    public void kickUser(Long chatId, Long userId) throws TelegramApiException {
        var kick = new BanChatMember();
        kick.setChatId(chatId.toString());
        kick.setUserId(userId);

        try {
            bot.execute(kick); // Выполняем удаление пользователя
            System.out.println("User " + userId + " kicked from chat " + chatId);
        } catch (TelegramApiException e) {
            if (e.getMessage().contains("can't remove chat owner")) {
                System.out.println("Cannot kick chat owner with ID " + userId);
            } else {
                throw e; // Пробрасываем исключение для обработки
            }
        }
    }







    // Удаление сообщения бота
    private void deleteMessage(Long chatId, Integer messageId) throws TelegramApiException {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId.toString());
        deleteMessage.setMessageId(messageId);

        try {
            bot.execute(deleteMessage); // Выполняем удаление
            System.out.println("Deleted message with ID " + messageId + " in chat " + chatId);
        } catch (TelegramApiException e) {
            System.out.println("Failed to delete message " + messageId + " in chat " + chatId + ": " + e.getMessage());
        }
    }



}
