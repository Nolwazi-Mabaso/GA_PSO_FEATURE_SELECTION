import java.util.*;

public class GARunner {

    public List<Integer> runGA(
            int numGenerations,
            List<List<Integer>> groups,
            double[][] trainData,
            int[] trainLabels,
            double[][] valData,
            int[] valLabels,
            FitnessFunction fitnessFunction,
            GeneticAlgorithm ga) {

        List<List<Integer>> population = ga.initializePopulation(groups.size(), groups);

        List<Integer> globalBestChromosome = null;
        double globalBestFitness = -1.0;

        System.out.println("Starting Evolutionary Optimization (" + numGenerations + " Generations)...\n");
        for (int gen = 0; gen < numGenerations; gen++) {
            List<Double> fitnessScores = new ArrayList<>();
            
            double currentGenBestFitness = -1.0;
            List<Integer> currentGenBestChromosome = null;

            for (List<Integer> chromosome : population) {
                List<Integer> decoded = ga.decodeChromosome(chromosome, groups);

                double fitness;
                if (decoded.isEmpty()) {
                    fitness = 0.0; 
                } else {
                    fitness = fitnessFunction.calculateFitness(
                            trainData, trainLabels, valData, valLabels, decoded
                    );
                }

                fitnessScores.add(fitness);

                if (fitness > currentGenBestFitness) {
                    currentGenBestFitness = fitness;
                    currentGenBestChromosome = chromosome;
                }
            }
            if (currentGenBestFitness > globalBestFitness) {
                globalBestFitness = currentGenBestFitness;
                globalBestChromosome = new ArrayList<>(currentGenBestChromosome);
            }

            System.out.printf("Generation %3d | Best Fitness: %.4f | Global Best: %.4f%n", 
                    gen + 1, currentGenBestFitness, globalBestFitness);

            if (gen < numGenerations - 1) {
                ga.evolve(population, groups, fitnessScores);
            }
        }

        System.out.println("\nOptimization Complete.");
        System.out.printf("Best Overall Fitness Achieved: %.4f%n", globalBestFitness);
        
        return ga.decodeChromosome(globalBestChromosome, groups);
    }
}