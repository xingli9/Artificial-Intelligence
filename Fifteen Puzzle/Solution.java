package Assign1;

import java.util.*;

// Solving the 16-puzzle with A* using two heuristics:
// tiles-out-of-place and total-distance-to-move
/*
 * 
 * 
 */
public class Solution implements Comparable<Solution>{
	public static final int PUZZLE_WIDTH = 4;
	public static final int BLANK = 0;
	
	// BETTER: false for tiles-displaced heuristic, true for Manhattan distance
	public static boolean BETTER = true;
	public static boolean EUCLIDEAN = false;
	// You can change this representation if you prefer.
	// If you don't, be careful about keeping the tiles and the blank
	// row and column consistent.
	private int[][] tiles; // [row][column]
	private int blank_r, blank_c; // blank row and column

	private int f;
	private int g = 0;
	private int h;
	private int isedge = 0;
	private int previousMove = 0;
	private Solution parent = null;

	public static void main(String[] args) {
		
		Solution myPuzzle = readPuzzle();
		long startTime = System.nanoTime();
		LinkedList<Solution> solutionSteps = myPuzzle.solve(BETTER);
		printSteps(solutionSteps);
		double timeDif = (System.nanoTime() - startTime) * 0.000000001;
		System.out.println("The time difference is: " + timeDif + " second");
	}

	Solution() {
		tiles = new int[PUZZLE_WIDTH][PUZZLE_WIDTH];
	}

	static Solution readPuzzle() {
		Solution newPuzzle = new Solution();
		Scanner myScanner = new Scanner(System.in);
		int row = 0;
		while (myScanner.hasNextLine() && row < PUZZLE_WIDTH) {
			String line = myScanner.nextLine();
			String[] numStrings = line.split(" ");
			for (int i = 0; i < PUZZLE_WIDTH; i++) {
				if (numStrings[i].equals("-")) {
					newPuzzle.tiles[row][i] = BLANK;
					newPuzzle.blank_r = row;
					newPuzzle.blank_c = i;
				} else {
					newPuzzle.tiles[row][i] = new Integer(numStrings[i]);
				}
			}
			row++;
		}

		myScanner.close();
		return newPuzzle;
	}

	public String toString() {
		String out = "";
		for (int i = 0; i < PUZZLE_WIDTH; i++) {
			for (int j = 0; j < PUZZLE_WIDTH; j++) {
				if (j > 0) {
					out += " ";
				}
				if (tiles[i][j] == BLANK) {
					out += "-";
				} else {
					out += tiles[i][j];
				}
			}
			out += "\n";
		}
		return out;
	}

	public Solution copy() {
		Solution clone = new Solution();
		clone.blank_r = blank_r;
		clone.blank_c = blank_c;
		for (int i = 0; i < PUZZLE_WIDTH; i++) {
			for (int j = 0; j < PUZZLE_WIDTH; j++) {
				clone.tiles[i][j] = this.tiles[i][j];
			}
		}
		return clone;
	}

	// betterH: if false, use tiles-out-of-place heuristic
	// if true, use total-manhattan-distance heuristic
	LinkedList<Solution> solve(boolean betterH) {
		// TODO - placeholder just to compile
		// use new comparator
/*	
 * 		Queue<NumberPuzzle> openList = new PriorityQueue<NumberPuzzle>(new Comparator<NumberPuzzle>() {
			public int compare(NumberPuzzle n1, NumberPuzzle n2) {
				return n1.f - n2.f;
			}
		});*/
		
		// use comparable
		Queue<Solution> openList = new PriorityQueue<Solution>();
		Solution endNode = null;
		endNode = recursion(this, openList, betterH);
		LinkedList<Solution> result = new LinkedList<Solution>();
		Solution firstNode = endNode;
		while (firstNode != null) {
			result.addFirst(firstNode);
			firstNode = firstNode.parent;

		}
		return result;
	}

	// recursion
	private Solution recursion(Solution currentNode, Queue<Solution> openList, boolean betterH) {
		for (int i = 1; i <= 4; i++) {
			Solution nextNode = currentNode.copy();
			nextNode.swap(i);
			nextNode.previousMove = i;
			nextNode.g = currentNode.g + 1;

			if (betterH) {
				if (EUCLIDEAN) {
					nextNode.euclideanH();
				} else {
					nextNode.manhattanH();
				}
			} else {
				nextNode.tileH();
			}
			nextNode.f = nextNode.g + nextNode.h;
			if (nextNode.isedge == 0 && (currentNode.g == 0 || Math.abs(currentNode.previousMove - i) != 2)) {
				nextNode.parent = currentNode;
				openList.add(nextNode);
			}
			if (nextNode.solved()) {
				return nextNode;
			}
		}
		return recursion(openList.poll(), openList, betterH);
	}

