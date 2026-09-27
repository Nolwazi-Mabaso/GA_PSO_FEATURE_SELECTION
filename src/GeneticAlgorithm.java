import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GeneticAlgorithm {
    private final Random random = new Random();

    private final int populationSize;
    private final int generations;
    private final double crossoverRate;
    private final double mutationRate;
    private final int eliteCount = 3; 

    private List<Integer> lastBestChromosome = null;
    private int repeats = 0;
    private final int maxRepeats = 15;
    private String reason = null;

    public GeneticAlgorithm(int populationSize,
                            int generations,
                            double crossoverRate,
                            double mutationRate) {

        this.populationSize = populationSize;
        this.generations = generations;
        this.crossoverRate = crossoverRate;
        this.mutationRate = mutationRate;
    }

    public List<List<Integer>> initializePopulation(int numGroups, List<List<Integer>> groups) {
        List<List<Integer>> population = new ArrayList<>();

        for (int i = 0; i < populationSize; i++) {
            List<Integer> chromosome = new ArrayList<>();
            for (int g = 0; g < numGroups; g++) {
                List<Integer> group = groups.get(g);
                if (random.nextDouble() < 0.5) {
                    chromosome.add(random.nextInt(group.size()));
                } else {
                    chromosome.add(-1);
                }
            }
            population.add(chromosome);
        }
        return population;
    }

    public List<Integer> decodeChromosome(List<Integer> chromosome, List<List<Integer>> groups) {
        List<Integer> featureIndices = new ArrayList<>();
        for (int g = 0; g < chromosome.size(); g++) {
            int gene = chromosome.get(g);
            if (gene >= 0 && gene < groups.get(g).size()) {
                featureIndices.add(groups.get(g).get(gene));
            }
        }
        return featureIndices;
    }

    private List<List<Integer>> selectElite(List<List<Integer>> population, List<Double> fitnessScores) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < population.size(); i++) {
            indices.add(i);
        }

        indices.sort((a, b) -> Double.compare(fitnessScores.get(b), fitnessScores.get(a)));

        List<List<Integer>> elites = new ArrayList<>();
        for (int i = 0; i < Math.min(eliteCount, population.size()); i++) {
            elites.add(new ArrayList<>(population.get(indices.get(i))));
        }
        return elites;
    }

    private List<Integer> tournamentSelection(List<List<Integer>> population, List<Double> fitnessScores, int tournamentSize) {
        int bestIndex = random.nextInt(population.size());

        for (int i = 1; i < tournamentSize; i++) {
            int competitorIndex = random.nextInt(population.size());
            if (fitnessScores.get(competitorIndex) > fitnessScores.get(bestIndex)) {
                bestIndex = competitorIndex;
            }
        }
        return new ArrayList<>(population.get(bestIndex));
    }

    private List<List<Integer>> uniformCrossover(List<Integer> parent1, List<Integer> parent2) {
        List<Integer> child1 = new ArrayList<>();
        List<Integer> child2 = new ArrayList<>();

        if (random.nextDouble() < crossoverRate) {
            for (int i = 0; i < parent1.size(); i++) {
                if (random.nextBoolean()) {
                    child1.add(parent1.get(i));
                    child2.add(parent2.get(i));
                } else {
                    child1.add(parent2.get(i));
                    child2.add(parent1.get(i));
                }
            }
        } else {
            child1 = new ArrayList<>(parent1);
            child2 = new ArrayList<>(parent2);
        }

        List<List<Integer>> children = new ArrayList<>();
        children.add(child1);
        children.add(child2);
        return children;
    }

    private void mutate(List<Integer> chromosome, List<List<Integer>> groups) {
        for (int i = 0; i < chromosome.size(); i++) {
            if (random.nextDouble() < mutationRate) {
                int currentGene = chromosome.get(i);
                if (currentGene == -1) {
                    chromosome.set(i, random.nextInt(groups.get(i).size()));
                } else {
                    if (random.nextBoolean()) {
                        chromosome.set(i, -1);
                    } else {
                        chromosome.set(i, random.nextInt(groups.get(i).size()));
                    }
                }
            }
        }
    }

    public void evolve(List<List<Integer>> population, List<List<Integer>> groups, List<Double> fitnessScores) {
        List<List<Integer>> newPopulation = selectElite(population, fitnessScores);

        while (newPopulation.size() < populationSize) {
            List<Integer> parent1 = tournamentSelection(population, fitnessScores, 3);
            List<Integer> parent2 = tournamentSelection(population, fitnessScores, 3);
            List<List<Integer>> children = uniformCrossover(parent1, parent2);

            mutate(children.get(0), groups);
            mutate(children.get(1), groups);

            newPopulation.add(children.get(0));
            if (newPopulation.size() < populationSize) {
                newPopulation.add(children.get(1));
            }
        }

        population.clear();
        population.addAll(newPopulation);
    }

    public boolean hasConverged(List<Integer> currentBestChromosome) {
        if (currentBestChromosome == null) return false;

        if (lastBestChromosome != null && lastBestChromosome.equals(currentBestChromosome)) {
            repeats++;
        } else {
            repeats = 0;
        }

        lastBestChromosome = new ArrayList<>(currentBestChromosome);

        if (repeats >= maxRepeats) {
            reason = "No improvement in best solution for " + maxRepeats + " consecutive generations.";
            return true;
        }
        return false;
    }

     public void printResults(List<Integer> bestChromosome, List<Integer> bestFeatureSubset) {

        System.out.println("\n");

        System.out.println("BEST FEATURE SUBSET");

        System.out.println("_______________________________________");

        System.out.println("Encoded Chromosome:");

        System.out.println(bestChromosome);

        System.out.println("\nSelected Feature Indices:");

        System.out.println(bestFeatureSubset);

        System.out.println("Total Selected Features: " + bestFeatureSubset.size());

        System.out.println("___________________________________________");

    }
    public String getStopReason() { return reason; }
    public int getGenerations() { return generations; }
    public int getPopulationSize() { return populationSize; }
    public double getCrossoverRate() { return crossoverRate; }
    public double getMutationRate() { return mutationRate; }
    public int getEliteCount() { return eliteCount; }
}