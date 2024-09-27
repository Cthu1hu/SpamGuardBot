package com.github.SpamGuardBot;
import com.github.SpamGuardBot.config.BotConfig;
import jakarta.validation.constraints.NotNull;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Arrays;

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
    public void onUpdateReceived(@NotNull Update update)
    {
        if( update.hasMessage() && update.getMessage().hasText()){
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String memberName = update.getMessage().getFrom().getFirstName();

                //if (messageText.equals("Ч") || messageText.equals("Р")){
                    //execute(InlineKeyboard.hermitageInlineKeyboardAb(chatId));
               // }
            /*switch(messageText){
                case "/start":
                    startBot(chatId, memberName);
                    break;
                default: log.info("Unexpected message");

            }*/
            startBot(chatId);
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
