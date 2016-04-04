package structures;

import misc.Settings;

import java.util.*;

public class Node {

	public enum Status {
		ACCEPTABLE, COMMON, REJECTABLE
	}

	public enum SINK_TYPE {
		NON_SINK, ACCEPTING_SINK, REJECTING_SINK, LOW_SINK
	}

	private int number;
	private Map<String, Node> children;
	private Map<String, Set<Node>> parents;
	private int acceptingEndings;
	private int rejectingEndings;
	private Status status;
	private Status statusBackup;
	private int depth;
	private int depthBackup;
	private int acceptingPathsSum;
	private int rejectingPathsSum;
	private Map<String, Integer> acceptingPaths;
	private Map<String, Integer> rejectingPaths;
	private Node representative;
	private int color;

	public Node(int number) {
		init(number, 0);
	}

	public Node(int number, int depth) {
		init(number, depth);
	}

	public Node(int number, int depth, String label, Node parent) {
		init(number, depth);
		addParent(label, parent);
	}

	private void init(int number, int depth) {
		this.number = number;
		this.depth = depth;
		this.depthBackup = 0;
		this.acceptingPathsSum = 0;
		this.acceptingPaths = new HashMap<>();
		this.rejectingPathsSum = 0;
		this.rejectingPaths = new HashMap<>();
		this.children = new HashMap<>();
		this.parents = new HashMap<>();
		this.acceptingEndings = 0;
		this.rejectingEndings = 0;
		this.status = Status.COMMON;
		this.statusBackup = null;
		this.representative = this;
		this.color = -1;
	}

	public void backup() {
		if (statusBackup == null && depthBackup == 0) {
			statusBackup = status;
			depthBackup = depth;
		}
	}

	void resetBackup() {
		statusBackup = null;
		depthBackup = 0;
	}

	public void restore() {
		if (statusBackup != null && depthBackup != 0) {
			status = statusBackup;
			if (status == Status.COMMON) {
				acceptingEndings = 0;
				rejectingEndings = 0;
			}
			depth = depthBackup;

			statusBackup = null;
			depthBackup = 0;
		}
	}

	public int getAcceptingEndings() {
		return acceptingEndings;
	}

	public void setAcceptingEndings(int acceptingEndings) {
		this.acceptingEndings = acceptingEndings;
	}

	public int getRejectingEndings() {
		return rejectingEndings;
	}

