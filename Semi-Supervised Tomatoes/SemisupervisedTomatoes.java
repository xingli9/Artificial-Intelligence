package assign6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

// Semisupervised Tomatoes:
// EM some Naive Bayes and Markov Models to do sentiment analysis.
// Based on solution code for Assignment 3.
//
// Input from train.tsv.zip at 
// https://www.kaggle.com/c/sentiment-analysis-on-movie-reviews
//
// itself gathered from Rotten Tomatoes.
//
// Format is PhraseID[unused]   SentenceID  Sentence[tokenized]
//
// Just a few sentiment labels this time - this is semisupervised.
//
// We'll only use the first line for each SentenceID, since the others are
// micro-analyzed phrases that would just mess up our counts.
//
// After training, we'll identify the top words for each cluster by
// Pr(cluster | word) - the words that are much more likely in the cluster
// than in the general population - and categorize the new utterances.

public class SemisupervisedTomatoes {

    public static final int CLASSES = 2;
    // Assume sentence numbering starts with this number in the file
    public static final int FIRST_SENTENCE_NUM = 1;
    
    // Probability of either a unigram or bigram that hasn't been seen
    // Gotta make this real generous if we're not using logs
    public static final double OUT_OF_VOCAB_PROB = 0.000001;

    // Words to print per class
    public static final int TOP_N = 10;
    // Times (in expectation) that we need to see a word in a cluster
    // before we think it's meaningful enough to print in the summary
    public static final double MIN_TO_PRINT = 15.0;

    public static boolean USE_UNIFORM_PRIOR = false;
    public static boolean SEMISUPERVISED = true;
    public static boolean FIXED_SEED = true;

    public static final int ITERATIONS = 200;

    // We may play with this in the assignment, but it's good to have common
    // ground to talk about
    public static Random rng = (FIXED_SEED? new Random(2018) : new Random());

    public static NaiveBayesModel nbModel;

    public static class NaiveBayesModel {
        public double[] classCounts;
        public double[] totalWords;
        public ArrayList<HashMap<String, Double>> wordCounts;

        public NaiveBayesModel() {
            classCounts = new double[CLASSES];
            totalWords = new double[CLASSES];
            wordCounts = new ArrayList<HashMap<String, Double>>();
            for (int i = 0; i < CLASSES; i++) {
                wordCounts.add(new HashMap<String, Double>());
            }
        }

        // Update the model given a sentence and its probability of
        // belonging to each class
        void update(String sentence, ArrayList<Double> probs) {
            // TODO
       		String[] words = sentence.split(" ");
        		for (int i = 0; i < CLASSES; i++) {
        			classCounts[i] += probs.get(i);
        			totalWords[i] += probs.get(i) * words.length;
        			for (int j = 0; j < words.length; j++) {
        				String standardized = words[j].toLowerCase();
        				if (wordCounts.get(i) == null || !wordCounts.get(i).containsKey(standardized)) {
        					wordCounts.get(i).put(standardized, probs.get(i));
        				} else {
        					double newProb = wordCounts.get(i).get(standardized) + probs.get(i);
        					wordCounts.get(i).put(standardized, newProb);
        				}
        			}
        		}
        }

        // Classify a new sentence using the data and a Naive Bayes model.
		// Assume every token in the sentence is space-delimited, as the input
		// was. Return a list of class probabilities.
		public ArrayList<Double> classify(String sentence) {
			// TODO (the below is a placeholder to compile)
			ArrayList<Double> result = new ArrayList<Double>();
			String[] words = sentence.split(" ");
			double[] likelihood = new double[CLASSES];
			double totalSentiment = 0;
			for (int i = 0; i < CLASSES; i++) {
				totalSentiment += classCounts[i];
			}
			// System.out.println("sentimentCounts: " + sentimentCounts[0] +
			// "totalSentiment: " + totalSentiment);
			for (int i = 0; i < CLASSES; i++) {
				if (totalSentiment == 0) {
					System.err.println("Warning!! No data!!");
					break;
				}
				if (totalWords[i] == 0) {
					likelihood[i] = Double.MIN_NORMAL;
					continue;
				}
				double pOfC = classCounts[i] / totalSentiment;
				double pOfE = 1.0;
				for (int j = 0; j < words.length; j++) {
					String standardized = words[j].toLowerCase();
					if (wordCounts.get(i).containsKey(standardized)) {
						pOfE *= wordCounts.get(i).get(standardized) / totalWords[i];
					} else {
						pOfE *= OUT_OF_VOCAB_PROB;
					}
				}
				likelihood[i] = (pOfE * pOfC == 0) ? Double.MIN_NORMAL : pOfE * pOfC;
			}
			likelihood[0] = likelihood[0] / (likelihood[0] + likelihood[1]);
			likelihood[1] = 1 - likelihood[0];
			result.add(likelihood[0]);
			result.add(likelihood[1]);
			return result;
		}

