package EDSM;

import algorithms.StateMerger;
import misc.Settings;
import structures.APTA;
import structures.Node;

import java.util.Map;

public class FanoutEDSMMerger extends StateMerger {

	public FanoutEDSMMerger(APTA apta) {
		super(apta);
	}

	@Override
	protected boolean isConsistent(Node red, Node blue) {
		if (red.isAcceptable() && blue.isRejectable() || red.isRejectable() && blue.isAcceptable()) {
			return false;
		}
		if (!checkConsistencyConditions(red, blue)) {
			return false;
		}
		if (!checkConsistencyConditions(blue, red)) {
			return false;
		}
		return true;
	}

	private boolean checkConsistencyConditions(Node first, Node second) {
		if (first.getAcceptingPathsSum() >= Settings.PATHS_LOWER_BOUND) {
			for (Map.Entry<String, Integer> e : second.getAcceptingPaths().entrySet()) {
				if (e.getValue() >= Settings.PATHS_ON_SYMBOL_LOWER_BOUND) {
					if (first.getAcceptingPaths().containsKey(e.getKey()) &&
							first.getAcceptingPaths().get(e.getKey()) == 0) {
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	protected int scoreAdd(Node red, Node blue) {
		int score = 0;
		for (String label : apta.getAlphabet()) {
			if (red.getAcceptingPaths().containsKey(label) && blue.getAcceptingPaths().containsKey(label)) {
				if (red.getAcceptingPaths().get(label) > 0 && blue.getAcceptingPaths().get(label) > 0) {
					score++;
				}
			}
//			if (red.getRejectingPaths().containsKey(label) && blue.getRejectingPaths().containsKey(label)) {
//				if (red.getRejectingPaths().get(label) > 0 && blue.getRejectingPaths().get(label) > 0) {
//					score++;
//				}
//			}
		}
		return score;
	}
}
