import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GeneticAlgorithm {
    private final Random random = new Random(1);

    private final int populationSize;
    private final int generations;
    private final double crossoverRate;
    private final double mutationRate;
    private final int eliteCount = 3; 

    public GeneticAlgorithm(int populationSize,
                            int generations,
                            double crossoverRate,
                            double mutationRate) {

        this.populationSize = populationSize;
        this.generations = generations;
        this.crossoverRate = crossoverRate;
        this.mutationRate = mutationRate;
    }

    public List<List<Integer>> initializePopulation(int numGroups,
                                                    List<List<Integer>> groups) {

        List<List<Integer>> population = new ArrayList<>();

        for (int i = 0; i < populationSize; i++) {
            List<Integer> chromosome = new ArrayList<>();

            for (int g = 0; g < numGroups; g++) {
                List<Integer> group = groups.get(g);

                if (random.nextDouble() < 0.5) {
                    int selectedFeature = random.nextInt(group.size());
                    chromosome.add(selectedFeature);
                } else {
                    chromosome.add(-1);
                }
            }

            population.add(chromosome);
        }

        return population;
    }

    public List<Integer> decodeChromosome(List<Integer> chromosome,
                                          List<List<Integer>> groups) {

        List<Integer> featureIndices = new ArrayList<>();

        for (int g = 0; g < chromosome.size(); g++) {
            int gene = chromosome.get(g);

            if (gene >= 0 && gene < groups.get(g).size()) {
                featureIndices.add(groups.get(g).get(gene));
            }
        }

        return featureIndices;
    }

    private List<List<Integer>> selectElite(
            List<List<Integer>> population,
            List<Double> fitnessScores) {

        List<Integer> indices = new ArrayList<>();

        for (int i = 0; i < population.size(); i++) {
            indices.add(i);
        }

        indices.sort((a, b) ->
                Double.compare(
                        fitnessScores.get(b),
                        fitnessScores.get(a)
                )
        );

        List<List<Integer>> elites = new ArrayList<>();

        for (int i = 0; i < eliteCount; i++) {
            elites.add(
                    new ArrayList<>(
                            population.get(indices.get(i))
                    )
            );
        }

        return elites;
    }

    private List<Integer> tournamentSelection(
            List<List<Integer>> population,
            List<Double> fitnessScores,
            int tournamentSize) {

        List<Integer> tournament = new ArrayList<>();

        for (int i = 0; i < tournamentSize; i++) {
            int index = random.nextInt(population.size());
            tournament.add(index);
        }

        int bestIndex = tournament.get(0);

        for (int index : tournament) {
            if (fitnessScores.get(index) > fitnessScores.get(bestIndex)) {
                bestIndex = index;
            }
        }

        return new ArrayList<>(population.get(bestIndex));
    }

    private List<List<Integer>> uniformCrossover(
            List<Integer> parent1,
            List<Integer> parent2) {

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

    private void mutate(
            List<Integer> chromosome,
            List<List<Integer>> groups) {

        for (int i = 0; i < chromosome.size(); i++) {

            if (random.nextDouble() < mutationRate) {
                int currentGene = chromosome.get(i);

                if (currentGene == -1) {
                    chromosome.set(
                            i,
                            random.nextInt(groups.get(i).size())
                    );
                } else {
                    if (random.nextBoolean()) {
                        chromosome.set(i, -1);
                    } else {
                        chromosome.set(
                                i,
                                random.nextInt(groups.get(i).size())
                        );
                    }
                }
            }
        }
    }


    public void evolve(
            List<List<Integer>> population,
            List<List<Integer>> groups,
            List<Double> fitnessScores) {
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

    // Getters
    public int getGenerations() {
        return generations;
    }
}