package eu.trentorise.smartcampus.mobility.gamification.challenges;

import java.util.HashMap;
import java.util.Map;


public class SingleModeConfig {

    private String modeName;
    private int weight;
    private Double prizeMatrixMin;
    private Double prizeMatrixMax;
    private Double prizeMatrixIntermediate;

    // used to increment or decrement the prize for all modes
    private double power = 1.2;

    public SingleModeConfig(String modeName, int weight, Double prizeMatrixMin,
                            Double prizeMatrixMax, Double prizeMatrixIntermediate) {
        this.modeName = modeName;
        this.weight = weight;
        this.prizeMatrixMin = prizeMatrixMin * power;
        this.prizeMatrixMax = prizeMatrixMax * power;
        this.prizeMatrixIntermediate = prizeMatrixIntermediate * power;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public Double getPrizeMatrixMin() {
        return prizeMatrixMin;
    }

    public void setPrizeMatrixMin(Double prizeMatrixMin) {
        this.prizeMatrixMin = prizeMatrixMin;
    }

    public Double getPrizeMatrixMax() {
        return prizeMatrixMax;
    }

    public void setPrizeMatrixMax(Double prizeMatrixMax) {
        this.prizeMatrixMax = prizeMatrixMax;
    }

    public Double getPrizeMatrixIntermediate() {
        return prizeMatrixIntermediate;
    }

    public void setPrizeMatrixIntermediate(Double prizeMatrixIntermediate) {
        this.prizeMatrixIntermediate = prizeMatrixIntermediate;
    }

    public String getModeName() {
        return modeName;
    }

    public void setModeName(String modeName) {
        this.modeName = modeName;
    }

    @Override
    public String toString() {
        // return this class fields as a map
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("modeName", modeName);
        result.put("weight", weight);
        result.put("prizeMatrixMin", prizeMatrixMin);
        result.put("prizeMatrixMax", prizeMatrixMax);
        result.put("prizeMatrixIntermediate", prizeMatrixIntermediate);
        return result.toString();
    }
}
