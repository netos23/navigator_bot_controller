package ru.fbtw.navigator.bot_controller.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.fbtw.navigator.bot_controller.domain.User;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {
    User findUserByUsername(String username);

    User findUserByEmail(String email);
}
