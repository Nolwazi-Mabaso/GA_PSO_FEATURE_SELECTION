import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class PSO {

    private final Random random = new Random(); 

    private final int swarmSize;
    private final int iterations;
    private final double inertiaWeight;
    private final double cognitiveWeight; 
    private final double socialWeight;

    private double[] lastBestPosition = null;
    private int repeats = 0;
    private final int maxRepeats = 15;
    private String reason = null;

    public PSO(int swarmSize,
               int iterations,
               double inertiaWeight,
               double cognitiveWeight,
               double socialWeight) {
        this.swarmSize = swarmSize;
        this.iterations = iterations;
        this.inertiaWeight = inertiaWeight;
        this.cognitiveWeight = cognitiveWeight;
        this.socialWeight = socialWeight;
    }

    public int getIterations() { return iterations; }
    public int getSwarmSize() { return swarmSize; }
    public double getInertiaWeight() { return inertiaWeight; }
    public double getCognitiveWeight() { return cognitiveWeight; }
    public double getSocialWeight() { return socialWeight; }
    public String getStopReason() { return reason; }
    public int getRepeatCount() { return repeats; }
    public int getRepeatLimit() { return maxRepeats; }

    public boolean hasConverged(double[] currentGBestPosition) {
        if (currentGBestPosition == null) {
            return false;
        }

        if (lastBestPosition != null && Arrays.equals(lastBestPosition, currentGBestPosition)) {
            repeats++;
        } else {
            repeats = 0;
        }

        lastBestPosition = currentGBestPosition.clone();

        if (repeats >= maxRepeats) {
            reason = "No improvement in global best position for " + maxRepeats + " consecutive iterations.";
            return true;
        }

        return false;
    }

    public static class Particle {
        public double[] position;
        public double[] velocity;
        public double[] pBestPosition;
        public double pBestFitness;

        Particle(double[] position, double[] velocity) {
            this.position = position;
            this.velocity = velocity;
            this.pBestPosition = position.clone();
            this.pBestFitness = -1.0;
        }
    }

    public List<Particle> initializeSwarm(int numGroups, List<List<Integer>> groups) {
        List<Particle> swarm = new ArrayList<>();

        for (int p = 0; p < swarmSize; p++) {
            double[] position = new double[numGroups];
            double[] velocity = new double[numGroups];

            for (int g = 0; g < numGroups; g++) {
                int groupSize = groups.get(g).size();
                double lowerBound = -1.0;
                double upperBound = groupSize - 1.0;

                position[g] = lowerBound + random.nextDouble() * (upperBound - lowerBound);
                velocity[g] = 0.0;
            }

            swarm.add(new Particle(position, velocity));
        }

        return swarm;
    }

    public List<Integer> decodeParticle(double[] position, List<List<Integer>> groups) {
        List<Integer> featureIndices = new ArrayList<>();

        for (int g = 0; g < position.length; g++) {
            int groupSize = groups.get(g).size();
            int gene = roundAndClamp(position[g], groupSize);

            if (gene >= 0 && gene < groupSize) {
                featureIndices.add(groups.get(g).get(gene));
            }
        }

        return featureIndices;
    }

    private int roundAndClamp(double value, int groupSize) {
        int rounded = (int) Math.round(value);

        if (rounded < -1) {
            return -1;
        }
        if (rounded > groupSize - 1) {
            return groupSize - 1;
        }

        return rounded;
    }

    public void updateSwarm(List<Particle> swarm, List<List<Integer>> groups, double[] gBestPosition) {
        for (Particle particle : swarm) {
            for (int g = 0; g < particle.position.length; g++) {

                double r1 = random.nextDouble();
                double r2 = random.nextDouble();

                double cognitive = cognitiveWeight * r1 * (particle.pBestPosition[g] - particle.position[g]);
                double social = socialWeight * r2 * (gBestPosition[g] - particle.position[g]);

                particle.velocity[g] = (inertiaWeight * particle.velocity[g]) + cognitive + social;
                particle.position[g] += particle.velocity[g];

                int groupSize = groups.get(g).size();
                double lowerBound = -1.0;
                double upperBound = groupSize - 1.0;

                if (particle.position[g] < lowerBound) {
                    particle.position[g] = lowerBound;
                    particle.velocity[g] = 0.0;
                }
                if (particle.position[g] > upperBound) {
                    particle.position[g] = upperBound;
                    particle.velocity[g] = 0.0;
                }
            }
        }
    }

    public void printResults(double[] bestPosition, List<Integer> bestFeatureSubset) {
        System.out.println("\n_______________________________________");
        System.out.println("BEST FEATURE SUBSET (PSO)");
        System.out.println("_______________________________________");

        if (bestPosition == null || bestFeatureSubset == null) {
            System.out.println("No valid global best solution found.");
            return;
        }

        List<Integer> roundedPosition = new ArrayList<>();
        for (double value : bestPosition) {
            roundedPosition.add((int) Math.round(value));
        }

        System.out.println("Encoded Position (rounded): " + roundedPosition);
        System.out.println("Selected Feature Indices:   " + bestFeatureSubset);
        System.out.println("Total Selected Features:    " + bestFeatureSubset.size());
        System.out.println();
    }
}