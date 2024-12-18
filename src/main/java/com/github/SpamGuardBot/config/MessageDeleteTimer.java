package com.github.SpamGuardBot.config;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.BanChatMember;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MessageDeleteTimer {
    private final TelegramLongPollingBot bot;
    private final Map<Long, Timer> userTimers = new HashMap<>();

    public MessageDeleteTimer(TelegramLongPollingBot bot) {
        this.bot = bot;
    }

    public void startResponseTimer(Message userMessage, Integer botWelcomeMessageId, long newUserId) {
        long chatId = userMessage.getChatId();
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    kickUser(chatId, newUserId);
                    deleteMessage(chatId, botWelcomeMessageId);
                } catch (TelegramApiException e) {
                    System.out.println("Error kicking user " + newUserId + ": " + e.getMessage());
                } finally {
                    timer.cancel();
                    userTimers.remove(newUserId);
                }
            }
        };

        timer.schedule(task, 10000);
        userTimers.put(newUserId, timer);
    }

    public void cancel(Long userId) {
        Timer timer = userTimers.get(userId);
        if (timer != null) {
            timer.cancel();
            userTimers.remove(userId);
        }
    }

    public void kickUser(Long chatId, Long userId) throws TelegramApiException {    var kick = new BanChatMember();
        kick.setChatId(chatId.toString());
        kick.setUserId(userId);
        try {
            bot.execute(kick); // Выполняем удаление пользовател
            System.out.println("User " + userId + " kicked from chat " + chatId);    } catch (TelegramApiException e) {
            if (e.getMessage().contains("can't remove chat owner")) {
                System.out.println("Cannot kick chat owner with ID " + userId);
            }
            else {
                throw e; // Пробрасываем исключение для обработки
            }
        }
    }
    private void deleteMessage(Long chatId, Integer messageId) throws TelegramApiException {
        if (messageId == null) {
            return;
        }
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId.toString());
        deleteMessage.setMessageId(messageId);

        try {
            bot.execute(deleteMessage);
        } catch (TelegramApiException e) {
            System.out.println("Failed to delete message " + messageId + " in chat " + chatId + ": " + e.getMessage());
        }
    }
}