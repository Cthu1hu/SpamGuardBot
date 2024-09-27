package com.github.SpamGuardBot;
import com.github.SpamGuardBot.config.BotConfig;
import jakarta.validation.constraints.NotNull;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


import java.util.ArrayList;
import java.util.List;


@Slf4j
@Component
public class Button extends TelegramLongPollingBot {
    final BotConfig config;

    public Button(BotConfig config) {
        this.config = config;
    }

    @Override
    public String getBotUsername() {
        return "";
    }

    @Override
    public String getBotToken() { return config.getToken(); }


    long chatId = 0;

    @Override
    public void onUpdateReceived(@NotNull Update update)
    {
        if (update.hasMessage() && update.getMessage().hasText()){
            String messageText = update.getMessage().getText();
            chatId = update.getMessage().getChatId();

            if (messageText.equals("А-Я")){
                try {
                    execute(InlineKeyboard(chatId));
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
            else if (update.hasCallbackQuery()){
                String call_data = update.getCallbackQuery().getData();
                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(chatId));

                if (call_data.equals("ЧЕЛОВЕК")){
                    message.setText(String.valueOf("Человеков мы любим"));
                    System.out.println(messageText);
                    try {
                        execute(message);
                        log.info("Reply sent");
                    }
                    catch(TelegramApiException e){
                        log.error(e.getMessage());
                    }
                }

                else if (call_data.equals("РОБОТ")){
                    message.setText(String.valueOf("Роботы стоять"));
                    System.out.println(messageText);
                    try {
                        execute(message);
                        log.info("Reply sent");
                    }
                    catch(TelegramApiException e){
                        log.error(e.getMessage());
                    }
                }

            }

        }
    }

    public static SendMessage InlineKeyboard (long chatId){
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выбери, кто ты");

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();

        InlineKeyboardButton inlineKeyboardButton1 = new InlineKeyboardButton();
        inlineKeyboardButton1.setText("Человек");
        inlineKeyboardButton1.setCallbackData("ЧЕЛОВЕК");

        InlineKeyboardButton inlineKeyboardButton2 = new InlineKeyboardButton();
        inlineKeyboardButton2.setText("Робот");
        inlineKeyboardButton2.setCallbackData("РОБОТ");

        row1.add(inlineKeyboardButton1);
        row1.add(inlineKeyboardButton2);

        rows.add(row1);

        markupInline.setKeyboard(rows);
        message.setReplyMarkup(markupInline);

        return message;
    }

}
