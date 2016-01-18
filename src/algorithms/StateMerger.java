package algorithms;

import structures.APTA;
import structures.Node;

import java.util.*;

public abstract class StateMerger {

	protected int score;
	protected List<String> alphabet;
	private APTA apta;

	public StateMerger(APTA apta) {
		this.apta = apta;
		alphabet = asSortedList(apta.getAlphabet());
		score = 0;
	}

	protected abstract boolean isConsistent(Node red, Node blue);
	protected abstract int scoreAdd(Node red, Node blue);

	public boolean merge(Node red, Node blue, boolean finalMerge) {
		if (!isConsistent(red, blue)) {
			score = -1;
		}
		if (score >= 0) {
			score += scoreAdd(red, blue);
		}
		red.backup();
		if (!red.isAcceptable() && !red.isRejectable()) {
			red.setStatus(blue.getStatus());
		}
		red.setDepth(Math.min(red.getDepth(), blue.getDepth()));
		for (Map.Entry<String, Integer> e : blue.getAcceptingPaths().entrySet()) {
			red.addAcceptingPath(e.getKey(), e.getValue());
		}
		for (Map.Entry<String, Integer> e : blue.getRejectingPaths().entrySet()) {
			red.addRejectingPath(e.getKey(), e.getValue());
		}

		if(finalMerge) {
			apta.update(red, blue);
		} else {
			blue.setRepresentative(red);
		}

		for (String s : alphabet) {
			Node redChild = red.getChild(s);
			Node blueChild = blue.getChild(s);

			if (redChild == null) {
				if (blueChild != null) {
					blueChild.backup();
					blueChild.setDepth(red.getDepth());
					red.addChild(s, blueChild);
					blueChild.getParents().get(s).remove(blue);
				}
			} else if (redChild != blueChild && blueChild != null) {
				redChild = redChild.findRepresentative();
				blueChild = blueChild.findRepresentative();
				if (!merge(redChild, blueChild, finalMerge)) {
					return false;
				}
			}
		}
		return true;
	}

	public void undoMerge(Node red, Node blue) {
		for (int i = alphabet.size() - 1; i >= 0; i--) {
			String s = alphabet.get(i);
			Node redChild = red.getChild(s);
			Node blueChild = blue.getChild(s);

			if (redChild == blueChild) {
				if (blueChild != null) {
					red.getChildren().remove(s);
					blueChild.getParents().get(s).add(blue);
					blueChild.getParents().get(s).remove(red);
					blueChild.restore();
				}
			} else if (redChild != null && blueChild != null) {
				//redChild = redChild.findRepresentative();
				//blueChild = blueChild.findRepresentative();
				redChild = blueChild.findRepresentative();
				undoMerge(redChild, blueChild);
			}
		}

		red.restore();
		for (Map.Entry<String, Integer> e : blue.getAcceptingPaths().entrySet()) {
			red.addAcceptingPath(e.getKey(), -e.getValue());
		}
		for (Map.Entry<String, Integer> e : blue.getRejectingPaths().entrySet()) {
			red.addRejectingPath(e.getKey(), -e.getValue());
		}

		blue.setRepresentative(blue);
	}

	public int getScore() {
		return score;
	}

	public void resetScore() {
		score = 0;
	}

	private static	<T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
		List<T> list = new ArrayList<>(c);
		Collections.sort(list);
		return list;
	}
}
