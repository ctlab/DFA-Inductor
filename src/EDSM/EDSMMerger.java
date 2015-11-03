package EDSM;

import structures.APTA;
import structures.Node;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class EDSMMerger {

	protected int score;
	protected List<String> alphabet;
	private APTA apta;

	public EDSMMerger(List<String> alphabet, APTA apta) {
		this.alphabet = alphabet;
		this.apta = apta;
		score = 0;
	}

	protected abstract boolean isConsistent(Node red, Node blue);
	protected abstract int scoreAdd(Node red, Node blue);

	protected boolean merge(Node red, Node blue, boolean finalMerge) {
		if (!isConsistent(red, blue)) {
			score = -blue.getDepth();
			return false;
		}
		score += scoreAdd(red, blue);
		red.backup();
		if (!red.isAcceptable() && !red.isRejectable()) {
			red.setStatus(blue.getStatus());
		}
		red.setDepth(Math.min(red.getDepth(), blue.getDepth()));
		red.setAcceptingPathsSum(red.getAcceptingPathsSum() + blue.getAcceptingPathsSum());
		red.setRejectingPathsSum(red.getRejectingPathsSum() + blue.getRejectingPathsSum());
		for (Map.Entry<String, Integer> e : blue.getAcceptingPaths().entrySet()) {
			red.addAcceptingPath(e.getKey(), e.getValue());
		}
		for (Map.Entry<String, Integer> e : blue.getRejectingPaths().entrySet()) {
			red.addRejectingPath(e.getKey(), e.getValue());
		}
		blue.setRepresentative(red);

		if(finalMerge) {
			apta.update(red, blue);
		}

		for (int i = 0; i < alphabet.size(); i++) {
			String s = alphabet.get(i);
			Node redChild = red.getChild(s);
			Node blueChild = blue.getChild(s);

			if (redChild == null) {
				if (blueChild != null) {
					blueChild.backup();
					blueChild.setDepth(red.getDepth());
					red.addChild(s, blueChild);
					blueChild.getParents().get(s).remove(blue);
				}
			} else if (redChild != blueChild) {
				redChild = redChild.findRepresentative();
				blueChild = blueChild.findRepresentative();
				if (!merge(redChild, blueChild, finalMerge)) {
					return false;
				}
			}
		}
		return true;
	}

	protected void undoMerge(Node red, Node blue) {
		if (!isConsistent(red, blue)) {
			return;
		}
		for (int i = alphabet.size() - 1; i >= 0; i--) {
			String s = alphabet.get(i);
			Node redChild = red.getChild(s);
			Node blueChild = blue.getChild(s);

			if (redChild == blueChild) {
				if (blueChild != null) {
					red.getChildren().remove(s);
					blueChild.restore();
				}
			} else if (redChild != null) {
				redChild = redChild.findRepresentative();
				blueChild = blueChild.findRepresentative();
				undoMerge(redChild, blueChild);
			}
		}

		red.restore();
		red.setAcceptingPathsSum(red.getAcceptingPathsSum() + blue.getAcceptingPathsSum());
		red.setRejectingPathsSum(red.getRejectingPathsSum() + blue.getRejectingPathsSum());
		for (Map.Entry<String, Integer> e : blue.getAcceptingPaths().entrySet()) {
			red.addAcceptingPath(e.getKey(), e.getValue());
		}
		for (Map.Entry<String, Integer> e : blue.getRejectingPaths().entrySet()) {
			red.addRejectingPath(e.getKey(), e.getValue());
		}

		blue.setRepresentative(red);
	}

	int getScore() {
		return score;
	}

	void resetScore() {
		score = 0;
	}
}