	public void setRejectingEndings(int rejectingEndings) {
		this.rejectingEndings = rejectingEndings;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public int getAcceptingPathsSum() {
		return acceptingPathsSum;
	}

	public int getRejectingPathsSum() {
		return rejectingPathsSum;
	}

	public void setAcceptingPathsSum(int acceptingPathsSum) {
		this.acceptingPathsSum = acceptingPathsSum;
	}

	public void setRejectingPathsSum(int rejectingPathsSum) {
		this.rejectingPathsSum = rejectingPathsSum;
	}

	public Map<String, Integer> getAcceptingPaths() {
		return acceptingPaths;
	}

	public Map<String, Integer> getRejectingPaths() {
		return rejectingPaths;
	}

	public void setAcceptingPaths(Map<String, Integer> acceptingPaths) {
		this.acceptingPaths = acceptingPaths;
	}

	public void setRejectingPaths(Map<String, Integer> rejectingPaths) {
		this.rejectingPaths = rejectingPaths;
	}

	public void addAcceptingPath(String label) {
		addAcceptingPath(label, 1);
	}

	public void addAcceptingPath(String label, int k) {
		acceptingPathsSum += k;
		if (!acceptingPaths.containsKey(label)) {
			acceptingPaths.put(label, 0);
		}
		acceptingPaths.put(label, acceptingPaths.get(label) + k);
	}

	public void addRejectingPath(String label) {
		addRejectingPath(label, 1);
	}

	public void addRejectingPath(String label, int k) {
		rejectingPathsSum += k;
		if (!rejectingPaths.containsKey(label)) {
			rejectingPaths.put(label, 0);
		}
		rejectingPaths.put(label, rejectingPaths.get(label) + k);
	}


	public int getNumber() {
		return number;
	}

	void setNumber(int number) {
		this.number = number;
	}

	public Status getStatus() {
		return status;
	}

	public boolean isAcceptable() {
		return status == Status.ACCEPTABLE;
	}

	public boolean isRejectable() {
		return status == Status.REJECTABLE;
	}

	public void setStatus(Status status) {
		this.status = status;
		if (status == Status.ACCEPTABLE) {
			if (acceptingEndings == 0) {
				acceptingEndings = 1;
			}
			if (rejectingEndings != 0) {
				rejectingEndings = 0;
			}
		} else if (status == Status.REJECTABLE) {
			if (rejectingEndings == 0) {
				rejectingEndings = 1;
			}
			if (acceptingEndings != 0) {
				acceptingEndings = 0;
			}
		}
	}

	public Map<String, Set<Node>> getParents() {
		return parents;
	}

	public Set<Node> getParentByLabel(String s) {
		return parents.containsKey(s) ? parents.get(s) : null;
	}

	public Map<String, Node> getChildren() {
		return children;
	}

	public Node getChild(String s) {
		return children.containsKey(s) ? children.get(s) : null;
	}

	public void addChild(String s, Node child) {
		children.put(s, child);
		if (!child.getParents().containsKey(s) || !child.getParents().get(s).contains(this)) {
			child.addParent(s, this);
		}
	}

	public void addParent(String s, Node parent) {
		if (!parents.containsKey(s)) {
			parents.put(s, new HashSet<Node>());
		}
		parents.get(s).add(parent);
		if (parent.getChild(s) == null || parent.getChild(s) != this) {
			parent.addChild(s, this);
		}
	}

	public Node findRepresentative() {
		Node rep = this.representative;
		while (rep != rep.representative) {
			rep = rep.findRepresentative();
		};
		return rep;
	}

	public void setRepresentative(Node representative) {
		this.representative = representative;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public SINK_TYPE getSinkType() {
		int sinkMode = Settings.SINKS_MODE;
		if (sinkMode == 0) {
			return SINK_TYPE.NON_SINK;
		}
		if (sinkMode == 3 || sinkMode == 4 || sinkMode == 5) {
			if (isLowInformationalSink()) {
				return SINK_TYPE.LOW_SINK;
			}
		}
		if (sinkMode == 1 || sinkMode ==  2 || sinkMode == 4 || sinkMode == 5) {
			if (isRejectingSink()) {
				return SINK_TYPE.REJECTING_SINK;
			}
		}
		if (sinkMode == 2 || sinkMode == 5) {
			if (isAcceptingSink()) {
				return SINK_TYPE.ACCEPTING_SINK;
			}
		}
		return SINK_TYPE.NON_SINK;
	}

	public boolean isParticularSink(int i) {
		int sinkMode = Settings.SINKS_MODE;
		if (sinkMode == 0) {
			return false;
		}
		switch (i) {
			case 0:
				if (sinkMode == 1 || sinkMode == 2 || sinkMode == 4 || sinkMode == 5) {
					return isRejectingSink();
				}
				if (sinkMode == 3) {
					return isLowInformationalSink();
				}
				return false;
			case 1:
				if (sinkMode == 2 || sinkMode == 5) {
					return isAcceptingSink();
				}
				if (sinkMode == 4) {
					return isLowInformationalSink();
				}
				return false;
			case 2:
				if (sinkMode == 5) {
					return isLowInformationalSink();
				}
				return false;
		}
		return false;
	}

	public boolean isLowInformationalSink() {
		Node node = findRepresentative();
		return node.acceptingPathsSum + node.getAcceptingEndings() < Settings.PATHS_LOWER_BOUND;
	}

	public boolean isAcceptingSink() {
		Node node = findRepresentative();
		return node.getRejectingPathsSum() == 0 && !node.isRejectable();
	}

	public boolean isRejectingSink() {
		Node node = findRepresentative();
		return node.getAcceptingPathsSum() == 0 && !node.isAcceptable();
	}
}
