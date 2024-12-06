package com.github.SpamGuardBot.config;

import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.io.File;

public class PhotoSender {

    private final AbsSender bot;

    // Конструктор, принимающий экземпляр бота
    public PhotoSender(AbsSender bot) {
        this.bot = bot;
    }

    // Метод для отправки фото в чат
    public Integer sendPhoto(long chatId, String filePath) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(String.valueOf(chatId));  // Устанавливаем ID чата
        sendPhoto.setPhoto(new InputFile(new File(filePath)));  // Устанавливаем путь к фото
        sendPhoto.setCaption("По какому предметы данный сборник?");
        try {
            // Отправляем фото и получаем сообщение
            Message message = bot.execute(sendPhoto);
            if (message != null && message.getMessageId() != null) {
                System.out.println("Verification message sent to chat: " + chatId);
                return message.getMessageId();  // Возвращаем ID отправленного сообщения
            } else {
                System.err.println("Failed to retrieve message ID for verification message.");
            }
        } catch (TelegramApiException e) {
            System.err.println("Failed to send verification message: " + e.getMessage());
        }
        return null;  // Возвращаем null, если не удалось отправить фото
    }
}
