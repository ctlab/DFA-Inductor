package EDSM;

import algorithms.StateMerger;
import structures.APTA;
import structures.Node;

import misc.Settings;

import java.util.Random;

public class EDSMWorker {

	public enum EDSMHeuristic{
		Status, Fanout
	}

	private APTA apta;
	private StateMerger merger;
	private Random random = new Random(Settings.SEED);

	public EDSMWorker(APTA apta) {
		this.apta = apta;
		switch (Settings.EDSM_HEURISTIC) {
			case Status:
				merger = new StatusEDSMMerger(apta);
				break;
			case Fanout:
				merger = new FanoutEDSMMerger(apta);
				break;
		}
	}

	public boolean startMerging() {
		while (true) {
			MergePair mergePair = findBestMerge();
			if (mergePair.score < 0) {
 				apta.promoteBlueToRed(mergePair.blue);
			} else {
				if (apta.getSize() < Settings.APTA_BOUND || apta.getRedNodes().size() > Settings.RED_BOUND) {
					break;
				}
				merge(mergePair);
			}
		}
		return true;
	}

	private MergePair findBestMerge() {
		MergePair pair;
		MergePair bestPair = new MergePair();
		double score;
		for (Node blue : apta.getBlueNodes()) {
			if (blue.getSinkType() != Node.SINK_TYPE.NON_SINK) {
				continue;
			}
			pair = new MergePair();
			for (Node red : apta.getRedNodes()) {
				score = mergeAndUndo(red, blue);
				if (Settings.RANDOM_GREEDY_MODE && score > 0) {
					score *= random.nextDouble();
				}
				if (score > pair.score || pair.score == 0) {
					pair.update(red, blue, score);
				}
			}
			//TODO: think about >= and >
			if (pair.score > 0) {
				if (Settings.EXTEND_FIRST) {
					if (bestPair.score >= 0 && pair.score >= bestPair.score) {
						bestPair = pair;
					}
				} else {
					if (pair.score >= bestPair.score) {
						bestPair = pair;
					}
				}
			}
			if (pair.score < 0) {
				if (Settings.EXTEND_FIRST) {
					bestPair = pair;
					break;
				} else {
					if (bestPair.score < 0 && bestPair.score < pair.score || bestPair.score == 0) {
						bestPair = pair;
					}
				}
			}
		}
		return bestPair;
	}

	private int merge(MergePair pair) {
		Node red = pair.red;
		Node blue = pair.blue;
		merger.resetScore();
		merger.merge(red.findRepresentative(), blue.findRepresentative());
		apta.updateRedBlue();
		return merger.getScore();
	}

	private int mergeAndUndo(Node red, Node blue) {
		merger.resetScore();
		merger.merge(red, blue);
		int res = merger.getScore() >= 0 ? merger.getScore() : -1;
		merger.undoMerge(red, blue);
		return res;
	}

	class MergePair {
		Node red;
		Node blue;
		double score;

		MergePair() {
			score = 0;
		}

		MergePair(Node red, Node blue, double score) {
			this.red = red;
			this.blue = blue;
			this.score = score;
		}

		MergePair(Node red, Node blue, boolean consistent) {
			this.red = red;
			this.blue = blue;
			score = Integer.MIN_VALUE;
		}

		void update(Node red, Node blue, double score) {
			this.red = red;
			this.blue = blue;
			//TODO: refactor
			this.score = score == 0 ? 0.0001 : score;
		}
	}
}
