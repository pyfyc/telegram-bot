package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private TelegramBot telegramBot;
    private NotificationTaskRepository notificationTaskRepository;

    private final static String WELCOME_MSG_TEXT = "Hi there! It is a chat bot :)";
    private final static String NOTIFICATION_TASK_PATTERN = "([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)";
    private final static String DATE_TIME_FORMATTER = "dd.MM.yyyy HH:mm";

    public TelegramBotUpdatesListener(TelegramBot telegramBot, NotificationTaskRepository notificationTaskRepository) {
        this.telegramBot = telegramBot;
        this.notificationTaskRepository = notificationTaskRepository;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            if (update.message() == null) {
                return;
            }

            logger.info("Processing update: {}", update);

            String incomeMsgText = update.message().text();
            long chatId = update.message().chat().id();

            // Process welcome message
            if (incomeMsgText.equals("/start")) {
                sendMessage(chatId, WELCOME_MSG_TEXT);
            }

            // Process notification task message
            else {
                Pattern pattern = Pattern.compile(NOTIFICATION_TASK_PATTERN);
                Matcher matcher = pattern.matcher(incomeMsgText);
                if (matcher.matches()) {
                    String date = matcher.group(1);
                    String message = matcher.group(3);
                    LocalDateTime localDateTime = LocalDateTime.parse(date, DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER));
                    logger.info("Notification task message (date: {}, message: {})", localDateTime, message);
                    notificationTaskRepository.save(new NotificationTask(chatId, message, localDateTime));
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    @Scheduled(cron = "0 0/1 * * * *") // runs at 00 sec on every minute
    public void sendNotificationTasks() {
        Collection<NotificationTask> currentTasks = notificationTaskRepository.findAllTasksByDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        currentTasks.forEach(task -> sendMessage(task.getChatId(), task.getMessage()));
    }

    private void sendMessage(long chatId, String messageText) {
        SendMessage message = new SendMessage(chatId, messageText);
        SendResponse response = telegramBot.execute(message);
        if (!response.isOk()) {
            logger.warn("Message was not sent: {}, error code: {}", message, response.errorCode());
        }
    }
}
