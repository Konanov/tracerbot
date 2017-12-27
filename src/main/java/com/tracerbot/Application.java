package com.tracerbot;

import marytts.LocalMaryInterface;
import marytts.exceptions.MaryConfigurationException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;

@SuppressWarnings("unused")
@SpringBootApplication
public class Application {

    @Bean
    public LocalMaryInterface maryInterface() throws MaryConfigurationException {
        return new LocalMaryInterface();
    }

    @Bean
    public TracerBot tracerBot() {
        ApiContextInitializer.init();
        TelegramBotsApi botsApi = new TelegramBotsApi();

        TracerBot bot = new TracerBot();
        try {
            botsApi.registerBot(bot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return bot;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
