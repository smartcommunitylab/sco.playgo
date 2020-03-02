package eu.trentorise.smartcampus.mobility.storage;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import eu.trentorise.smartcampus.mobility.gamification.model.PlayerTeam;

@Repository
public interface PlayerTeamRepository extends CrudRepository<PlayerTeam, String>{
	
	public List<PlayerTeam> findByGameIdAndInitiativeAndOwner(String gameId, String initiative, String owner);
	public List<PlayerTeam> findByGameIdAndInitiative(String gameId, String initiative);

}
