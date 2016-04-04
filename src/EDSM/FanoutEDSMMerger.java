package EDSM;

import algorithms.StateMerger;
import structures.APTA;
import structures.Node;

import misc.Settings;

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
			for (Map.Entry<String, Integer> e : first.getAcceptingPaths().entrySet()) {
				if (e.getValue() >= Settings.PATHS_ON_SYMBOL_LOWER_BOUND) {
					if (second.getAcceptingPaths().containsKey(e.getKey()) &&
							second.getAcceptingPaths().get(e.getKey()) == 0) {
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	protected int scoreAdd(Node red, Node blue) {
		return (red.getAcceptingPathsSum() > 0) && (blue.getAcceptingPathsSum() > 0) ? 1 : 0;
	}
}
