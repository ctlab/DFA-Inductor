package algorithms;

import structures.APTA;
import structures.Node;

import java.util.*;

public abstract class StateMerger {

	protected int score;
	protected List<String> alphabet;
	protected APTA apta;

	public StateMerger(APTA apta) {
		this.apta = apta;
		alphabet = asSortedList(apta.getAlphabet());
		score = 0;
	}

	protected abstract boolean isConsistent(Node red, Node blue);
	protected abstract int scoreAdd(Node red, Node blue);

	public void merge(Node red, Node blue) {
		if (!isConsistent(red, blue)) {
			score = -1;
		}

		if (score >= 0) {
			score += scoreAdd(red, blue);
		}
		red.backupAndSetStatus(blue);
		if (!red.isAcceptable() && !red.isRejectable()) {
			red.setStatus(blue.getStatus());
		}
		for (Map.Entry<String, Integer> e : blue.getAcceptingPaths().entrySet()) {
			red.addAcceptingPath(e.getKey(), e.getValue());
		}
		for (Map.Entry<String, Integer> e : blue.getRejectingPaths().entrySet()) {
			red.addRejectingPath(e.getKey(), e.getValue());
		}
		red.setAcceptingEndings(red.getAcceptingEndings() + blue.getAcceptingEndings());
		red.setRejectingEndings(red.getRejectingEndings() + blue.getRejectingEndings());

		blue.setRepresentative(red);

		for (String s : alphabet) {
			Node redChild = red.getChild(s);
			Node blueChild = blue.getChild(s);

			if (redChild == null) {
				if (blueChild != null) {
					red.addChild(s, blueChild);
				}
			} else if (blueChild != null) {
				redChild = redChild.findRepresentative();
				blueChild = blueChild.findRepresentative();
				if (redChild != blueChild) {
					blueChild.saveBackReference(s, blue);
					merge(redChild, blueChild);
				}
			}
		}
	}

	public void undoMerge(Node red, Node blue) {
		for (int i = alphabet.size() - 1; i >= 0; i--) {
			String s = alphabet.get(i);
			Node redChild = red.getChild(s);
			Node blueChild = blue.getChild(s);

			if (redChild == blueChild) {
				if (blueChild != null) {
					red.getChildren().remove(s);
				}
			} else if (redChild != null && blueChild != null) {
				blueChild = blueChild.findUntilBackReference(s, blue);
				redChild = blueChild.getRepresentative();
				if (redChild != blueChild) {
					undoMerge(redChild, blueChild);
				}
				blueChild.removeBackReference(s);
			}
		}

		red.restoreStatus(blue);
		for (Map.Entry<String, Integer> e : blue.getAcceptingPaths().entrySet()) {
			red.addAcceptingPath(e.getKey(), -e.getValue());
		}
		for (Map.Entry<String, Integer> e : blue.getRejectingPaths().entrySet()) {
			red.addRejectingPath(e.getKey(), -e.getValue());
		}
		red.setAcceptingEndings(red.getAcceptingEndings() - blue.getAcceptingEndings());
		red.setRejectingEndings(red.getRejectingEndings() - blue.getRejectingEndings());

		blue.setRepresentative(null);
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
