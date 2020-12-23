package ru.fbtw.navigator.bot_controller.config;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.fbtw.navigator.bot_controller.domain.Slot;
import ru.fbtw.navigator.bot_controller.domain.TelegramServer;
import ru.fbtw.navigator.bot_controller.repository.SlotsRepo;
import ru.fbtw.navigator.bot_controller.repository.TelegramServerRepo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Configuration
@ConfigurationProperties(prefix = "telegram.controller")
public class AppConfig {
	String url;
	String name;

	@Bean
	public TelegramServer telegramServer(
			TelegramServerRepo telegramServerRepo,
			SlotsRepo slotsRepo
	) throws FileNotFoundException {
		TelegramServer currentServer = new TelegramServer();
		currentServer.setName(name);
		currentServer.setUrl(url);

		List<Slot> slots = getSlots(currentServer);
		currentServer.setEmptySlotsCount(slots.size());

		telegramServerRepo.save(currentServer);
		slotsRepo.saveAll(slots);
		return currentServer;
	}


	private List<Slot> getSlots(TelegramServer server) throws FileNotFoundException {
		List<Slot> slots = new ArrayList<>();
		Gson gson = new Gson();
		File slotsFile = new File("slots.json");
		Reader reader = new FileReader(slotsFile);

		JsonArray slotsArray = JsonParser.parseReader(reader)
				.getAsJsonObject()
				.get("slots")
				.getAsJsonArray();

		for(JsonElement element : slotsArray){
			Slot slot = gson.fromJson(element, Slot.class);
			slot.setServer(server);
			slots.add(slot);
		}
		return slots;
	}
}
