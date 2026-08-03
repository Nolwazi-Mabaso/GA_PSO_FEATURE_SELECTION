import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PSO {

    private final Random random = new Random(1);

    private final int swarmSize;
    private final int iterations;
    private final double inertiaWeight;
    private final double cognitiveWeight; 
    private final double socialWeight;   

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

    public int getIterations() {
        return iterations;
    }

    public static class Particle {
        double[] position;
        double[] velocity;
        double[] pBestPosition;
        double pBestFitness;

        Particle(double[] position, double[] velocity) {
            this.position = position;
            this.velocity = velocity;
            this.pBestPosition = position.clone();
            this.pBestFitness = -1;
        }
    }


    public List<Particle> initializeSwarm(int numGroups, List<List<Integer>> groups) {

        List<Particle> swarm = new ArrayList<>();

        for (int p = 0; p < swarmSize; p++) {

            double[] position = new double[numGroups];
            double[] velocity = new double[numGroups];

            for (int g = 0; g < numGroups; g++) {
                int groupSize = groups.get(g).size();

                double lowerBound = -1;
                double upperBound = groupSize - 1;

                position[g] = lowerBound + random.nextDouble() * (upperBound - lowerBound);
                velocity[g] = 0; 
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
            rounded = -1;
        }
        if (rounded > groupSize - 1) {
            rounded = groupSize - 1;
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
                double lowerBound = -1;
                double upperBound = groupSize - 1;

                if (particle.position[g] < lowerBound) {
                    particle.position[g] = lowerBound;
                    particle.velocity[g] = 0;
                }
                if (particle.position[g] > upperBound) {
                    particle.position[g] = upperBound;
                    particle.velocity[g] = 0;
                }
            }
        }
    }

    public void printResults(double[] bestPosition, List<Integer> bestFeatureSubset) {
        System.out.println("\n");
        System.out.println("BEST FEATURE SUBSET (PSO)");
        System.out.println("_______________________________________");
        System.out.println("Encoded Position (rounded):");

        List<Integer> roundedPosition = new ArrayList<>();
        for (double value : bestPosition) {
            roundedPosition.add((int) Math.round(value));
        }
        System.out.println(roundedPosition);

        System.out.println("\nSelected Feature Indices:");
        System.out.println(bestFeatureSubset);
        System.out.println("Total Selected Features: " + bestFeatureSubset.size());
        System.out.println("\n");
    }
}