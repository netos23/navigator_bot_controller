package ru.fbtw.navigator.bot_controller.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TelegramWebHookResponse {
	boolean ok;
	boolean result;
	String description;

	public TelegramWebHookResponse() {
	}
}
