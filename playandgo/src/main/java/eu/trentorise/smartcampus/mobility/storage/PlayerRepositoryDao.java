package eu.trentorise.smartcampus.mobility.storage;

import java.util.List;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;

@Repository
public interface PlayerRepositoryDao extends CrudRepository<Player, String>{
	
	/*public Player findByPid(String id);
	
	public Player findBySocialId(String id);

	@Query("{'nickName': ?0}")
	public Player findByNick(String nickname);
	
	@Query("{'nickName': { '$regex': ?0, $options:'i'}}")
	public Player findByNickIgnoreCase(String nickname);*/
	
	List<Player> findAllByGameId(String gameId);

	Player findByGameIdAndMail(String gameId, String mail);

	@Query(value="{'gameId' : ?0}", fields="{nickname: 1}")
	List<Player> findNicknames(String gameId);
	
	Iterable<Player> findAllByCheckedRecommendationAndGameId(boolean recommendation, String gameId);
	
	public Player findByPlayerIdAndGameId(String id, String gameId);

//	@Query("{'nickname': ?0}")
	public Player findByNicknameAndGameId(String nickname, String appId);
	
	@Query("{'nickname': { '$regex': ?0, $options:'i'}, 'gameId' : ?1}")
	public Player findByNicknameIgnoreCaseAndGameId(String nickname, String gameId);

//	@Query ("{'personalData.nick_recommandation': ?0, 'gameId' : ?1}")
//	public List<Player> findByNicknameRecommendationAndGameId(String nickname, String gameId);

	@Query ("{'personalData.nick_recommandation': { '$regex': ?0, $options:'i'}, 'gameId' : ?1}")
	public List<Player> findByNicknameRecommendationIgnoreCaseAndGameId(String nickname, String gameId);	
	
}
