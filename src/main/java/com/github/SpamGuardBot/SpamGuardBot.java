package com.github.SpamGuardBot;

import com.github.SpamGuardBot.config.BotConfig;
import com.github.SpamGuardBot.config.MessageDeleteTimer;
import com.github.SpamGuardBot.config.MathQuestionGenerator;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SpamGuardBot extends TelegramLongPollingBot {
    private final BotConfig config;
    private final MessageDeleteTimer timer;
    private final MathQuestionGenerator questionGenerator;
    private final Set<Long> pendingUsers = ConcurrentHashMap.newKeySet();
    private final Map<Long, String> userQuestions = new ConcurrentHashMap<>(); // Храним вопросы для пользователей

    public SpamGuardBot(BotConfig config) {
        this.config = config;
        this.timer = new MessageDeleteTimer(this);
        this.questionGenerator = new MathQuestionGenerator();
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(@NotNull Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            long chatId = message.getChatId();

            // Обрабатываем новых пользователей
            if (message.getNewChatMembers() != null && !message.getNewChatMembers().isEmpty()) {
                for (User newUser : message.getNewChatMembers()) {
                    if (newUser.getId().equals(message.getMessageId())) {
                        sendWelcomeMessage(chatId);
                        return;
                    }

                    log.info("New user detected: " + newUser.getUserName());
                    pendingUsers.add(newUser.getId());

                    // Генерация математического вопроса через ИИ
                    String question = questionGenerator.generateMathQuestion("математика");
                    userQuestions.put(newUser.getId(), question);

                    Integer questionId = sendQuestion(chatId, question, newUser.getId());
                    timer.startResponseTimer(message, questionId, newUser.getId());
                }
            }
            // Обрабатываем ответы от пользователей
            else if (pendingUsers.contains(message.getFrom().getId())) {
                try {
                    handleUserResponse(chatId, message);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void sendWelcomeMessage(long chatId) {
        String welcomeText = "Привет! Я - SpamGuardBot. Для проверки ответьте на математический вопрос.";
        SendMessage welcomeMessage = new SendMessage(String.valueOf(chatId), welcomeText);
        try {
            execute(welcomeMessage);
        } catch (TelegramApiException e) {
            log.error("Failed to send welcome message: " + e.getMessage());
        }
    }

    private Integer sendQuestion(long chatId, String question, long userId) {
        String text = String.format("Привет, %d! Ответьте на вопрос для подтверждения: \n%s", userId, question);
        SendMessage questionMessage = new SendMessage(String.valueOf(chatId), text);
        try {
            execute(questionMessage);
            log.info("Question sent to user: " + userId);
        } catch (TelegramApiException e) {
            log.error("Failed to send question: " + e.getMessage());
        }
        return questionMessage.getMessageThreadId();
    }

    private void handleUserResponse(long chatId, Message message) throws TelegramApiException {
        String userAnswer = message.getText().trim();
        long userId = message.getFrom().getId();
        String question = userQuestions.get(userId);

        // Проверка ответа через ИИ
        if (question != null && MathQuestionGenerator.checkAnswerWithAI(question, userAnswer)) {
            sendVerificationSuccess(chatId, userId);
            pendingUsers.remove(userId);
            userQuestions.remove(userId);
        } else {
            sendVerificationFailure(chatId, userId);
            timer.kickUser(chatId, userId);
        }
    }

    private void sendVerificationSuccess(long chatId, long userId) {
        String successText = "Поздравляю! Вы успешно прошли проверку.";
        SendMessage successMessage = new SendMessage(String.valueOf(chatId), successText);
        try {
            execute(successMessage);
            log.info("User passed verification: " + userId);
        } catch (TelegramApiException e) {
            log.error("Failed to send success message: " + e.getMessage());
        }
    }

    private void sendVerificationFailure(long chatId, long userId) {
        String failureText = "К сожалению, ваш ответ неверный. Вы удалены из группы.";
        SendMessage failureMessage = new SendMessage(String.valueOf(chatId), failureText);
        try {
            execute(failureMessage);
            log.info("User failed verification: " + userId);
        } catch (TelegramApiException e) {
            log.error("Failed to send failure message: " + e.getMessage());
        }
    }
}
