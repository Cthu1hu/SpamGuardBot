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

    // Структура для картинок, описаний и допустимых ответов
    private static class VerificationImage {
        String filePath;
        String description;
        List<String> validResponses;

        public VerificationImage(String filePath, String description, List<String> validResponses) {
            this.filePath = filePath;
            this.description = description;
            this.validResponses = validResponses;
        }
    }

    // Список картинок, описаний и допустимых ответов
    private List<VerificationImage> verificationImages = List.of(
            new VerificationImage("C:\\Users\\M.Yurkevich\\ph1.jpg", "По какому предмету эта книга?",
                    List.of("матан", "матанализ", "математический анализ", "мат анализ", "Матан", "Матанализ", "Математический анализ", "Мат анализ")),
            new VerificationImage("C:\\Users\\M.Yurkevich\\ph2.jpg", "Назовите наазвание этой теоремы.",
                    List.of("Теорема Виета", "теорема виета", "виета", "Виета")),
            new VerificationImage("C:\\Users\\M.Yurkevich\\ph3.jpg", "Что это за язык программирования?",
                    List.of("джава", "Джава", "Java", "java")),
            new VerificationImage("C:\\Users\\M.Yurkevich\\ph4.jpg", "Вычислите определитель матрицы второго порядка.",
                    List.of("2", "два")),
            new VerificationImage("C:\\Users\\M.Yurkevich\\ph5.jpg", "Вычислите объем куба. ",
                    List.of("105", "сто пять", "105м", "105м^3"))
    );

    private final Random random = new Random(); // Для случайного выбора картинки

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

            log.info("Received a message update in chat: " + chatId);

            if (message.getNewChatMembers() != null && !message.getNewChatMembers().isEmpty()) {
                log.info("New members detected in chat: " + chatId);

                for (User newUser : message.getNewChatMembers()) {
                    log.info("New member username: " + newUser.getUserName() + ", ID: " + newUser.getId());

                    if (newUser.getId().equals(getMe().getId())) {
                        sendWelcomeMessage(newUser.getId());
                        return;
                    }

                    newUserId = newUser.getId();
                    verificationMessageId = sendVerificationMessage(chatId);
                    pendingUsers.add(newUserId); // Добавляем пользователя в список проверяемых
                    timer.startResponseTimer(message, verificationMessageId, newUserId); // Запускаем таймер
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
            log.info("Welcome message sent to chat: " + chatId);
        } catch (TelegramApiException e) {
            log.error("Failed to send welcome message: " + e.getMessage());
        }
    }

    private Integer sendVerificationMessage(long chatId) {
        // Случайный выбор картинки из списка
        int randomIndex = random.nextInt(verificationImages.size());
        VerificationImage randomImage = verificationImages.get(randomIndex);

        // Создаем объект PhotoSender
        PhotoSender photoSender = new PhotoSender(this);

        // Отправляем фото через PhotoSender и возвращаем messageId
        return photoSender.sendPhoto(chatId, randomImage.filePath, randomImage.description);
    }

    private void handleUserResponse(long chatId, Message message) {
        String userResponse = message.getText().toLowerCase().trim();

        // Преобразуем список картинок в карту с возможностью поиска картинки по пути
        VerificationImage currentImage = verificationImages.stream()
                .filter(image -> image.filePath.equals(message.getReplyToMessage().getText())) // Ищем картинку по пути
                .findFirst()
                .orElse(null);

        if (currentImage != null && currentImage.validResponses.contains(userResponse)) {
            try {
                deleteMessages(chatId, verificationMessageId, message.getMessageId());
                log.info("User " + message.getFrom().getId() + " passed verification.");
            } catch (TelegramApiException e) {
                log.error("Failed to delete verification messages: " + e.getMessage());
            }
        } else {
            try {
                timer.kickUser(chatId, message.getFrom().getId()); // Удаляем пользователя
                log.info("User " + message.getFrom().getId() + " failed verification and was removed.");
            } catch (TelegramApiException e) {
                log.error("Failed to kick user: " + e.getMessage());
            }
        }
    }

    private void deleteMessages(long chatId, Integer... messageIds) throws TelegramApiException {
        for (Integer messageId : messageIds) {
            if (messageId != null) {
                execute(new org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage(String.valueOf(chatId), messageId));
            }
        }
    }
}