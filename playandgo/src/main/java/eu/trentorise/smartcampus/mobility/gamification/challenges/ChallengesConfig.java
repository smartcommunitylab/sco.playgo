package eu.trentorise.smartcampus.mobility.gamification.challenges;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;


/**
 * Challenges configuration main class
 */

@Component
public class ChallengesConfig {


    /**
     * --------------------------------------
     * default configuration
     * --------------------------------------
     */

    // Increase in the prize of the challenges
    public static double competitiveChallengesBooster = 1.1;

    public static String gLeaves = "green leaves";

    // default modes
    private static final String NO_CAR_TRIPS = "NoCar_Trips";
    public static final String BIKE_KM = "Bike_Km";
    private static final String BUS_KM = "Bus_Km";
    private static final String BIKE_SHARING_TRIPS = "BikeSharing_Trips";
    private static final String WALK_TRIPS = "Walk_Trips";
    private static final String BIKE_TRIPS = "Bike_Trips";
    private static final String TRAIN_KM = "Train_Km";
    public static final String BUS_TRIPS = "Bus_Trips";
    public static final String TRAIN_TRIPS = "Train_Trips";
    private static final String ZERO_IMPACT_TRIPS = "ZeroImpact_Trips";
    private static final String BIKE_SHARING_KM = "BikeSharing_Km";
    public static final String WALK_KM = "Walk_Km";
    public static final String GREEN_LEAVES = "green leaves";

    // Default prize matrix dimension, number of rows
    public static final int PRIZE_MATRIX_NROW = 4;

    // Default prize matrix dimension, number of columns
    public static final int PRIZE_MATRIX_NCOL = 10;

    // Default prize matrix coordinate for try once, number of row
    public static final int PRIZE_MATRIX_TRY_ONCE_ROW_INDEX = 1;

    // Default prize matrix coordinate for try once, number of column
    public static final int PRIZE_MATRIX_TRY_ONCE_COL_INDEX = 9;

    // Prize matrix approximator value
    public static final Double PRIZE_MATRIX_APPROXIMATOR = 10.0;

    /**
     * --------------------------------------
     * challenge generation
     * --------------------------------------
     */

    public static final double booster = 1.3;
    public static final int week_n = 5;    
    
    /**
     * --------------------------------------
     * dynamic configuration
     * --------------------------------------
     */

    // Enable default users filtering
    private static boolean userFiltering = false;

    // Enable select top 2 challenges
    private static final boolean selectTopTwo = true;

    // Transportation mode configuration
    // First, declare call supported modes. Order matters!
    public static String[] defaultMode = {WALK_KM, BUS_TRIPS, BIKE_KM, TRAIN_TRIPS, GREEN_LEAVES};
    // Second, declare corresponding *_Trips of *_Km modes (i.e. Walk_km =>
    // Walk_Trips), used for try once challenges
    private static final String[] defaultModetrip = {BIKE_TRIPS, WALK_TRIPS,
            BUS_TRIPS, TRAIN_TRIPS, BIKE_SHARING_TRIPS};
    // "Walk_Trips", "Bike_Trips", "BikeSharing_Trips"
    // defining different improvement percentage 10%,20%, etc.
    private static final Double[] percentage = {0.1, 0.2, 0.3, 0.4};

    // default user player id
    private static List<String> playerIds;

    // recommendation system configuration
    private static ChallengesModeConfiguration modeConfiguration;

    // TODO fare che si aggiorni in automatico
    private static String challengeNamePrefix = "w%d_rs_";

    protected String[] levelNames = new String[] {"GreenStarter", "GreenFollower", "GreenLover", "GreenInfluencer", "GreenSoldier", "GreenMaster", "GreenAmbassador", "GreenWarrior", "GreenVeteran", "GreenGuru", "GreenGod"};

    public ChallengesConfig() {
        init();
    }

