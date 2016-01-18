package EDSM;

import algorithms.StateMerger;
import structures.APTA;
import structures.Node;

import java.util.List;

public class StatusEDSMMerger extends StateMerger {

	public StatusEDSMMerger(APTA apta) {
		super(apta);
	}

	@Override
	protected boolean isConsistent(Node red, Node blue) {
		if (red.isAcceptable() && blue.isRejectable() || red.isRejectable() && blue.isAcceptable()) {
			return false;
		}
		return true;
	}

	@Override
	protected int scoreAdd(Node red, Node blue) {
		return (red.isAcceptable() && blue.isAcceptable()) || (red.isRejectable() && blue.isRejectable()) ? 1 : 0;
	}
}
