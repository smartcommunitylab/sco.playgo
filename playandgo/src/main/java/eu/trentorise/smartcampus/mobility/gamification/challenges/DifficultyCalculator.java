package eu.trentorise.smartcampus.mobility.gamification.challenges;

import java.util.HashMap;
import java.util.Map;

public class DifficultyCalculator {

    public final static Integer EASY = 1;
    public final static Integer MEDIUM = 2;
    public final static Integer HARD = 3;
    public final static Integer VERY_HARD = 4;

    // Prize Matrix for each mode
    private Map<String, PlanePointFunction> prizeMatrixMap = new HashMap<>();

    public DifficultyCalculator() {
        for (String mode : ChallengesConfig.defaultMode) {
            SingleModeConfig config = ChallengesConfig.getModeConfig(mode);

            PlanePointFunction matrix = new PlanePointFunction(
                    ChallengesConfig.PRIZE_MATRIX_NROW,
                    ChallengesConfig.PRIZE_MATRIX_NCOL, config.getPrizeMatrixMin(),
                    config.getPrizeMatrixMax(), config.getPrizeMatrixIntermediate(),
                    ChallengesConfig.PRIZE_MATRIX_APPROXIMATOR);
            prizeMatrixMap.put(mode, matrix);
        }
    }

    /**
     * compute difficulties valye for given input
     */
    public static Integer computeDifficulty(Map<Integer, Double> quartiles, Double baseline, Double target) {

        if (quartiles == null || baseline == null || target == null) {
            throw new IllegalArgumentException("All input must be not null");
        }
        if (quartiles.get(4) == null || quartiles.get(7) == null
                || quartiles.get(9) == null) {
            throw new IllegalArgumentException(
                    "Quartiles that must be defined: 4,7,9");
        }
        double virtualValue = quartiles.get(9) - quartiles.get(7);

        double values[] = new double[6];
        values[0] = quartiles.get(4);
        values[1] = quartiles.get(7);
        values[2] = quartiles.get(9);
        values[3] = quartiles.get(9) + virtualValue;
        values[4] = quartiles.get(9) + virtualValue * 2;
        values[5] = quartiles.get(9) + virtualValue * 3;

        Integer zone = computeZone(values, baseline);
        Integer targetZone = computeZone(values, target);

        int diffZone = targetZone - zone;
        if (diffZone <= 0) {
            return EASY;
        }
        if (diffZone == 1) {
            return MEDIUM;
        }
        if (diffZone == 2) {
            return HARD;
        }
        return VERY_HARD;
    }

    private static Integer computeZone(double[] values, Double baseline) {
        if (baseline <= values[0]) {
            return 1;
        } else if (baseline > values[0] && baseline <= values[1]) {
            return 2;
        } else if (baseline > values[1] && baseline <= values[2]) {
            return 3;
        } else if (baseline > values[2] && baseline <= values[3]) {
            return 4;
        } else if (baseline > values[3] && baseline <= values[4]) {
            return 5;
        } else if (baseline > values[4] && baseline <= values[5]) {
            return 6;
        } else if (baseline > values[5]) {
            return 7;
        }
        return null;
    }


    public int calculatePrize(Integer difficulty, double percent, String modeName) {

        int y;
        if (percent <= 0.1) {
            y = 0;
        } else if (percent <= 0.2) {
            y = 1;
        } else if (percent <= 0.3) {
            y = 2;
        } else if (percent <= 0.4) {
            y = 4;
        } else { // if (percent <= 1) {
            y = 9;
        }

        return (int) Math.ceil(prizeMatrixMap.get(modeName).get(difficulty - 1, y));
    }

    public int getTryOnceBonus(String counterName) {
        return (int) Math.ceil(prizeMatrixMap.get(counterName).getTryOncePrize(
                ChallengesConfig.PRIZE_MATRIX_TRY_ONCE_ROW_INDEX,
                ChallengesConfig.PRIZE_MATRIX_TRY_ONCE_COL_INDEX));
    }
}
