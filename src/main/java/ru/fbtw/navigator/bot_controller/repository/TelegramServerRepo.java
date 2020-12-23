package ru.fbtw.navigator.bot_controller.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.fbtw.navigator.bot_controller.domain.TelegramServer;


@Repository
public interface TelegramServerRepo extends CrudRepository<TelegramServer, String> {

}
