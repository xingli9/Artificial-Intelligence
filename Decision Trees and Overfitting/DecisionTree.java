package assignment4;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

// An assignment on decision trees, using the "Adult" dataset from
// the UCI Machine Learning Repository.  The dataset predicts
// whether someone makes over $50K a year from their census data.
//
// Input data is a comma-separated values (CSV) file where the
// target classification is given a label of "Target."
// The other headers in the file are the feature names.
//
// Features are assumed to be strings, with comparison for equality
// against one of the values as a decision, unless the value can
// be parsed as a double, in which case the decisions are < comparisons
// against the values seen in the data.

public class DecisionTree {

	public Feature feature; // if true, follow the yes branch
	public boolean decision; // for leaves
	public DecisionTree yesBranch;
	public DecisionTree noBranch;

	public static double CHI_THRESH = 3.84; // chi-square test critical value
	public static double EPSILON = 0.00000001; // for determining whether vals roughly equal
	public static boolean PRUNE = false; // prune with chi-square test or not
	public static boolean[] isAdded;

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		// Keep header line around for interpreting decision trees
		String header = scanner.nextLine();
		Feature.featureNames = header.split(",");
		System.err.println("Reading training examples...");
		ArrayList<Example> trainExamples = readExamples(scanner);
		// We'll assume a delimiter of "---" separates train and test as before
		DecisionTree tree = new DecisionTree(trainExamples);
		if (PRUNE) {
			tree.pruning(trainExamples);
		}
		System.out.println(tree);
		System.out.println("Training data results: ");
		System.out.println(tree.classify(trainExamples));
		System.err.println("Reading test examples...");
		ArrayList<Example> testExamples = readExamples(scanner);
		Results results = tree.classify(testExamples);
		System.out.println("Test data results: ");
		System.out.print(results);
		
		
		
