package com.github.SpamGuardBot;

import com.github.SpamGuardBot.config.BotConfig;
import com.github.SpamGuardBot.config.MessageDeleteTimer;
import jakarta.validation.constraints.NotNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import com.github.SpamGuardBot.config.PhotoSender;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SpamGuardBot extends TelegramLongPollingBot {
    final BotConfig config;
    private Long newUserId = null;
    private Integer verificationMessageId = null;
    private MessageDeleteTimer timer;
    private final Set<Long> pendingUsers = ConcurrentHashMap.newKeySet();

    private static class VerificationImage {
        String filePath;
        String description;
        List<String> validResponses;

        public VerificationImage(String filePath, String description) {
            this.filePath = filePath;
            this.description = description;
            this.validResponses = List.of("матан", "матанализ", "математический анализ", "мат анализ","Виета", "виета", "Виета","джава","два","2","105");
        }

        public VerificationImage() {

        }
    }

    private List<VerificationImage> verificationImages = List.of(
            new VerificationImage("C:\\Users\\ursvn\\Downloads\\ph1.jpg", "По какому предмету эта книга?"),
            new VerificationImage("C:\\Users\\ursvn\\Downloads\\ph2.jpg", "Назовите название этой теоремы."),

            new VerificationImage("C:\\Users\\ursvn\\Downloads\\ph3.jpg", "Что за язык"),

            new VerificationImage("C:\\Users\\ursvn\\Downloads\\ph4.jpg", "Определитель?????"),

            new VerificationImage("C:\\Users\\ursvn\\Downloads\\ph5.jpg", "навали Value")

    );

    private final Random random = new Random();

    public SpamGuardBot(BotConfig config) {
        this.config = config;
        this.timer = new MessageDeleteTimer(this);
    }

    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() { return config.getToken(); }

    @SneakyThrows
    @Override
    public void onUpdateReceived(@NotNull Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            long chatId = message.getChatId();

            if (message.getNewChatMembers() != null && !message.getNewChatMembers().isEmpty()) {
                for (User newUser : message.getNewChatMembers()) {
                    if (newUser.getId().equals(getMe().getId())) {
                        sendWelcomeMessage(newUser.getId());
                        return;
                    }

                    newUserId = newUser.getId();
                    verificationMessageId = sendVerificationMessage(chatId);
                    pendingUsers.add(newUserId);
                    timer.startResponseTimer(message, verificationMessageId, newUserId);
                }
            } else if (message.getReplyToMessage() != null && pendingUsers.contains(message.getFrom().getId())) {
                handleUserResponse(chatId, message);
            }
        }
    }

    private void sendWelcomeMessage(long chatId) {
        String welcomeText = "Привет! Я - SpamGuardBot, и я здесь, чтобы помочь защитить эту группу от спама!";
        SendMessage welcomeMessage = new SendMessage();
        welcomeMessage.setChatId(String.valueOf(chatId));
        welcomeMessage.setText(welcomeText);

        try {
            execute(welcomeMessage);
        } catch (TelegramApiException e) {
            log.error("Failed to send welcome message: {}", e.getMessage());
        }
    }

    private Integer sendVerificationMessage(long chatId) {
        int randomIndex = random.nextInt(verificationImages.size());
        VerificationImage randomImage = verificationImages.get(randomIndex);
        PhotoSender photoSender = new PhotoSender(this);
        return photoSender.sendPhoto(chatId, randomImage.filePath, randomImage.description);
    }

    private void handleUserResponse(long chatId, Message message) {
        long userId = message.getFrom().getId();
        String userResponse = message.getText().toLowerCase().trim();
        log.info(userResponse);
        Random random = new Random();
        VerificationImage currentImage = new VerificationImage();
        currentImage.validResponses = verificationImages.stream().findAny().get().validResponses;
        System.out.println(currentImage.validResponses);
        if (currentImage.validResponses.contains(userResponse)) {
            try {
                timer.cancel(userId);
                deleteMessages(chatId, verificationMessageId, message.getMessageId());
                pendingUsers.remove(userId);
            } catch (TelegramApiException e) {
                log.error("Failed to delete verification messages: {}", e.getMessage());
            }
        } else {
            try {
                timer.cancel(userId);
                timer.kickUser(chatId, userId);
                pendingUsers.remove(userId);
            } catch (TelegramApiException e) {
                log.error("Failed to kick user: {}", e.getMessage());
            }
        }
    }

    private void cancelTimer(Long userId) {
        timer.cancel(userId);
    }

    private void deleteMessages(long chatId, Integer... messageIds) throws TelegramApiException {
        for (Integer messageId : messageIds) {
            if (messageId != null) {
                execute(new org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage(String.valueOf(chatId), messageId));
            }
        }
    }
}