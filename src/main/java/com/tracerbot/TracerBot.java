package com.tracerbot;

import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.abilitybots.api.sender.DefaultSender;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.abilitybots.api.sender.SilentSender;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

public class TracerBot extends AbilityBot {

    private SilentSender silent;

    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

    TracerBot() {
        super("466535663:AAFbWNw1-GeZJkh0PQFGABIgjZ8ieztZMI4",
                "GeoTracerBot");
        MessageSender sender = new DefaultSender(this);
        silent = new SilentSender(sender);
    }

    public Ability sayHelloWorld() {
        return Ability
                .builder()
                .name("hello")
                .info("says hello world!")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> this.executor.scheduleWithFixedDelay(
                        new YouSuck(ctx), 0L, 5L, SECONDS
                ))
                .build();
    }

    class YouSuck implements Runnable {
        private MessageContext context;

        YouSuck(MessageContext context) {
            this.context = context;
        }

        @Override
        public void run() {
            silent.send("http://ruinformer.com/uploads/_pages/43341/1dd3b3bd805a3896baff4e63223ba200.jpg", context.chatId());
        }
    }

    public String getBotUsername() {
        return "GeoTracerBot";
    }

    @Override
    public int creatorId() {
        return new Random().nextInt();
    }

    public String getBotToken() {
        return "466535663:AAFbWNw1-GeZJkh0PQFGABIgjZ8ieztZMI4";
    }
}
