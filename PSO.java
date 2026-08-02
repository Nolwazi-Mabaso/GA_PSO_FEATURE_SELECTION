import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/*
 * Particle Swarm Optimization, mirroring GeneticAlgorithm's structure so
 * it can be dropped into the same Main loop with minimal changes.
 *
 * Same chromosome/gene encoding as the GA:
 *   -1     = do not select a feature from this group
 *    0..n  = index of the selected feature within the group
 *
 * P-S-O is naturally continuous (real-valued position + velocity), so
 * each particle's position is a double[] internally. When it's time to
 * evaluate fitness or report a result, the position gets rounded and
 * clamped to the nearest valid gene value, same encoding as the GA,
 * just reached a different way.
 */
public class PSO {

    private final Random random = new Random(1);

    private final int swarmSize;
    private final int iterations;
    private final double inertiaWeight;
    private final double cognitiveWeight; // pulls particle toward its own best
    private final double socialWeight;    // pulls particle toward the swarm's best

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

    /*
     * A single particle: its current position/velocity, plus the best
     * position IT has personally found so far (pBest).
     */
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

    /*
     * Creates the initial swarm. Each particle's position has one
     * dimension per Pearson feature group, same as a GA chromosome's
     * gene count. Each dimension is randomly placed between -1 (not
     * selected) and (groupSize - 1) (last valid feature index).
     */
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
                velocity[g] = 0; // start with no motion
            }

            swarm.add(new Particle(position, velocity));
        }

        return swarm;
    }

    /*
     * Converts a particle's continuous position into the actual feature
     * indices used by KNN, same job as GeneticAlgorithm.decodeChromosome.
     */
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

    // Rounds a continuous position value to the nearest valid gene,
    // then clamps it inside [-1, groupSize - 1].
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

    /*
     * Updates every particle's velocity and position for one iteration,
     * pulling each particle toward its own best-known position (pBest)
     * and the swarm's best-known position (gBest). This is the P-S-O
     * equivalent of GeneticAlgorithm.evolve().
     */
    public void updateSwarm(List<Particle> swarm, List<List<Integer>> groups, double[] gBestPosition) {

        for (Particle particle : swarm) {

            for (int g = 0; g < particle.position.length; g++) {

                double r1 = random.nextDouble();
                double r2 = random.nextDouble();

                double cognitive = cognitiveWeight * r1 * (particle.pBestPosition[g] - particle.position[g]);
                double social = socialWeight * r2 * (gBestPosition[g] - particle.position[g]);

                particle.velocity[g] = (inertiaWeight * particle.velocity[g]) + cognitive + social;
                particle.position[g] += particle.velocity[g];

                // Clamp position back inside valid bounds for this group,
                // otherwise particles can drift off into meaningless values.
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

    /*
     * Same reporting format as GeneticAlgorithm.printResults, so console
     * output is directly comparable between the two algorithms.
     */
    public void printResults(double[] bestPosition, List<Integer> bestFeatureSubset) {
        System.out.println("\n======================================");
        System.out.println("BEST FEATURE SUBSET (PSO)");
        System.out.println("======================================");
        System.out.println("Encoded Position (rounded):");

        List<Integer> roundedPosition = new ArrayList<>();
        for (double value : bestPosition) {
            roundedPosition.add((int) Math.round(value));
        }
        System.out.println(roundedPosition);

        System.out.println("\nSelected Feature Indices:");
        System.out.println(bestFeatureSubset);
        System.out.println("Total Selected Features: " + bestFeatureSubset.size());
        System.out.println("======================================");
    }
}