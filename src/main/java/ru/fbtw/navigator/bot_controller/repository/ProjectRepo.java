package ru.fbtw.navigator.bot_controller.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.fbtw.navigator.bot_controller.domain.Project;


@Repository
public interface ProjectRepo extends CrudRepository<Project, Long> {
}
