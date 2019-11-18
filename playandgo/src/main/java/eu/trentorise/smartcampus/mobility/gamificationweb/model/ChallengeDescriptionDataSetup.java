package eu.trentorise.smartcampus.mobility.gamificationweb.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

@Component
public class ChallengeDescriptionDataSetup {

	@Autowired
	@Value("${challengesDir}")
	private String challengesDir;
	
	@PostConstruct
	public void init() throws IOException {
		Yaml yaml = new Yaml(new Constructor(ChallengeDescriptionDataSetup.class));
		ChallengeDescriptionDataSetup data = (ChallengeDescriptionDataSetup) yaml.load(new FileInputStream(getChallengeResourceURL("chall-conf.yml")));
		this.descriptions = data.descriptions;
	}

	private List<ChallengeDescriptionData> descriptions;
	private Map<String,ChallengeDescriptionData> descriptionsMap;

	public List<ChallengeDescriptionData> getDescriptions() {
		return descriptions;
	}

	public void setDescriptions(List<ChallengeDescriptionData> descriptions) {
		this.descriptions = descriptions;
	}
	
	@Override
	public String toString() {
		return "ChallengeDescriptionDataSetup [descriptions=" + descriptions + "]";
	}
	
	public ChallengeDescriptionData findChallDescByID(String id){
		if (descriptionsMap == null) {
			descriptionsMap = new HashMap<String, ChallengeDescriptionData>();
			for (ChallengeDescriptionData challdesc : descriptions) {
				descriptionsMap.put(challdesc.getId(), challdesc);
			}
		}
		return descriptionsMap.get(id);
	}

	public File getChallengeResourceURL(String file) throws MalformedURLException {
		return new File(challengesDir + (challengesDir.endsWith("/") ? "" : "/")+ file);
	}

}