	public boolean solved() {
		int shouldBe = 1;
		for (int i = 0; i < PUZZLE_WIDTH; i++) {
			for (int j = 0; j < PUZZLE_WIDTH; j++) {
				if (tiles[i][j] != shouldBe) {
					return false;
				} else {
					// Take advantage of BLANK == 0
					shouldBe = (shouldBe + 1) % (PUZZLE_WIDTH * PUZZLE_WIDTH);
				}
			}
		}
		return true;
	}

	static void printSteps(LinkedList<Solution> steps) {
		if (steps == null) {
			System.out.println("There is no solution!");
			return;
		}
		for (Solution s : steps) {
			System.out.println(s);
		}
	}

	private void swap(int i) {
		/*
		 * 1: left move
		 * 2: up move
		 * 3: right move
		 * 4: down move
		 * 
		 */
		if (i == 1) {
			if (this.blank_c == 0) {
				this.isedge = -1;
			} else {
				this.tiles[this.blank_r][this.blank_c] = this.tiles[this.blank_r][this.blank_c - 1];
				this.blank_c--;
				this.tiles[this.blank_r][this.blank_c] = BLANK;
			}
		}

		if (i == 3) {
			if (this.blank_c == 3) {
				this.isedge = -1;
			} else {
				this.tiles[this.blank_r][this.blank_c] = this.tiles[this.blank_r][this.blank_c + 1];
				this.blank_c++;
				this.tiles[this.blank_r][this.blank_c] = BLANK;
			}
		}

		if (i == 2) {
			if (this.blank_r == 0) {
				this.isedge = -1;
			} else {
				this.tiles[this.blank_r][this.blank_c] = this.tiles[this.blank_r - 1][this.blank_c];
				this.blank_r--;
				this.tiles[this.blank_r][this.blank_c] = BLANK;
			}
		}

		if (i == 4) {
			if (this.blank_r == 3) {
				this.isedge = -1;
			} else {
				this.tiles[this.blank_r][this.blank_c] = this.tiles[this.blank_r + 1][this.blank_c];
				this.blank_r++;
				this.tiles[this.blank_r][this.blank_c] = BLANK;
			}
		}
	}

	// use total-manhattan-distance heuristic
	private void manhattanH() {
		int value = 0;
		for (int i = 0; i < PUZZLE_WIDTH; i++) {
			for (int j = 0; j < PUZZLE_WIDTH; j++) {
				if (i == this.blank_r && j == this.blank_c) {
					continue;
				}
				int distanceR = Math.abs(tiles[i][j] / PUZZLE_WIDTH - i);
				int distanceC = Math.abs(tiles[i][j] % PUZZLE_WIDTH - j);
				value += (distanceR + distanceC);
			}
		}
		this.h = value;
	}
	
	// use euclidean heuristic
	private void euclideanH() {
		double value = 0;
		for (int i = 0; i < PUZZLE_WIDTH; i++) {
			for (int j = 0; j < PUZZLE_WIDTH; j++) {
				if (i == this.blank_r && j == this.blank_c) {
					continue;
				}
				int distanceR = tiles[i][j] / PUZZLE_WIDTH;
				int distanceC = tiles[i][j] % PUZZLE_WIDTH;
				double distance = Math.sqrt((distanceR - i ) * (distanceR - i ) + (distanceC - j ) * (distanceC - j ));
				value += distance;
			}
		}
		
		this.h = (int)value;
	}

	// use tiles-out-of-place heuristic
	private void tileH() {
		int value = 0;
		int shouldBe = 1;
		for (int i = 0; i < PUZZLE_WIDTH; i++) {
			for (int j = 0; j < PUZZLE_WIDTH; j++) {
				if (tiles[i][j] != shouldBe) {
					value++;
				}
				shouldBe = (shouldBe + 1) % (PUZZLE_WIDTH * PUZZLE_WIDTH);
			}
		}
		this.h = value;
	}

	@Override
	public int compareTo(Solution o) {
		// TODO Auto-generated method stub
		return this.f - o.f;
	}




}
