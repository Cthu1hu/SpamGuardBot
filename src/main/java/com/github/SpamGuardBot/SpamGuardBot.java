package com.github.SpamGuardBot;
import com.github.SpamGuardBot.config.BotConfig;
import jakarta.validation.constraints.NotNull;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;




@Slf4j
@Component
public class SpamGuardBot extends TelegramLongPollingBot{
    final BotConfig config;

    public SpamGuardBot(BotConfig config) { this.config = config; }
    @Override
    public String getBotUsername() { return config.getBotName(); }
    @Override
    public String getBotToken() { return config.getToken(); }
    @Override
    public void onUpdateReceived(@NotNull Update update) {
        if (update.hasMessage()) {
            var message = update.getMessage();
            long chatId = message.getChatId();

            // Проверка на добавление новых участников
            if (message.getNewChatMembers() != null && !message.getNewChatMembers().isEmpty()) {
                for (User newUser : message.getNewChatMembers()) {
                    // Отправка сообщения с кнопками для новых участников
                    sendVerificationMessage(chatId);
                }
            }
        }

        // Обработка CallbackQuery
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        }
    }

    private void sendVerificationMessage(long chatId) {
        // Используем метод из Button для создания сообщения с кнопками
        SendMessage message = Button.InlineKeyboard(chatId);

        try {
            execute(message);
            log.info("Verification message sent to chat: " + chatId);
        } catch (TelegramApiException e) {
            log.error("Failed to send verification message with buttons: " + e.getMessage());
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));

        if ("ЧЕЛОВЕК".equals(callData)) {
            message.setText("Человеков мы любим");
        } else if ("РОБОТ".equals(callData)) {
            message.setText("Роботы стоять");
        }

        try {
            execute(message);
            log.info("Callback response sent to chat: " + chatId);
        } catch (TelegramApiException e) {
            log.error("Failed to send callback response: " + e.getMessage());
        }
    }
}