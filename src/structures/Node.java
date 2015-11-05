package structures;

import java.util.*;

public class Node {

	public enum Status {
		ACCEPTABLE, COMMON, REJECTABLE
	}

	private int number;
	private Map<String, Node> children;
	private Map<String, Set<Node>> parents;
	private Status status;
	private Status statusBackup;
	private int depth;
	private int depthBackup;
	private int acceptingPathsSum;
	private int rejectingPathsSum;
	private Map<String, Integer> acceptingPaths;
	private Map<String, Integer> rejectingPaths;
	private Node representative;

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
		this.depthBackup = depth;
		this.acceptingPathsSum = 0;
		this.acceptingPaths = new HashMap<>();
		this.children = new HashMap<>();
		this.parents = new HashMap<>();
		this.status = Status.COMMON;
		this.representative = this;
	}

	public void backup() {
		statusBackup = status;
		depthBackup = depth;
	}

	public void restore() {
		status = statusBackup;
		depth = depthBackup;
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
		acceptingPathsSum++;
		if (!acceptingPaths.containsKey(label)) {
			acceptingPaths.put(label, 0);
		}
		acceptingPaths.put(label, acceptingPaths.get(label) + k);
	}

	public void addRejectingPath(String label) {
		addRejectingPath(label, 1);
	}

	public void addRejectingPath(String label, int k) {
		rejectingPathsSum++;
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
		while (rep != rep.findRepresentative());
		return rep;
	}

	public void setRepresentative(Node representative) {
		this.representative = representative;
	}
}