    // init recommendation system configuration
    private static void init() {

        modeConfiguration = new ChallengesModeConfiguration();

        modeConfiguration.put(BIKE_KM, new SingleModeConfig(BIKE_KM, 8, 100.0,
                250.0, 175.0));
        modeConfiguration.put(WALK_KM, new SingleModeConfig(WALK_KM, 10, 120.0,
                250.0, 175.0));

        modeConfiguration.put(BUS_TRIPS, new SingleModeConfig(BUS_TRIPS, 10,
                100.0, 250.0, 175.0));
        modeConfiguration.put(TRAIN_TRIPS, new SingleModeConfig(TRAIN_TRIPS,
                10, 100.0, 250.0, 175.0));

        // TODO CHECK
        modeConfiguration.put(GREEN_LEAVES, new SingleModeConfig(
                GREEN_LEAVES, 0, 100.0, 250.0, 175.0));

        // OLD

        modeConfiguration.put(BIKE_SHARING_TRIPS, new SingleModeConfig(
                BIKE_SHARING_TRIPS, 10, 220.0, 380.0, 300.0));

        modeConfiguration.put(TRAIN_KM, new SingleModeConfig(TRAIN_KM, 0,
                150.0, 350.0, 240.0));
        modeConfiguration.put(BUS_KM, new SingleModeConfig(BUS_KM, 0, 150.0,
                350.0, 240.0));
        modeConfiguration.put(NO_CAR_TRIPS, new SingleModeConfig(NO_CAR_TRIPS,
                0, 100.0, 250.0, 150.0));
        modeConfiguration.put(BIKE_SHARING_KM, new SingleModeConfig(
                BIKE_SHARING_KM, 0, 200.0, 300.0, 250.0));
        modeConfiguration.put(WALK_TRIPS, new SingleModeConfig(WALK_TRIPS, 0,
                150.0, 250.0, 200.0));
        modeConfiguration.put(BIKE_TRIPS, new SingleModeConfig(BIKE_TRIPS, 0,
                220.0, 380.0, 300.0));
        modeConfiguration.put(ZERO_IMPACT_TRIPS, new SingleModeConfig(
                ZERO_IMPACT_TRIPS, 0, 200.0, 380.0, 280.0));


        // Arrays.sort(defaultMode);

    }


    public static Integer getWeight(String key) {
        // TODO weight currently disabled
        // return modeConfiguration.get(key).getWeight();
        return 10;
    }

    public static List<String> getPlayerIds() {
        return playerIds;
    }

    public Set<String> getModeKeySet() {
        return modeConfiguration.getModeKeySet();
    }

    public static SingleModeConfig getModeConfig(String mode) {
        if (null == modeConfiguration) {
            init();
        }
        return modeConfiguration.get(mode);
    }

    public static boolean isUserfiltering() {
        return userFiltering;
    }

    public boolean isSelecttoptwo() {
        return selectTopTwo;
    }

    public ChallengesModeConfiguration getModeConfiguration() {
        return modeConfiguration;
    }

    public static String[] getDefaultMode() {
        return defaultMode;
    }

    public static String[] getDefaultModetrip() {
        return defaultModetrip;
    }

    public static Double[] getPercentage() {
        return percentage;
    }

    /**
     * @param mode
     * @return true if input mode is a default mode trip
     */
    public static boolean isDefaultMode(String mode) {
        if (mode == null) {
            return false;
        }
        for (int i = 0; i < getDefaultModetrip().length; i++) {
            String m = getDefaultModetrip()[i];
            if (m.toLowerCase().equals(mode.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static String getChallengeNamePrefix() {
        return challengeNamePrefix;
    }
    
    public static double roundTarget(String mode, double improvementValue) {
        if (mode.endsWith("_Trips")) {
            improvementValue = Math.ceil(improvementValue);
        } else {
            if (improvementValue > 1000)
                improvementValue = Math.ceil(improvementValue / 100) * 100;
            else if (improvementValue > 100)
                improvementValue = Math.ceil(improvementValue / 10) * 10;
            else
                improvementValue = Math.ceil(improvementValue);
        }
        return improvementValue;
    }    

}
