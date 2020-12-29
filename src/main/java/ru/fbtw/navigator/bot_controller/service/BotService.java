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
import ru.fbtw.navigator.bot_controller.domain.*;
import ru.fbtw.navigator.bot_controller.exseption.TelegramBotCreateException;
import ru.fbtw.navigator.bot_controller.exseption.TelegramBotException;
import ru.fbtw.navigator.bot_controller.repository.SlotsRepo;
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
	private static final String DEL_URL = "https://api.telegram.org/bot%s/deleteWebhook?drop_pending_updates=True";

	File botTemplates;
	String propertiesFormat;

	ProjectService projectService;
	TelegramBotRepo botRepo;
	TelegramServer server;
	SlotsRepo slotsRepo;
	TelegramServerRepo serverRepo;

	Map<Long, Process> processMap;

	OkHttpClient client;


	public BotService(
			ProjectService projectService,
			TelegramBotRepo botRepo,
			TelegramServer server,
			SlotsRepo slotsRepo,
			TelegramServerRepo serverRepo
	) throws IOException {
		this.slotsRepo = slotsRepo;
		log.info("Creating bot service");
		this.projectService = projectService;
		this.botRepo = botRepo;
		this.server = server;
		this.serverRepo = serverRepo;

		client = new OkHttpClient().newBuilder()
				.build();

		botTemplates = new File("template");
		//botJar = new File("template/invoke_tester.jar");
		if (!botTemplates.exists()) {
			throw new FileNotFoundException("Missing parent bot");
		}

		File propertiesFile = new File("application.properties.template");
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
			registerBot(project,slot);
			slotsRepo.delete(slot);
			setWebhook(project, slot);
		}
	}

	private void registerBot(Project project, Slot slot) {
		log.info("Register bot  for project id: {}", project.getId());
		projectService.activatePlatform(project,Platform.TG_BOT);

		TelegramBot telegramBot = new TelegramBot();
		telegramBot.setServer(server);
		telegramBot.setUrl(slot.getUrl());
		telegramBot.setPort(slot.getPort());
		telegramBot.setProject(project);

		botRepo.save(telegramBot);

	}

	private void setWebhook(Project project, Slot slot) throws IOException, NullPointerException, TelegramBotCreateException {
		log.info("Setting webhook for project id: {}", project.getId());
		String url = String.format(WEB_HOOK_URL, project.getTelegramApiKey(), slot.getUrl());

		ResponseBody body = getResponseBody(url);

		TelegramWebHookResponse response = new Gson().fromJson(body.string(), TelegramWebHookResponse.class);

		if (response.isOk() && response.isResult()) {
			log.info("message: {} for project id: {}", response.getDescription(), project.getId());

		} else {
			log.error("message: {} for project id: {}", response.getDescription(), project.getId());
			remove(project.getId());
			throw new TelegramBotCreateException("Web hook wasn't set");
		}
	}

	private void copyFiles(Project project, String json, Slot slot) throws IOException, TelegramBotException {
		log.info("Copy files for project id: {}", project.getId());
		Long projectId = project.getId();
		File root = new File(projectId.toString());

		if (!root.exists() && root.mkdir()) {
			File[] src = Objects.requireNonNull(botTemplates.listFiles());
			for (File file : src) {
				File app = new File(root.getAbsolutePath() + "/" + file.getName());
				FileUtils.copyFile(file, app);
			}

			File propertiesFile = new File(root.getAbsolutePath() + "/application.properties");
			FileUtils.write(propertiesFile, getProperties(project, slot), StandardCharsets.UTF_8);

			File envFile = new File(root.getAbsolutePath() + "/default_env.json");
			FileUtils.write(envFile, json, StandardCharsets.UTF_8);

		} else {
			log.error("Directory exist for project{}", projectId);
			removeFiles(projectId);
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
		Iterable<Slot> slots = slotsRepo.findAllByServer(server);
		Iterator<Slot> slotIterator;

		if (slots != null && (slotIterator = slots.iterator()).hasNext()) {
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
		File dir = new File(projectId.toString());
		ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", "tg_bot.jar")
				.directory(dir)
				.redirectOutput(new File("logs/"+projectId.toString() + "app.log"));
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


	public void remove(String json) throws IOException, TelegramBotCreateException {
		Long id = getIdFromJson(json);
		// todo: дописать
		remove(id);
	}

	private void remove(Long id) throws IOException, TelegramBotCreateException {
		Process process = processMap.get(id);
		if (process != null) {
			process.destroy();
		}

		removeFiles(id);
		closePlatform(id);
	}

	private void removeFiles(Long id) throws IOException {
		log.info("Removing files for id: {}", id);
		File root = new File(id.toString());
		for(File file : root.listFiles()){
			FileUtils.forceDelete(file);
		}
		FileUtils.forceDelete(root);
	}

	private void closePlatform(Long id) throws IOException, TelegramBotCreateException, NoSuchElementException {
		log.info("Closing platform for id: {}", id);
		Project project = projectService.getProjectById(id).get();

		try {

			TelegramBot bot = botRepo.findByProjectId(id);
			botRepo.delete(bot);

			Slot slot = new Slot();
			slot.setServer(server);
			slot.setUrl(bot.getUrl());
			slot.setPort(bot.getPort());
			slotsRepo.save(slot);

			projectService.removePlatform(project, Platform.TG_BOT);

			removeWebHook(project);
		}catch (Exception ex){
			log.error("Error while deleting");
			ex.printStackTrace();
		}
	}

	private void removeWebHook(Project project) throws IOException, TelegramBotCreateException {
		log.info("Deleting webhook for project id: {}", project.getId());
		String url = String.format(DEL_URL, project.getTelegramApiKey());

		ResponseBody body = getResponseBody(url);

		TelegramWebHookResponse response = new Gson().fromJson(body.string(), TelegramWebHookResponse.class);

		if (response.isOk() && response.isResult()) {
			log.info("message: {} for project id: {}", response.getDescription(), project.getId());

		} else {
			log.error("message: {} for project id: {}", response.getDescription(), project.getId());
			throw new TelegramBotCreateException("Web hook wasn't deleted");
		}
	}

	private ResponseBody getResponseBody(String url) throws IOException {
		Request request = new Request.Builder()
				.url(url)
				.method("GET", null)
				.build();

		return client.newCall(request)
				.execute()
				.body();
	}
}
