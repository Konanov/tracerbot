package com.tracerbot;

import com.jcraft.jsch.IO;
import com.jcraft.jsch.JSchException;
import lombok.extern.slf4j.Slf4j;
import marytts.LocalMaryInterface;
import marytts.exceptions.SynthesisException;
import marytts.util.data.audio.MaryAudioUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.abilitybots.api.sender.DefaultSender;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendVoice;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import javax.sound.sampled.AudioInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Slf4j
@Service
@SuppressWarnings("unused")
public class TracerBot extends AbilityBot {

    @Autowired
    private LocalMaryInterface maryInterface;

    private Map<Long, SshUtil> sessions;

    private MessageSender botSender;

    private String tmpFolder;

    TracerBot() {
        super("466535663:AAFbWNw1-GeZJkh0PQFGABIgjZ8ieztZMI4",
                "GeoTracerBot");
        this.botSender = new DefaultSender(this);
        tmpFolder = System.getProperty("java.io.tmpdir");
        sessions = new ConcurrentHashMap<>();
    }

    public Ability say() {
        return Ability
                .builder()
                .name("say")
                .info("synthesize speech!")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> {
                    final String name = ctx.user().fullName();
                    try {
                        SendVoice speech = synthesize(ctx);
                        botSender.sendVoice(speech);
                        log.info("Voice message successfully sent to user {}", name);
                    } catch (TelegramApiException e) {
                        log.error("Unable to send voice message on request of user {}. Exception: \n{}",
                                name, e.getMessage());
                    }
                })
                .build();
    }

    public Ability getCredentials() {
        return Ability
                .builder()
                .name("credentials")
                .info("get credentials to execute by ssh")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> {
                    Long chatId = ctx.chatId();
                    SshUtil ssh = new SshUtil(ctx.firstArg(), ctx.secondArg(), ctx.thirdArg());
                    sessions.put(chatId, ssh);
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText(String.format("Connection was established to %s.", ctx.firstArg()));
                    try {
                        botSender.execute(message);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                })
                .build();
    }

    public Ability executeBySsh() {
        return Ability
                .builder()
                .name("execute")
                .info("execute command by ssh")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> {
                    Long chatId = ctx.chatId();
                    String uncut = ctx.update().getMessage().getText();
                    String command = uncut.substring(8, uncut.length());
                    SshUtil ssh = sessions.get(chatId);
                    String result;
                    try {
                        result = ssh.executeCommand(command);
                    } catch (JSchException | IOException e) {
                        result = "FAIL";
                        e.printStackTrace();
                    }
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText(String.format("Result of command is %s.", result));
                    try {
                        botSender.execute(message);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                })
                .build();
    }

    private SendVoice synthesize(MessageContext context) {
        String uncut = context.update().getMessage().getText();
        String text = uncut.substring(5, uncut.length());

        AudioInputStream audio = null;
        try {
            audio = maryInterface.generateAudio(text);
        } catch (SynthesisException e) {
            log.error("Synthesis failed: \n{}", e.getMessage());
            System.exit(1);
        }
        double[] samples = MaryAudioUtils.getSamplesAsDoubleArray(audio);
        SendVoice sendAudio = new SendVoice();

        Path tempFile;
        try {
            tempFile = Files.createFile(Paths.get(tmpFolder).resolve(context.firstArg()));
            MaryAudioUtils.writeWavFile(samples, tempFile.toString(), audio.getFormat());
        } catch (IOException e) {
            tempFile = Paths.get(tmpFolder);
            log.error("Unable to allocate file in the temp folder. This request will fail.");
        }

        try {
            sendAudio.setNewVoice("Synthesized", Files.newInputStream(tempFile, StandardOpenOption.READ));
            Files.delete(tempFile);
        } catch (IOException e) {
            log.error("Unable to open inputStream to read temp file from path: \n{}", tempFile.toString());
        }
        sendAudio.setChatId(context.chatId());
        return sendAudio;
    }

    //class Task implements Runnable {
    //    private MessageContext context;
//
    //    Task(MessageContext context) {
    //        this.context = context;
    //    }
//
    //    @Override
    //    public void run() {
    //        silentSender.send("http://ruinformer.com/uploads/_pages/43341/1dd3b3bd805a3896baff4e63223ba200.jpg", context.chatId());
    //    }
    //}

    @Override
    public String getBotUsername() {
        return "GeoTracerBot";
    }

    @Override
    public int creatorId() {
        return new Random().nextInt();
    }

    @Override
    public String getBotToken() {
        return "466535663:AAFbWNw1-GeZJkh0PQFGABIgjZ8ieztZMI4";
    }
}
