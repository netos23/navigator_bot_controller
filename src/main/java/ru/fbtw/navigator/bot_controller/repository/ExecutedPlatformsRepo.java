package ru.fbtw.navigator.bot_controller.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.fbtw.navigator.bot_controller.domain.ExecutedProject;
import ru.fbtw.navigator.bot_controller.domain.Platform;
import ru.fbtw.navigator.bot_controller.domain.Project;

import java.util.Optional;
import java.util.Set;

@Repository
public interface ExecutedPlatformsRepo extends CrudRepository<ExecutedProject, Long> {
	Set<ExecutedProject> findAllByProject(Project project);


	Optional<ExecutedProject> findByProjectAndPlatforms(Project project, Platform platform);

	boolean removeByProjectAndPlatforms(Project project, Platform platform);

	boolean removeById(Long id);
}
