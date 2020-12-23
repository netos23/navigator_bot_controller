package ru.fbtw.navigator.bot_controller.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.fbtw.navigator.bot_controller.domain.Project;
import ru.fbtw.navigator.bot_controller.domain.TelegramBot;

@Repository
public interface TelegramBotRepo extends CrudRepository<TelegramBot,Long> {
	TelegramBot findByProject(Project project);
}