        // printTopWords: Print five words with the highest
        // Pr(thisClass | word) = scale Pr(word | thisClass)Pr(thisClass)
        // but skip those that have appeared (in expectation) less than 
        // MIN_TO_PRINT times for this class (to avoid random weird words
        // that only show up once in any sentence)
        void printTopWords(int n) {
            for (int c = 0; c < CLASSES; c++) {
                System.out.println("Cluster " + c + ":");
                ArrayList<WordProb> wordProbs = new ArrayList<WordProb>();
                for (String w : wordCounts.get(c).keySet()) {
                    if (wordCounts.get(c).get(w) >= MIN_TO_PRINT) {
                        // Treating a word as a one-word sentence lets us use
                        // our existing model
                        ArrayList<Double> probs = nbModel.classify(w);
                        wordProbs.add(new WordProb(w, probs.get(c)));
                    }
                }
                Collections.sort(wordProbs);
                for (int i = 0; i < n; i++) {
                    if (i >= wordProbs.size()) {
                        System.out.println("No more words...");
                        break;
                    }
                    System.out.println(wordProbs.get(i).word);
                }
            }
        }
    }

    public static void main(String[] args) {
    		System.out.println("ddd");
        Scanner myScanner = new Scanner(System.in);
        ArrayList<String> sentences = getTrainingData(myScanner);
        trainModels(sentences);
        nbModel.printTopWords(TOP_N);
        classifySentences(myScanner);
    }

    public static ArrayList<String> getTrainingData(Scanner sc) {
        int nextFresh = FIRST_SENTENCE_NUM;
        ArrayList<String> sentences = new ArrayList<String>();
        while(sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.startsWith("---")) {
                return sentences;
            }
            // Data should be filtered now, so just add it
            sentences.add(line);
        }
        return sentences;
    }

    static void trainModels(ArrayList<String> sentences) {
        // We'll start by assigning the sentences to random classes.
        // 1.0 for the random class, 0.0 for everything else
        System.err.println("Initializing models....");
        HashMap<String,ArrayList<Double>> naiveClasses = randomInit(sentences);
        // Initialize the parameters by training as if init were
		// ground truth (essentially starting with M step)
		// TODO
        nbModel = new NaiveBayesModel();
		for (int i = 0; i < ITERATIONS; i++) {
			System.err.println("EM round " + i);
			// TODO: E STEP
			if (i > 0) {
				for (String sent : naiveClasses.keySet()) {
					naiveClasses.put(sent, nbModel.classify(sent));
				}
			}
			
			//clear the data
			for (int j = 0; j < CLASSES; j++) {
				nbModel.classCounts[j] = 0.0;
				nbModel.totalWords[j] = 0.0;
				nbModel.wordCounts.get(j).clear();
			}
			// TODO: M STEP
			for (String sent : naiveClasses.keySet()) {
				nbModel.update(sent, naiveClasses.get(sent));
			}
		}
	}

    static HashMap<String,ArrayList<Double>> randomInit(ArrayList<String> sents) {
        HashMap<String,ArrayList<Double>> counts = new HashMap<String,ArrayList<Double>>();
        for (String sent : sents) {
            ArrayList<Double> probs = new ArrayList<Double>();
            if (SEMISUPERVISED && sent.startsWith(":)")) {
                // Class 1 = positive
                probs.add(0.0);
                probs.add(1.0);
                for (int i = 2; i < CLASSES; i++) {
                    probs.add(0.0);
                }
                // Shave off emoticon
                sent = sent.substring(3);
            } else if (SEMISUPERVISED && sent.startsWith(":(")) {
                // Class 0 = negative
                probs.add(1.0);
                probs.add(0.0);
                for (int i = 2; i < CLASSES; i++) {
                    probs.add(0.0);
                }
                // Shave off emoticon
                sent = sent.substring(3);
            } else {
                double baseline = 1.0/CLASSES;
                // Slight deviation to break symmetry
                int randomBumpedClass = rng.nextInt(CLASSES);
                double bump = (1.0/CLASSES * 0.25);// Symmetry breaking
                if (SEMISUPERVISED) {
                    // Symmetry breaking not necessary, already got it
                    // from labeled examples
                    bump = 0.0;
                }
                for (int i = 0; i < CLASSES; i++) {
                    if (i == randomBumpedClass) {
                        probs.add(baseline + bump);
                    } else {
                        probs.add(baseline - bump/(CLASSES-1));
                    }
                }
            }
            counts.put(sent, probs);
        }
        return counts;
    }

    public static class WordProb implements Comparable<WordProb> {
        public String word;
        public Double prob;

        public WordProb(String w, Double p) {
            word = w;
            prob = p;
        }

        public int compareTo(WordProb wp) {
            // Reverse order
            if (this.prob > wp.prob) {
                return -1;
            } else if (this.prob < wp.prob) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public static void classifySentences(Scanner scan) {
        while(scan.hasNextLine()) {
            String line = scan.nextLine();
            System.out.print(line + ":");
            ArrayList<Double> probs = nbModel.classify(line);
            for (int c = 0; c < CLASSES; c++) {
                System.out.print(probs.get(c) + " ");
            }
            System.out.println();
        }
    }

}
