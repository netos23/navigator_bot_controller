package ru.fbtw.navigator.bot_controller.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import javax.persistence.*;

@Entity
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Slot {
	@Id
	String url;
	Integer port;

	@ManyToOne
	TelegramServer server;

	public Slot(){

	}
}
