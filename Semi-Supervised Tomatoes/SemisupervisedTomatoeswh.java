package assign6;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.Map;

// Semi true
// a terrible movie I would not wish on anyone:0.9994985803307004 5.014196692995866E-4
// this is a delightful , joyous romp:0.1610566930960881 0.8389433069039118

// Semi false
// a terrible movie I would not wish on anyone:0.014465891502236452 0.9855341084977636
// this is a delightful , joyous romp:0.986459956456441 0.013540043543558904

// Semi false, fixed seed false
// a terrible movie I would not wish on anyone:0.1209031901646889 0.8790968098353111
// this is a delightful , joyous romp:0.2038801209027949 0.796119879097205

// a terrible movie I would not wish on anyone:0.581976682843917 0.4180233171560829
// this is a delightful , joyous romp:0.3111284198284027 0.6888715801715972

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

public class SemisupervisedTomatoeswh {

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
    // public static boolean SEMISUPERVISED = true;
    // public static boolean FIXED_SEED = true;
    public static boolean SEMISUPERVISED = false;
    public static boolean FIXED_SEED = false;

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
            // convert to lower case 
            sentence = sentence.toLowerCase();

            // split sentence into words
            String[] words = sentence.split(" ");

            // For example, if my probability vector is [0.3, 0.7], and I see
            // the sentence “I like fun,” 
            for (int i = 0; i < probs.size(); i ++) {
                // then classCounts[0] will increase by 0.3, 
                classCounts[i] += probs.get(i);
                // totalWords[0] will increase by 0.3 * 3, 
                totalWords[i] += probs.get(i) * words.length;
                // wordCounts[0][“I”] should increase by 0.3, and so on; while
                // classCounts[1] will increase by 0.7, and so on for class 1.
                for (int j = 0; j < words.length; j ++) {
                    HashMap<String, Double> temp = wordCounts.get(i);
                    if (temp.containsKey(words[j])) {
                        temp.put(words[j], temp.get(words[j]) + probs.get(i));
                        wordCounts.set(i, temp);
                    } else {
                        temp.put(words[j], probs.get(i));
                        wordCounts.set(i, temp);
                    }
                }
            }
            // System.out.println("sent: " + sentence + " probs: " + probs.toString());
        }

        // Classify a new sentence using the data and a Naive Bayes model.
        // Assume every token in the sentence is space-delimited, as the input
        // was.  Return a list of class probabilities.
        public ArrayList<Double> classify(String sentence) {
            
            ArrayList<Double> res = new ArrayList<Double>();
            // convert to lower case 
            sentence = sentence.toLowerCase();

            // split sentence into words
            String[] words = sentence.split(" ");

            int resClass = -1;
            double resProb = Double.NEGATIVE_INFINITY;
            double totalProb = 0;

            // calculating prior probability
            double classSum = 0;
            for (double c : classCounts) {
                classSum += c;
            }

            for (int i = 0; i < CLASSES; i ++) {
                double sentiProb = classCounts[i] / classSum;

                double wordProbSum = 1;

                HashMap<String, Double> wordMap = wordCounts.get(i);
                double demoinator = totalWords[i];

                for (String word: words) {
                    // if current word is in current sentiment hashmap
                    if (wordMap.containsKey(word)) {
                        double numerator = (wordMap.get(word) == 0) ? Double.MIN_NORMAL : wordMap.get(word);
                        double temp = numerator / demoinator;
                        
                        wordProbSum *= temp;
                    } else {
                        wordProbSum *= OUT_OF_VOCAB_PROB;
                    }
                }

                double temp = (sentiProb * wordProbSum == 0) ? Double.MIN_NORMAL : sentiProb * wordProbSum;
                res.add(temp);
                totalProb += temp;
            }

            // normalize the result
            for (int i = 0; i < res.size(); i ++) {
                double r = res.get(i);
                r /= totalProb;
                res.set(i, r);
            }

            return res;
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
        nbModel = new NaiveBayesModel();

        for (String sent : sentences) {
            if (SEMISUPERVISED && (sent.startsWith(":(") || sent.startsWith(":)"))) {
                // Shave off emoticon
                sent = sent.substring(3);
            }

            ArrayList<Double> probs = naiveClasses.get(sent);
            nbModel.update(sent, probs);
        }

        // for (int i = 0; i < CLASSES; i++) {
        //     System.out.println("                   class " + i);
        //     HashMap<String, Double> temp = nbModel.wordCounts.get(i);
        //     for (Map.Entry<String, Double> entry :  temp.entrySet()) {
        //         String sent = entry.getKey();
        //         Double probs = entry.getValue();
        //         System.out.println("sent: " + sent + " probs: " + probs);
        //     }
        // }

        // TODO
        for (int i = 0; i < ITERATIONS; i++) {
            System.err.println("EM round " + i);

            // TODO:  E STEP
            for (String sent : sentences) {
                if (SEMISUPERVISED && (sent.startsWith(":(") || sent.startsWith(":)"))) {
                    // Shave off emoticon
                    sent = sent.substring(3);
                }

                ArrayList<Double> probs = nbModel.classify(sent);
                naiveClasses.put(sent, probs);
            }

            nbModel = new NaiveBayesModel();

            // TODO:  M STEP
            for (String sent : sentences) {
                if (SEMISUPERVISED && (sent.startsWith(":(") || sent.startsWith(":)"))) {
                    // Shave off emoticon
                    sent = sent.substring(3);
                }

                ArrayList<Double> probs = naiveClasses.get(sent);
                // System.out.println("probs: " + probs);
                nbModel.update(sent, probs);
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
                double bump = (1.0/CLASSES * 0.25);
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
