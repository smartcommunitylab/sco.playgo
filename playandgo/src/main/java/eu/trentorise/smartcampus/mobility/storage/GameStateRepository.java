package eu.trentorise.smartcampus.mobility.storage;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import eu.trentorise.smartcampus.mobility.security.GameState;

@Repository
public interface GameStateRepository extends CrudRepository<GameState, String>{
}
