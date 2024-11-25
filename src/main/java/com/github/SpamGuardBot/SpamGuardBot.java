package com.github.SpamGuardBot;

import com.github.SpamGuardBot.config.BotConfig;
import com.github.SpamGuardBot.config.MessageDeleteTimer;
import jakarta.validation.constraints.NotNull;
import lombok.SneakyThrows;
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
public class SpamGuardBot extends TelegramLongPollingBot {
    final BotConfig config;
    private Long newUserId = null; // Поле для хранения ID нового участника
    private Integer verificationMessageId = null; // ID сообщения с проверкой для удаления
    private MessageDeleteTimer timer; // Экземпляр таймера

    public SpamGuardBot(BotConfig config) {
        this.config = config;
        this.timer = new MessageDeleteTimer(this); // Создаем таймер с передачей бота
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(@NotNull Update update) {
        if (update.hasMessage()) {
            var message = update.getMessage();
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

                    newUserId = newUser.getId(); // Сохраняем ID нового участника
                    verificationMessageId = sendVerificationMessage(chatId); // Сохраняем ID сообщения
                    timer.startResponseTimer(message, verificationMessageId,newUserId); // Запускаем таймер для удаления
                }
            }
        }

        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        }
    }


    private void sendWelcomeMessage(long chatId) {
        String welcomeText = "Привет! Я - SpamGuardBot, и я здесь, чтобы помочь защитить эту группу от спама!";
        SendMessage welcomeMessage = new SendMessage();
        welcomeMessage.setChatId(String.valueOf(chatId));
        welcomeMessage.setText(welcomeText);

        log.info("Preparing to send welcome message to chat: " + chatId + " with text: " + welcomeText);

        try {
            execute(welcomeMessage);
            log.info("Welcome message sent to chat: " + chatId);
        } catch (TelegramApiException e) {
            log.error("Failed to send welcome message: " + e.getMessage());
        }
    }

    private Integer sendVerificationMessage(long chatId) {
        SendMessage message = Button.InlineKeyboard(chatId); // Сообщение с кнопкой для проверки

        try {
            var sentMessage = execute(message);
            if (sentMessage != null && sentMessage.getMessageId() != null) {
                log.info("Verification message sent to chat: " + chatId);
                return sentMessage.getMessageId(); // Возвращаем ID сообщения для таймера
            } else {
                log.error("Failed to retrieve message ID for verification message.");
            }
        } catch (TelegramApiException e) {
            log.error("Failed to send verification message: " + e.getMessage());
        }
        return null;
    }


    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callData = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Long userId = callbackQuery.getFrom().getId(); // ID пользователя, который отправил коллбэк
        SendMessage message = new SendMessage();
        // Если пользователь подтвердил, что он человек
        if ("ЧЕЛОВЕК".equals(callData)) {
            log.info("User " + userId + " verified as human.");
            message.setText("Человеков мы любим!");
            timer.cancel(newUserId); // Отменяем таймер
        }
        // Если пользователь подтвердил, что он робот
        else if ("РОБОТ".equals(callData)) {
            log.info("User " + userId + " identified as robot.");
            message.setText("Роботы стоять");
            timer.cancel(newUserId); // Отменяем таймер, чтобы избежать повторного срабатывания
            try {
                timer.kickUser(chatId, newUserId); // Удаляем
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }
}