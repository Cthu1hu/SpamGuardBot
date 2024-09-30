package com.github.SpamGuardBot;
import com.github.SpamGuardBot.config.BotConfig;
import jakarta.validation.constraints.NotNull;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
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
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (!messageText.isEmpty()) {
                try {
                    execute(Button.InlineKeyboard(chatId));
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (update.hasCallbackQuery()) {
            String call_data = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));

            if (call_data.equals("ЧЕЛОВЕК")) {
                message.setText("Человеков мы любим");
                System.out.println(call_data);
                try {
                    execute(message);
                    log.info("Reply sent");
                } catch (TelegramApiException e) {
                    log.error(e.getMessage());
                }
            } else if (call_data.equals("РОБОТ")) {
                message.setText("Роботы стоять");
                System.out.println(call_data);
                try {
                    execute(message);
                    log.info("Reply sent");
                } catch (TelegramApiException e) {
                    log.error(e.getMessage());
                }
            }
        }
    }

    private void startBot(long chatId){

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));

        try{
            execute(message);
            log.info("Reply sent");
        }
        catch(TelegramApiException e){
            log.error(e.getMessage());
        }
    }
}
