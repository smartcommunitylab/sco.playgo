package eu.trentorise.smartcampus.mobility.storage;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import eu.trentorise.smartcampus.mobility.gamificationweb.model.Avatar;

@Repository
public interface AvatarRepository extends CrudRepository<Avatar, String>{
	
	public Avatar findByPlayerIdAndGameId(String playerId, String gameId);

}
