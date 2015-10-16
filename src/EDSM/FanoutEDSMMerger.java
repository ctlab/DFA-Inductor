package EDSM;

import structures.APTA;
import structures.Node;

import java.util.List;
import java.util.Map;

public class FanoutEDSMMerger extends EDSMMerger {
	protected int pathsLowerBound = 0;
	protected int pathsOnSymbolLowerBound = 0;

	public FanoutEDSMMerger(List<String> alphabet) {
		super(alphabet);
		pathsLowerBound = 25;
		pathsOnSymbolLowerBound = 10;
	}

	@Override
	protected boolean isConsistent(Node red, Node blue) {
		if (red.isAcceptable() && blue.isRejectable() || red.isRejectable() && blue.isAcceptable()) {
			return false;
		}
		if (red.getAcceptingPathsSum() >= pathsLowerBound) {
			for (Map.Entry<String, Integer> e : red.getAcceptingPaths().entrySet()) {
				if (e.getValue() >= pathsOnSymbolLowerBound) {
					if (blue.getAcceptingPaths().get(e.getKey()) == 0) {
						return false;
					}
				}
			}
		}
		if (blue.getAcceptingPathsSum() >= pathsLowerBound) {
			for (Map.Entry<String, Integer> e : blue.getAcceptingPaths().entrySet()) {
				if (e.getValue() >= pathsOnSymbolLowerBound) {
					if (red.getAcceptingPaths().get(e.getKey()) == 0) {
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

	public void setPathsLowerBound(int pathsLowerBound) {
		this.pathsLowerBound = pathsLowerBound;
	}

	public void setPathsOnSymbolLowerBound(int pathsOnSymbolLowerBound) {
		this.pathsOnSymbolLowerBound = pathsOnSymbolLowerBound;
	}
}
