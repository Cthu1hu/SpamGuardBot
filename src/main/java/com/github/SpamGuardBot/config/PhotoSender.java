package com.github.SpamGuardBot.config;

import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.io.File;

public class PhotoSender {

    private final AbsSender bot;

    public PhotoSender(AbsSender bot) {
        this.bot = bot;
    }

    public Integer sendPhoto(long chatId, String filePath, String caption) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(String.valueOf(chatId));

        File photoFile = new File(filePath);
        if (!photoFile.exists()) {
            System.err.println("File not found: " + filePath);
            return null;
        }

        sendPhoto.setPhoto(new InputFile(photoFile));
        sendPhoto.setCaption(caption); // Теперь описание передается как параметр
        try {
            Message message = bot.execute(sendPhoto);
            if (message != null && message.getMessageId() != null) {
                System.out.println("Verification message sent to chat: " + chatId);
                return message.getMessageId();
            } else {
                System.err.println("Failed to retrieve message ID for verification message.");
            }
        } catch (TelegramApiException e) {
            System.err.println("Failed to send verification message: " + e.getMessage());
        }
        return null;
    }
}