		//System.out.println("hood" + "\n" + "dd");
		
	}

	public static ArrayList<Example> readExamples(Scanner scanner) {
		ArrayList<Example> examples = new ArrayList<Example>();
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (line.startsWith("---") || line == null) {
				break;
			}
			// Skip missing data lines
			if (!line.contains("?")) {
				Example newExample = new Example(line);
				examples.add(newExample);
			}
		}
		return examples;
	}

	public static class Example {
		// Not all features will use both arrays. The Feature.isNumerical static
		// array will determine whether the numericals array can be used. If not,
		// the strings array will be used. The indices correspond to the columns
		// of the input, and thus the different features. "target" is special
		// as it gives the desired classification of the example.
		public String[] strings; // Use only if isNumerical[i] is false
		public double[] numericals; // Use only if isNumerical[i] is true
		boolean target;

		// Construct an example from a CSV input line
		public Example(String dataline) {
			// Assume a basic CSV with no double-quotes to handle real commas
			strings = dataline.split(",");
			// We'll maintain a separate array with everything that we can
			// put into numerical form, in numerical form.
			// No real need to distinguish doubles from ints.
			numericals = new double[strings.length];
			if (Feature.isNumerical == null) {
				// First data line; we're determining types
				Feature.isNumerical = new boolean[strings.length];
				for (int i = 0; i < strings.length; i++) {
					if (Feature.featureNames[i].equals("Target")) {
						target = strings[i].equals("1");
					} else {
						try {
							numericals[i] = Double.parseDouble(strings[i]);
							Feature.isNumerical[i] = true;
						} catch (NumberFormatException e) {
							Feature.isNumerical[i] = false;
							// string stays where it is, in strings
						}
					}
				}
			} else {
				for (int i = 0; i < strings.length; i++) {
					if (i >= Feature.isNumerical.length) {
						System.err.println("Too long line: " + dataline);
					} else if (Feature.featureNames[i].equals("Target")) {
						target = strings[i].equals("1");
					} else if (Feature.isNumerical[i]) {
						try {
							numericals[i] = Double.parseDouble(strings[i]);
						} catch (NumberFormatException e) {
							Feature.isNumerical[i] = false;
							// string stays where it is
						}
					}
				}
			}
		}

		// Possibly of help in debugging: a way to print examples
		public String toString() {
			String out = "";
			for (int i = 0; i < Feature.featureNames.length; i++) {
				out += Feature.featureNames[i] + "=" + strings[i] + ";";
			}
			return out;
		}
	}

	public static class Feature {
		// Which feature are we talking about? Can index into Feature.featureNames
		// to get name of the feature, or into strings and numericals arrays of example
		// to get feature value
		public int featureNum;
		// WLOG assume numerical features are "less than"
		// and String features are "equal to"
		public String svalue; // the string value to compare a string feature against
		public double dvalue; // the numerical threshold to compare a numerical feature against
		public static String[] featureNames; // extracted from the header
		public static boolean[] isNumerical = null; // need to read a line to see the size

		public Feature(int featureNum, String value) {
			this.featureNum = featureNum;
			this.svalue = value;
		}

		public Feature(int featureNum, double value) {
			this.featureNum = featureNum;
			this.dvalue = value;
		}

		// Ask whether the answer is "yes" or "no" to the question implied by this
		// feature
		// when applied to a particular example
		public boolean apply(Example e) {
			if (Feature.isNumerical[featureNum]) {
				return (e.numericals[featureNum] < dvalue);
			} else {
				return (e.strings[featureNum].equals(svalue));
			}
		}

		// It's suggested that when you generate a collection of potential features, you
		// use a HashSet to avoid duplication of features. The equality and hashCode
		// operators
		// that follow can help you with this.
		public boolean equals(Object o) {
			if (!(o instanceof Feature)) {
				return false;
			}
			Feature otherFeature = (Feature) o;
			if (featureNum != otherFeature.featureNum) {
				return false;
			} else if (Feature.isNumerical[featureNum]) {
				if (Math.abs(dvalue - otherFeature.dvalue) < EPSILON) {
					return true;
				}
				return false;
			} else {
				if (svalue.equals(otherFeature.svalue)) {
					return true;
				}
				return false;
			}
			
		}

		
		public int hashCode() {
			
			return (featureNum + (svalue == null ? 0 : svalue.hashCode()) + (int) (dvalue * 10000));
		}

		// Print feature's check; called when printing decision trees
		public String toString() {
			if (Feature.isNumerical[featureNum]) {
				return Feature.featureNames[featureNum] + " < " + dvalue;
			} else {
				return Feature.featureNames[featureNum] + " = " + svalue;
			}
		}

	}

	// This constructor should create the whole decision tree recursively.
	DecisionTree(ArrayList<Example> examples) {
		// TODO your code here
		if (examples == null || examples.isEmpty()) {
			return;
		}
		
		double hValue = entropy(examples);
		if (hValue == 0) {
			decision = examples.get(0).target;
			feature = null;
			return;
		} else if (hValue > 0) {
			decision = true;
		} else {
			decision = false;
		}
		
		
		if (isAdded == null) {
			isAdded = new boolean[Feature.featureNames.length];
		} else {
			boolean isLeaf = true;
			for (int i = 0; i < isAdded.length; i++) {
				if (!isAdded[i]) {
					isLeaf = false;
					break;
				}	
			}
			if (isLeaf) {
				if (hValue > 0) {
					decision = true;
				} else {
					decision = false;
				}
				return;
			}
		}
		
		double bestH = 1.0;//Math.abs(hValue);
		Feature bestFeature = null;
		ArrayList<Example> bestTrueExamples = null;
		ArrayList<Example> bestFalseExamples = null;
		
		HashSet<Integer> set = new HashSet<Integer>();
		
			for (int i = 0; i < Feature.featureNames.length; i++) {
				if (Feature.featureNames[i].equals("Target") || (isAdded != null && isAdded[i] == true)) {
					continue;
				}

				for (int j = 0; j < examples.size(); j++) {
					Feature newFeature;
					if (Feature.isNumerical[i]) {
						newFeature = new Feature(i, examples.get(j).numericals[i]);
					} else {
						newFeature = new Feature(i, examples.get(j).strings[i]);
					}
					if (set.contains(newFeature.hashCode())) {
						continue;
					}
					set.add(newFeature.hashCode());
					ArrayList<Example> trueExamples = new ArrayList<Example>();
					ArrayList<Example> falseExamples = new ArrayList<Example>();

					for (int k = 0; k < examples.size(); k++) {
						if (newFeature.apply(examples.get(k))) {
							trueExamples.add(examples.get(k));
						} else {
							falseExamples.add(examples.get(k));
						}
					}
					double hYes = Math.abs(entropy(trueExamples));
					double hNo = Math.abs(entropy(falseExamples));
					if (hYes < 0 || hNo < 0) {
						continue;
					}
					double hExpected = hYes * trueExamples.size() / examples.size()
							+ hNo * falseExamples.size() / examples.size();

					if (hExpected < bestH) {
						bestH = hExpected;
						bestFeature = newFeature;
						bestTrueExamples = trueExamples;
						bestFalseExamples = falseExamples;
					}
				}

			}
			

		
		
		
		if (bestFeature != null) {
			isAdded[bestFeature.featureNum] = true;

			feature = bestFeature;
			// System.out.println(bestH + " " + hValue);

			yesBranch = new DecisionTree(bestTrueExamples);
			noBranch = new DecisionTree(bestFalseExamples);
		}
		
		
		
		
	}

	private double entropy(ArrayList<Example> examples) {
		if (examples == null) {
			return -1.0;
		}
		int trueNum = 0;
		int falseNum = 0;
		for (int i = 0; i < examples.size(); i++) {
			if (examples.get(i).target) {
				trueNum++;
			} else {
				falseNum++;
			}
		}
		double hTrue = 0;
		double hFalse = 0;
		//System.out.println(trueNum + "   " + falseNum);
		double total = trueNum + falseNum +0.0;
		
		if (trueNum == 0 || falseNum == 0) {
			hTrue = 0;
			
		} else {
			hTrue = - (trueNum / total) * Math.log(trueNum / total) / Math.log(2.0);
			hFalse = - (falseNum / total) * Math.log(falseNum / total) / Math.log(2.0);
		}
		
		//System.out.println(hTrue + "   " + hFalse);
		//System.out.println("--------------------------   ");
		double result = hTrue + hFalse;
		if (trueNum >= falseNum) {
			return result;
		} else {
			return -result;
		}
	}
	
	public void pruning(ArrayList<Example> examples) {
		if (feature == null || yesBranch == null && noBranch == null) {
			return;
		} else if (yesBranch == null && noBranch != null) {
			feature = null;
			noBranch = null;
			return;
		} else if (yesBranch != null && noBranch == null) {
			feature = null;
			yesBranch = null;
			return;
		}
		
		ArrayList<Example> trueExamples = new ArrayList<Example>();
		ArrayList<Example> falseExamples = new ArrayList<Example>();
		for (int k = 0; k < examples.size(); k++) {
			if (feature.apply(examples.get(k))) {
				trueExamples.add(examples.get(k));
			} else {
				falseExamples.add(examples.get(k));
			}
		}
		
		if (yesBranch != null) {
			yesBranch.pruning(trueExamples);
		}
		if (noBranch != null) {
			noBranch.pruning(trueExamples);
		}
		double distance = chiSquare(trueExamples, falseExamples);
		if ( distance <= CHI_THRESH) {
			feature = null;
			yesBranch = null;
			noBranch = null;
				 //System.out.println( " distance:---------  " + distance);
		}
	}
	
	private double chiSquare(ArrayList<Example> example1, ArrayList<Example> example2) {
		int p1 = 0;
		int n1 = 0;
		int p2 = 0;
		int n2 = 0;
		if (example1 == null || example2 == null) {
			return 0;
		}
		for (int i = 0; i < example1.size(); i++) {
			if (example1.get(i).target) {
				p1++;
				
			} else {
				n1++;
			}
		}
		for (int i = 0; i < example2.size(); i++) {
			if (example2.get(i).target) {
				p2++;
			} else {
				n2++;
			}
		}
		double total = p1 + n1 + p2 + n2 + 0.0;
		double pe1 = (p1 + p2) * (p1 + n1) / total;
		double ne1 = (n1 + n2) * (p1 + n1) / total;
		double pe2 = (p1 + p2) * (p2 + n2) / total;
		double ne2 = (n1 + n2) * (p2 + n2) / total;
		
		double result = (p1 - pe1) * (p1 - pe1) / pe1 + (n1 - ne1) * (n1 - ne1) / ne1 
				+ (p2 - pe2) * (p2 - pe2) / pe2 + (n2 - ne2) * (n2 - ne2) / ne2;
		return result;
	}

	public static class Results {
		public int true_positive; // correctly classified "yes"
		public int true_negative; // correctly classified "no"
		public int false_positive; // incorrectly classified "yes," should be "no"
		public int false_negative; // incorrectly classified "no", should be "yes"

		public Results() {
			true_positive = 0;
			true_negative = 0;
			false_positive = 0;
			false_negative = 0;
		}

		public String toString() {
			String out = "Precision: ";
			out += String.format("%.4f", true_positive / (double) (true_positive + false_positive));
			out += "\nRecall: " + String.format("%.4f", true_positive / (double) (true_positive + false_negative));
			out += "\n";
			out += "Accuracy: ";
			out += String.format("%.4f", (true_positive + true_negative)
					/ (double) (true_positive + true_negative + false_positive + false_negative));
			out += "\n";
			return out;
		}
	}

	public Results classify(ArrayList<Example> examples) {
		Results results = new Results();
		// TODO your code here, classifying each example with the tree and comparing to
		// the truth to populate the results structure
		if (examples == null) {
			return results;
		}
		for (int i = 0; i < examples.size(); i++) {
			boolean result = calculate(this, examples.get(i));
			if (examples.get(i).target) {
				if (result) {
					results.true_positive++;
				} else {
					results.false_negative++;
				}
			} else {
				if (result) {
					results.false_positive++;
				} else {
					results.true_negative++;
				}
			}
			
		}
		return results;
	}
	
	private boolean calculate(DecisionTree tree, Example example) {
		boolean result;
		if (tree.feature == null) {
			return tree.decision;
		}
		if (tree.feature.apply(example)) {
			result = calculate(tree.yesBranch, example);
		} else {
			result = calculate(tree.noBranch, example);
		}
		
		return result;
		
	}

	public String toString() {
		return toString(0);
	}

	// Print the decision tree as a set of nested if/else statements.
	// This is a little easier than trying to print with the root at the top.
	public String toString(int depth) {
		String out = "";
		for (int i = 0; i < depth; i++) {
			out += "    ";
		}
		if (feature == null) {
			out += (decision ? "YES" : "NO");
			out += "\n";
			return out;
		}
		out += "if " + feature + "\n";
		
		//System.out.println(depth);
		
		out += yesBranch.toString(depth + 1);
		for (int i = 0; i < depth; i++) {
			out += "    ";
		}
		out += "else\n";
		out += noBranch.toString(depth + 1);
		return out;
	}

}
