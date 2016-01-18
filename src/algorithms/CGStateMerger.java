package algorithms;

import structures.APTA;
import structures.Node;

import java.util.List;

public class CGStateMerger extends StateMerger{

	public CGStateMerger(APTA apta) {
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
		return 1;
	}
}
