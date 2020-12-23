package ru.fbtw.navigator.bot_controller.service;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import ru.fbtw.navigator.bot_controller.domain.Project;
import ru.fbtw.navigator.bot_controller.domain.Slot;
import ru.fbtw.navigator.bot_controller.domain.TelegramBot;
import ru.fbtw.navigator.bot_controller.domain.TelegramServer;
import ru.fbtw.navigator.bot_controller.exseption.TelegramBotCreateException;
import ru.fbtw.navigator.bot_controller.exseption.TelegramBotException;
import ru.fbtw.navigator.bot_controller.repository.TelegramBotRepo;
import ru.fbtw.navigator.bot_controller.repository.TelegramServerRepo;
import ru.fbtw.navigator.bot_controller.response.TelegramWebHookResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;


@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Component
public class BotService {
	private static final String WEB_HOOK_URL = "https://api.telegram.org/bot%s/setWebHook?url=%s";

	File botJar;
	String propertiesFormat;

	ProjectService projectService;
	TelegramBotRepo botRepo;
	TelegramServer server;
	TelegramServerRepo serverRepo;

	Map<Long, Process> processMap;

	OkHttpClient client;


	public BotService(
			ProjectService projectService,
			TelegramBotRepo botRepo,
			TelegramServer server,
			TelegramServerRepo serverRepo
	) throws IOException {
		log.info("Creating bot service");
		this.projectService = projectService;
		this.botRepo = botRepo;
		this.server = server;
		this.serverRepo = serverRepo;

		client = new OkHttpClient().newBuilder()
				.build();

		botJar = new File("template/tg_bot.jar");
		//botJar = new File("template/invoke_tester.jar");
		if (!botJar.exists()) {
			throw new FileNotFoundException("Missing parent bot");
		}

		File propertiesFile = new File("template/application.properties.template");
		if (!propertiesFile.exists()) {
			throw new FileNotFoundException("Missing parent bot");
		}

		propertiesFormat = FileUtils.readFileToString(propertiesFile, StandardCharsets.UTF_8);

		processMap = new HashMap<>();
	}

	@SneakyThrows
	public void invokeBot(Project project, String json) {
		Slot slot = nextSlot();
		if (slot != null) {
			copyFiles(project, json, slot);
			startBot(project.getId());
			registerBot(project, slot.getUrl());
			setWebhook(project, slot);
		}
	}

	private void registerBot(Project project, String url) {
		log.info("Register bot  for project id: {}", project.getId());
		TelegramBot telegramBot = new TelegramBot();
		telegramBot.setServer(server);
		telegramBot.setUrl(url);
		telegramBot.setProject(project);

		botRepo.save(telegramBot);
	}

	private void setWebhook(Project project, Slot slot) throws IOException, NullPointerException, TelegramBotCreateException {
		log.info("Setting webhook for project id: {}", project.getId());
		String url = String.format(WEB_HOOK_URL, project.getTelegramApiKey(), slot.getUrl());

		Request request = new Request.Builder()
				.url(url)
				.method("GET", null)
				.build();

		ResponseBody body = client.newCall(request)
				.execute()
				.body();

		TelegramWebHookResponse response = new Gson().fromJson(body.string(), TelegramWebHookResponse.class);

		if (response.isOk() && response.isResult()) {
			log.info("message: {} for project id: {}", response.getDescription(), project.getId());
		} else {
			log.error("message: {} for project id: {}", response.getDescription(), project.getId());
			closePlatform();
			throw new TelegramBotCreateException("Web hook wasn't set");
		}
	}

	private void copyFiles(Project project, String json, Slot slot) throws IOException, TelegramBotException {
		log.info("Copy files for project id: {}", project.getId());
		Long projectId = project.getId();
		File root = new File(projectId.toString());

		if (!root.exists() && root.mkdir()) {

			File app = new File(root.getAbsolutePath() + "/tg_bot.jar");
			//File app = new File(root.getAbsolutePath() + "/invoke_tester.jar");
			FileUtils.copyFile(botJar, app);

			File propertiesFile = new File(root.getAbsolutePath() + "/application.properties");
			FileUtils.write(propertiesFile, getProperties(project, slot), StandardCharsets.UTF_8);

			File envFile = new File(root.getAbsolutePath() + "/default_env.json");
			FileUtils.write(envFile, json, StandardCharsets.UTF_8);

		} else {
			log.error("Directory exist for project{}", projectId);
			closePlatform();
			throw new TelegramBotCreateException("Project folder for this id, exist");
		}
	}

	private String getProperties(Project project, Slot slot) {
		log.info("Writing properties for project{}", project.getId());
		return String.format(propertiesFormat,
				slot.getPort(),
				project.getTelegramName(),
				project.getTelegramApiKey(),
				slot.getUrl()
		);
	}

	private Slot nextSlot() throws NoSuchElementException {
		// get actual slots list in current server
		Iterable<Slot> slots = serverRepo.findById(server.getName())
				.get()
				.getSlots();
		Iterator<Slot> slotIterator = slots.iterator();

		if (slotIterator.hasNext()) {
			Slot slot = slotIterator.next();
			slotIterator.remove();
			return slot;
		} else {
			log.info("Slots are empty");
			throw new NoSuchElementException("Slots are empty");
		}
	}

	private void startBot(Long projectId) throws IOException {
		log.info("Starting bot");
		String pathname = projectId + "/tg_bot.jar";
		ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", pathname);
		Process process = processBuilder.start();

		processMap.put(projectId, process);
	}

	public boolean create(String jsonBody) throws IOException, TelegramBotException {
		Long projectId = getIdFromJson(jsonBody);
		Optional<Project> project = projectService.getProjectById(projectId);
		if (project.isPresent()) {
			invokeBot(project.get(), jsonBody);
		} else {
			throw new TelegramBotCreateException("Missing project with id: " + projectId);
		}
		return false;
	}

	private Long getIdFromJson(String jsonBody) {
		return JsonParser.parseString(jsonBody)
				.getAsJsonObject()
				.get("id")
				.getAsLong();
	}

	public void remove(String json) {
		Long id = getIdFromJson(json);
		// todo: дописать
		Process process = processMap.get(id);
		if (process != null) {
			process.destroy();
		}

		removeFiles(id);
		closePlatform();
	}

	private boolean removeFiles(Long id) {
		File root = new File(id.toString());
		return root.delete();
	}

	private void closePlatform() {
		//todo clear db, statistic & etc
	}
}
