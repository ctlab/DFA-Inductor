package EDSM;

import structures.APTA;
import structures.Node;
import structures.Node.Status;

import java.util.List;

public class StatusEDSMMerger extends EDSMMerger {

	public StatusEDSMMerger(List<String> alphabet) {
		super(alphabet);
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
