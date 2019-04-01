package assign5;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class Solution {

	public static final double GOLD_REWARD = 100.0;
	public static final double PIT_REWARD = -150.0;
	public static final double DISCOUNT_FACTOR = 0.5;
	public static final double EXPLORE_PROB = 0.2; // for Q-learning
	public static final double LEARNING_RATE = 0.1;
	public static final int ITERATIONS = 10000;
	public static final int MAX_MOVES = 1000;

	// Using a fixed random seed so that the behavior is a little
	// more reproducible across runs & students
	public static Random rng = new Random(2018);

	public static void main(String[] args) {
		//System.out.println("begain");
		Scanner myScanner = new Scanner(System.in);
		Problem problem = new Problem(myScanner);
		Policy policy = problem.solve(ITERATIONS);
		if (policy == null) {
			System.err.println("No policy.  Invalid solution approach?");
		} else {
			System.out.println(policy);
		}
		if (args.length > 0 && args[0].equals("eval")) {
			//System.out.println("Average utility per move: " + tryPolicy(policy, problem));
		}
	}

	public static class Problem {
		public String approach;
		public double[] moveProbs;
		public ArrayList<ArrayList<String>> map;

		// Format looks like
		// MDP [approach to be used]
		// 0.7 0.2 0.1 [probability of going 1, 2, 3 spaces]
		// - - - - - - P - - - - [space-delimited map rows]
		// - - G - - - - - P - - [G is gold, P is pit]
		//
		// You can assume the maps are rectangular, although this isn't enforced
		// by this constructor.

		Problem(Scanner sc) {
			approach = sc.nextLine();
			String probsString = sc.nextLine();
			String[] probsStrings = probsString.split(" ");
			moveProbs = new double[probsStrings.length];
			for (int i = 0; i < probsStrings.length; i++) {
				try {
					moveProbs[i] = Double.parseDouble(probsStrings[i]);
				} catch (NumberFormatException e) {
					break;
				}
			}
			
			map = new ArrayList<ArrayList<String>>();
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				String[] squares = line.split(" ");
				ArrayList<String> row = new ArrayList<String>(Arrays.asList(squares));
				map.add(row);
			}
		}

		Policy solve(int iterations) {
			if (approach.equals("MDP")) {
				MDPSolver mdp = new MDPSolver(this);
				return mdp.solve(this, iterations);
			} else if (approach.equals("Q")) {
				QLearner q = new QLearner(this);
				return q.solve(this, iterations);
			}
			return null;
		}

	}

	public static class Policy {
		public String[][] bestActions;

		public Policy(Problem prob) {
			bestActions = new String[prob.map.size()][prob.map.get(0).size()];
		}

		public String toString() {
			String out = "";
			for (int r = 0; r < bestActions.length; r++) {
				for (int c = 0; c < bestActions[0].length; c++) {
					if (c != 0) {
						out += " ";
					}
					out += bestActions[r][c];
				}
				out += "\n";
			}
			return out;
		}
	}

	// Returns the average utility per move of the policy,
	// as measured from ITERATIONS random drops of an agent onto
	// empty spaces
	public static double tryPolicy(Policy policy, Problem prob) {
		int totalUtility = 0;
		int totalMoves = 0;
		for (int i = 0; i < ITERATIONS; i++) {
			// Random empty starting loc
			int row, col;
			do {
				row = rng.nextInt(prob.map.size());
				col = rng.nextInt(prob.map.get(0).size());
			} while (!prob.map.get(row).get(col).equals("-"));
			// Run until pit, gold, or MAX_MOVES timeout
			// (in case policy recommends driving into wall repeatedly,
			// for example)
			for (int moves = 0; moves < MAX_MOVES; moves++) {
				totalMoves++;
				String policyRec = policy.bestActions[row][col];
				// Determine how far we go in that direction
				int displacement = 1;
				double totalProb = 0;
				double moveSample = rng.nextDouble();
				for (int p = 0; p <= prob.moveProbs.length; p++) {
					totalProb += prob.moveProbs[p];
					if (moveSample <= totalProb) {
						displacement = p + 1;
						break;
					}
				}
				int new_row = row;
				int new_col = col;
				if (policyRec.equals("U")) {
					new_row -= displacement;
					if (new_row < 0) {
						new_row = 0;
					}
				} else if (policyRec.equals("R")) {
					new_col += displacement;
					if (new_col >= prob.map.get(0).size()) {
						new_col = prob.map.get(0).size() - 1;
					}
				} else if (policyRec.equals("D")) {
					new_row += displacement;
					if (new_row >= prob.map.size()) {
						new_row = prob.map.size() - 1;
					}
				} else if (policyRec.equals("L")) {
					new_col -= displacement;
					if (new_col < 0) {
						new_col = 0;
					}
				}
				row = new_row;
				col = new_col;
				if (prob.map.get(row).get(col).equals("G")) {
					totalUtility += GOLD_REWARD;
					// End the current trial
					break;
				} else if (prob.map.get(row).get(col).equals("P")) {
					totalUtility += PIT_REWARD;
					break;
				}
			}
		}

		return totalUtility / (double) totalMoves;
	}

	public static class MDPSolver {

		// We'll want easy access to the real rewards while iterating, so
		// we'll keep both of these around
		public double[][] utilities;
		public double[][] rewards;
		public int[][] actions;

		public MDPSolver(Problem prob) {
			utilities = new double[prob.map.size()][prob.map.get(0).size()];
			rewards = new double[prob.map.size()][prob.map.get(0).size()];
			actions = new int[prob.map.size()][prob.map.get(0).size()];
			// Initialize utilities to the rewards in their spaces,
			// else 0
			for (int r = 0; r < utilities.length; r++) {
				for (int c = 0; c < utilities[0].length; c++) {
					String spaceContents = prob.map.get(r).get(c);
					if (spaceContents.equals("G")) {
						utilities[r][c] = GOLD_REWARD;
						rewards[r][c] = GOLD_REWARD;
					} else if (spaceContents.equals("P")) {
						utilities[r][c] = PIT_REWARD;
						rewards[r][c] = PIT_REWARD;
					} else {
						utilities[r][c] = 0.0;
						rewards[r][c] = 0.0;
					}
				}
			}
		}

		Policy solve(Problem prob, int iterations) {
			Policy policy = new Policy(prob);
			// TODO your code here & you'll probably want at least one helper function

			for (int itr = 0; itr < iterations; itr++) {
				
				int isNoChange = 1;
				for (int r = 0; r < utilities.length; r++) {
					for (int c = 0; c < utilities[0].length; c++) {
						if (utilities[r][c] == GOLD_REWARD || utilities[r][c] == PIT_REWARD) {
							continue;
						}
						double maxAction = Double.MIN_VALUE;
						for (int i = 0; i < 4; i++) {
							//0: up, 1: right, 2: down, 3: left
							double actionValue = 0;
							if (i == 0) {
								int j = 1;
								while (r - j >= 0 && j <= prob.moveProbs.length) {
									if (j == 1) {
										
									}
									actionValue += utilities[r - j][c] * prob.moveProbs[j - 1];
									j++;
								}
								int v = j;
								while (j <= prob.moveProbs.length) {
									actionValue += utilities[r - v + 1][c] * prob.moveProbs[j - 1];
									j++;
								}
							}
							if (i == 1) {
								int j = 1;
								while (c + j < utilities[0].length && j <= prob.moveProbs.length) {
									actionValue += utilities[r][c + j] * prob.moveProbs[j - 1];
									j++;
								}
								int v = j;
								while (j <= prob.moveProbs.length) {
									actionValue += utilities[r][c + v - 1] * prob.moveProbs[j - 1];
									j++;
								}
							}
							if (i == 2) {
								int j = 1;
								while (r + j < utilities.length && j <= prob.moveProbs.length) {
									actionValue += utilities[r + j][c] * prob.moveProbs[j - 1];
									j++;
								}
								int v = j;
								while (j <= prob.moveProbs.length) {
									actionValue += utilities[r + v - 1][c] * prob.moveProbs[j - 1];
									j++;
								}
							}
							if (i == 3) {
								int j = 1;
								while (c - j >= 0 && j <= prob.moveProbs.length) {
									actionValue += utilities[r][c - j] * prob.moveProbs[j - 1];
									j++;
								}
								int v = j;
								while (j <= prob.moveProbs.length) {
									actionValue += utilities[r][c - v + 1] * prob.moveProbs[j - 1];
									j++;
								}
							}
							if (actionValue > maxAction) {
								maxAction = actionValue;
								actions[r][c] = i;
							}	
						}
						double newUtility = rewards[r][c] + DISCOUNT_FACTOR * maxAction;
						if (newUtility != utilities[r][c]) {
							utilities[r][c] = newUtility;
							isNoChange = 0;
						}
						
					}
				}
				if (isNoChange == 1) {
					break;
				}
			}
			for (int r = 0; r < utilities.length; r++) {
				for (int c = 0; c < utilities[0].length; c++) {
					if (rewards[r][c] == GOLD_REWARD) {
						policy.bestActions[r][c] = "G";
						continue;
					}
					if (rewards[r][c] == PIT_REWARD) {
						policy.bestActions[r][c] = "P";
						continue;
					}
					int bestAction = -1;
					double maxValue = 0;
					/*for (int i = 0; i < 4; i++) {
						//0: up, 1: right, 2: down, 3: left
						double actionValue = 0;
						if (i == 0) {
							if (r - 1 >= 0) {
								actionValue = utilities[r - 1][c];
							} else {
								actionValue = 0.000001;
							}
						}
						if (i == 1) {
							if (c + 1 < utilities[0].length) {
								actionValue = utilities[r][c + 1];
							} else {
								actionValue = 0.000001;
							}
						}
						if (i == 2) {
							if (r + 1 < utilities.length) {
								actionValue = utilities[r + 1][c];
							} else {
								actionValue = 0.000001;
							}
						}
						if (i == 3) {
							if (c - 1 >= 0) {
								actionValue += utilities[r][c - 1];
							} else {
								actionValue = 0.000001;
							}
						}
						if (actionValue > maxValue && Math.abs(actionValue - maxValue) > 0.001) {
							maxValue = actionValue;
							bestAction = i;
						}	
					}*/
					switch (actions[r][c]) {
					case 0:
						policy.bestActions[r][c] = "U";
						break;
					case 1:
						policy.bestActions[r][c] = "R";
						break;
					case 2:
						policy.bestActions[r][c] = "D";
						break;
					case 3:
						policy.bestActions[r][c] = "L";
						break;
					default:
						System.out.println("Wrong! all utilities is 0!");
					}
					
					//policy.bestActions[r][c] = Double.toString(utilities[r][c]);
				}
			}
			return policy;
		}
		
		private double findBestAction(Problem prob, int r, int c, int level) {
			double maxAction = 0;
			for (int i = 0; i < 4; i++) {
				//0: up, 1: right, 2: down, 3: left
				double actionValue = 0;
				if (i == 0) {
					level = 1;
					while (r - level >= 0 && level <= prob.moveProbs.length) {
						if (level == 1) {
							
						}
						actionValue += utilities[r - level][c] * prob.moveProbs[level - 1];
						level++;
					}
				}
				if (i == 1) {
					int j = 1;
					while (c + j < utilities[0].length && j <= prob.moveProbs.length) {
						actionValue += utilities[r][c + j] * prob.moveProbs[j - 1];
						j++;
					}
				}
				if (i == 2) {
					int j = 1;
					while (r + j < utilities.length && j <= prob.moveProbs.length) {
						actionValue += utilities[r + j][c] * prob.moveProbs[j - 1];
						j++;
					}
				}
				if (i == 3) {
					int j = 1;
					while (c - j >= 0 && j <= prob.moveProbs.length) {
						actionValue += utilities[r][c - j] * prob.moveProbs[j - 1];
						j++;
					}
				}
				if (actionValue > maxAction) {
					maxAction = actionValue;
				}	
			}
			return 0.0;
		}
		

	}

	// QLearner: Same problem as MDP, but the agent doesn't know what the
	// world looks like, or what its actions do. It can learn the utilities of
	// taking actions in particular states through experimentation, but it
	// has no way of realizing what the general action model is
	// (like "Right" increasing the column number in general).
	public static class QLearner {

		// Use these to index into the first index of utilities[][][]
		public static final int UP = 0;
		public static final int RIGHT = 1;
		public static final int DOWN = 2;
		public static final int LEFT = 3;
		public static final int ACTIONS = 4;//index "4" is the bestAction in this state

		public double utilities[][][]; // utilities of actions
		public double rewards[][];


		public QLearner(Problem prob) {
			utilities = new double[prob.map.size()][prob.map.get(0).size()][ACTIONS];
			// Rewards are for convenience of lookup; the learner doesn't
			// actually "know" they're there until encountering them
			rewards = new double[prob.map.size()][prob.map.get(0).size()];
			
			for (int r = 0; r < rewards.length; r++) {
				for (int c = 0; c < rewards[0].length; c++) {
					String locType = prob.map.get(r).get(c);
					if (locType.equals("G")) {
						rewards[r][c] = GOLD_REWARD;
					} else if (locType.equals("P")) {
						rewards[r][c] = PIT_REWARD;
					} else {
						rewards[r][c] = 0.0; // not strictly necessary to init
					}
				}
			}
			// Java: default init utilities to 0
		}
		
		private int[] explore(int r, int c, int action, int step) {
			int dirIndex = 0;
			int[] newrc = new int[2];
			if (action == 0 || action == 2) {
				if (action == 0) {
					if (r - step < 0) {
						dirIndex = 0;
					} else {
						dirIndex = r - step;
					}
				} else {
					if (r + step >= utilities.length) {
						dirIndex = utilities.length - 1;
					} else {
						dirIndex = r + step;
					}
				}
				int bestAction = maxUtility(dirIndex, c);
				if (bestAction == -1) {
					bestAction = 0;
				}
				newrc[0] = dirIndex;
				newrc[1] = c;
				
				utilities[r][c][action] += LEARNING_RATE * (rewards[r][c] 
						+ DISCOUNT_FACTOR * utilities[dirIndex][c][bestAction] - utilities[r][c][action]);
				if (rewards[dirIndex][c] == GOLD_REWARD && utilities[dirIndex][c][0] != GOLD_REWARD) {
					for (int i = 0; i < 4; i++) {
						utilities[dirIndex][c][i] = GOLD_REWARD;
					}
				}
				if (rewards[dirIndex][c] == PIT_REWARD && utilities[dirIndex][c][0] != PIT_REWARD) {
					for (int i = 0; i < 4; i++) {
						utilities[dirIndex][c][i] = PIT_REWARD;
					}
				}
			} else if (action == 1 || action == 3){
				if (action == 1) {
					if (r + step >= utilities[0].length) {
						dirIndex = utilities[0].length - 1;
					} else {
						dirIndex = r + step;
					}
				} else {
					if (r - step < 0) {
						dirIndex = 0;
					} else {
						dirIndex = r - step;
					}
				}
				int bestAction = maxUtility(r, dirIndex);
				if (bestAction == -1) {
					bestAction = 0;
				}
				newrc[0] = r;
				newrc[1] = dirIndex;
				utilities[r][c][action] += LEARNING_RATE * (rewards[r][c] 
						+ DISCOUNT_FACTOR * utilities[r][dirIndex][bestAction] - utilities[r][c][action]);
				if (rewards[r][dirIndex] == GOLD_REWARD && utilities[dirIndex][c][0] != GOLD_REWARD) {
					for (int i = 0; i < 4; i++) {
						utilities[r][dirIndex][i] = GOLD_REWARD;
					}
				}
				if (rewards[r][dirIndex] == PIT_REWARD && utilities[dirIndex][c][0] != PIT_REWARD) {
					for (int i = 0; i < 4; i++) {
						utilities[r][dirIndex][i] = PIT_REWARD;
					}
				}
			} else {
				System.out.println("Illeagal action!!" + action);
			}
			/*if (utilities[r][c][action] > utilities[r][c][4]) {
				utilities[r][c][4] = action;
			}*/
			return newrc;
			
		}

		private int maxUtility(int r, int c) {
			int result = 0;
			double maxValue = utilities[r][c][0];
			int isBalance = 1;//1: all the utilities in 4 directions is the same
			for (int i = 1; i < 4; i++) {
				if (utilities[r][c][i] == maxValue) {
					continue;
				} else if (utilities[r][c][i] > maxValue) {
					maxValue = utilities[r][c][i];
					result = i;
					isBalance = 0;
				} else {
					isBalance = 0;
				}
			}
			if (isBalance == 1) {
				return -1;
			} else {
				return result;
			}
		}
		public Policy solve(Problem prob, int iterations) {
			Policy policy = new Policy(prob);
			// TODO: your code here; probably wants at least one helper too
			//double random = Math.random();
			for (int itr = 0; itr < iterations; itr++) {
				//int r = (int) (Math.random() * utilities.length);
				//int c = (int) (Math.random() * utilities[0].length);
				int newr = rng.nextInt(utilities.length);
				int newc = rng.nextInt(utilities[0].length);
				int r = newr;
				int c = newc;

				while (true) {
					

					if (utilities[r][c][0] == GOLD_REWARD || utilities[r][c][0] == PIT_REWARD) {
						break;
					}
					if (rewards[r][c] == GOLD_REWARD) {
						for (int i = 0; i < 4; i++) {
							utilities[r][c][i] = GOLD_REWARD;
						}
						break;
					}
					if (rewards[r][c] == PIT_REWARD) {
						for (int i = 0; i < 4; i++) {
							utilities[r][c][i] = PIT_REWARD;
						}
						break;
					}

					double p = rng.nextDouble();// Math.random();
					int action;
					if (p < EXPLORE_PROB) {
						action = (int) (rng.nextDouble() * 4);
					} else {
						int bestAction = maxUtility(r, c);

						if (bestAction != -1) {
							action = bestAction;
						} else {
							action = (int) (rng.nextDouble() * 4);
						}
					}
					double probMove = rng.nextDouble();
					// System.out.println("r: " + r + "c: " + c + "p: " + p + "action: " + action +
					// "moveprob: " + probMove);
					int[] newrc = new int[2];
					if (probMove >= 0 && probMove <= prob.moveProbs[0]) {
						newrc = explore(r, c, action, 1);
					} else if (probMove >= prob.moveProbs[0] && probMove < prob.moveProbs[1] + prob.moveProbs[0]) {
						newrc = explore(r, c, action, 2);
					} else {
						newrc = explore(r, c, action, 3);
					}

					r = newrc[0];
					c = newrc[1];
				}
			}
			for (int r = 0; r < utilities.length; r++) {
				for (int c = 0; c < utilities[0].length; c++) {
					if (rewards[r][c] == GOLD_REWARD) {
						policy.bestActions[r][c] = "G";
						continue;
					}
					if (rewards[r][c] == PIT_REWARD) {
						policy.bestActions[r][c] = "P";
						continue;
					}
					
/*					if (r == 2 && c == 4) {
                        System.out.println("up: " + utilities[r][c][0] + ",right: " + utilities[r][c][1] + ",down: " + utilities[r][c][2] + ",lift: " 
							+ utilities[r][c][3] + ",action: " + utilities[r][c][4]);
                    }*/
					int bestAction = maxUtility(r, c);
					if (bestAction == -1) {
						//bestAction = 0;
					}
					//StringBuilder result = new StringBuilder();
					String result = ".";
					for (int i = 0; i < 4; i++) {
						if (utilities[r][c][i] == utilities[r][c][bestAction]) {
							if (i == 0) {
								result += "U";
							}
							if (i == 1) {
								result += "R";
							}
							if (i == 2) {
								result += "D";
							}
							if (i == 3) {
								result += "L";
							}
						}
					}
					switch (bestAction) {
					case -1:
						policy.bestActions[r][c] = "-";// + result;
						break;
					case 0:
						policy.bestActions[r][c] = "U";// + result;
						break;
					case 1:
						policy.bestActions[r][c] = "R";// + result;
						break;
					case 2:
						policy.bestActions[r][c] = "D";// + result;
						break;
					case 3:
						policy.bestActions[r][c] = "L";// + result;
						break;
					default:
						System.out.println("Wrong! all utilities is 0!");
					}
				}
			}
			return policy;
		}
	}
}
