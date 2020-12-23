package ru.fbtw.navigator.bot_controller.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.fbtw.navigator.bot_controller.exseption.TelegramBotException;
import ru.fbtw.navigator.bot_controller.service.BotService;

import java.io.IOException;

@Slf4j
@RestController
public class ApiController {

	private final BotService botService;

	public ApiController(BotService botService) {
		this.botService = botService;
	}

	@GetMapping("/ping")
	public String ping() {
		return "{\"status\" : \"ok\"}";
	}

	@PostMapping("/create")
	public String create(@RequestBody String jsonBody){
		try {
			if(botService.create(jsonBody)){
				return "{\"status\" : \"ok\"}";
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TelegramBotException e) {
			e.printStackTrace();
		}

		return "{\"status\" : \"err\"}";
	}


}
