package eu.trentorise.smartcampus.mobility.gamification.challenges;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Recommendation system mode configuration
 */
public class ChallengesModeConfiguration {

    private Map<String, SingleModeConfig> configuration = new HashMap<String, SingleModeConfig>();

    public void put(String modelName, SingleModeConfig modeConfig) {
        this.configuration.put(modelName, modeConfig);
    }

    public SingleModeConfig get(String modelName) {
        return configuration.get(modelName);
    }

    public Set<String> getModeKeySet() {
        return configuration.keySet();
    }

    @Override
    public String toString() {
        return configuration.toString();
    }
}
