package ru.fbtw.navigator.bot_controller.repository;

import org.springframework.data.repository.CrudRepository;
import ru.fbtw.navigator.bot_controller.domain.Slot;
import ru.fbtw.navigator.bot_controller.domain.TelegramServer;

public interface SlotsRepo extends CrudRepository<Slot,String> {
	Iterable<Slot> findAllByServer(TelegramServer server);
}
