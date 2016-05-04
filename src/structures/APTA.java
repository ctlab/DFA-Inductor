package structures;

import structures.Node.SINK_TYPE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class APTA {

	private Node root;
	private int size;
	private int words;
	private int alphaSize;
	private Set<String> alphabet;
	private Set<Integer> acceptableNodes;
	private Set<Integer> rejectableNodes;
	private Map<Integer, Node> indexesOfNodes;
	private Map<String, Set<Integer>> vlset;
	private Set<Node> redNodes;
	private Set<Node> notRedNodes;
	private Set<Node> blueNodes;

	private StringTokenizer st = null;

	public APTA() {
		acceptableNodes = new HashSet<>();
		rejectableNodes = new HashSet<>();
		indexesOfNodes = new HashMap<>();
		vlset = new HashMap<>();
		alphabet = new HashSet<>();
		size = 0;
		redNodes = new TreeSet<>(new NodesComparator());
		notRedNodes = new TreeSet<>(new NodesComparator());
		blueNodes = new TreeSet<>(new NodesComparator());

		root = new Node(size);
		indexesOfNodes.put(size++, root);
		initRedBlue();
	}

	class NodesComparator implements Comparator<Node> {

		@Override
		public int compare(Node a, Node b) {
			int aW = a.getAcceptingPathsSum() + a.getRejectingPathsSum();
			int bW = b.getAcceptingPathsSum() + b.getRejectingPathsSum();

			if (aW > bW) {
				return -1;
			} else if (aW < bW) {
				return 1;
			} else {
				if (a.getNumber() < b.getNumber()) {
					return -1;
				} else if (a.getNumber() > b.getNumber()) {
					return 1;
				}
			}
			return 0;
		}
	}

	public APTA(APTA other) {
		this.size = other.size;
		this.words = other.words;
		this.alphaSize = other.alphaSize;

		alphabet = new HashSet<>(other.getAlphabet());
		acceptableNodes = new HashSet<>(other.getRejectableNodes());
		rejectableNodes = new HashSet<>(other.getRejectableNodes());
		vlset = new HashMap<>(other.vlset);
		redNodes = new TreeSet<>(new NodesComparator());
		notRedNodes = new TreeSet<>(new NodesComparator());
		blueNodes = new TreeSet<>(new NodesComparator());

		root = new Node(0);
		Node curNode = root;
		copyNodes(curNode, other.getRoot(), other);
	}

	private void copyNodes(Node current, Node parallel, APTA other) {
		String label;
		Node child;
		Node newChild;
		indexesOfNodes.put(parallel.getNumber(), current);
		for (Map.Entry<String, Node> e: parallel.getChildren().entrySet()) {
			label = e.getKey();
			child = e.getValue();
			newChild = new Node(child.getNumber());
			current.addChild(label, newChild);

			newChild.setAcceptingEndings(child.getAcceptingEndings());
			newChild.setRejectingEndings(child.getRejectingEndings());
			newChild.setAcceptingPathsSum(child.getAcceptingPathsSum());
			newChild.setAcceptingPaths(child.getAcceptingPaths());
			newChild.setRejectingPathsSum(child.getRejectingPathsSum());
			newChild.setRejectingPaths(child.getRejectingPaths());
			newChild.setStatus(child.getStatus());

			if (other.getRedNodes().contains(child)) {
				this.redNodes.add(newChild);
			} else {
				this.notRedNodes.add(newChild);
			}
			if (other.getBlueNodes().contains(child)) {
				this.blueNodes.add(newChild);
			}

			copyNodes(newChild, child, other);
		}
	}

	public APTA(InputStream is) throws IOException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			size = 0;
			acceptableNodes = new HashSet<>();
			rejectableNodes = new HashSet<>();
			indexesOfNodes = new HashMap<>();
			vlset = new HashMap<>();
			alphabet = new HashSet<>();
			redNodes = new TreeSet<>(new NodesComparator());
			notRedNodes = new TreeSet<>(new NodesComparator());
			blueNodes = new TreeSet<>(new NodesComparator());

			int lines = nextInt(br);
			words = lines;
			int alphaSize = nextInt(br);
			this.alphaSize = alphaSize;
			root = new Node(size);
			indexesOfNodes.put(size++, root);

			Node currentNode;
			Node newNode;
			String label;
			for (int line = 0; line < lines; line++) {
				currentNode = root;
				int status = nextInt(br);
				int len = nextInt(br);
				for (int i = 0; i < len; i++) {
					label = nextToken(br);
					if (i < len) {
						if (status == 1) {
							currentNode.addAcceptingPath(label);
						} else {
							currentNode.addRejectingPath(label);
						}
					}
					if (!alphabet.contains(label)) {
						alphabet.add(label);
						vlset.put(label, new HashSet<Integer>());
					}
					if (currentNode.getChildren().containsKey(label)) {
						currentNode = currentNode.getChildren().get(label);
					} else {
						vlset.get(label).add(currentNode.getNumber());
						newNode = new Node(size, label, currentNode);
						indexesOfNodes.put(size++, newNode);
						currentNode.addChild(label, newNode);
						currentNode = newNode;
					}
				}
				if (status == 1) {
					acceptableNodes.add(currentNode.getNumber());
					currentNode.setAcceptingEndings(1);
					currentNode.setStatus(Node.Status.ACCEPTABLE);
				} else {
					rejectableNodes.add(currentNode.getNumber());
					currentNode.setRejectingEndings(1);
					currentNode.setStatus(Node.Status.REJECTABLE);
				}
			}
			assert alphabet.size() == alphaSize;
		}
		initRedBlue();
	}

	public boolean isAcceptable(int number) {
		return acceptableNodes.contains(number);
	}

	public boolean isRejectable(int number) {
		return rejectableNodes.contains(number);
	}

	public Set<Node> getRedNodes() {
		return redNodes;
	}

	public Set<Node> getNotRedNodes() {
		return notRedNodes;
	}

	public Set<Node> getBlueNodes() {
		return blueNodes;
	}

	public Set<Node> getNotRedNotSinkNodes() {
		Set<Node> notRedNotSinkStates = new TreeSet<>(new NodesComparator());
		for (Node node : blueNodes) {
			if (node.getSinkType() == SINK_TYPE.NON_SINK) {
				addSubtree(notRedNotSinkStates, node);
			}
		}
		return notRedNotSinkStates;
	}

	private void addSubtree(Set<Node> states, Node node) {
		if (states.add(node)) {
			for (Node child : node.getChildren().values()) {
				addSubtree(states, child);
			}
		}
	}

	public Set<Node> getSinkStates() {
		Set<Node> sinkStates = new HashSet<>();
		for (Node node : blueNodes) {
			if (node.getSinkType() != SINK_TYPE.NON_SINK) {
				addSubtree(sinkStates, node);
			}
		}
		return sinkStates;
	}

	private void initRedBlue() {
		redNodes.add(getRoot());
		for (Node redNode : redNodes) {
			for (Node candidateBlueNode : redNode.getChildren().values()) {
				if (!redNodes.contains(candidateBlueNode)) {
					blueNodes.add(candidateBlueNode);
					notRedNodes.add(candidateBlueNode);
					initNotRed(candidateBlueNode);
				}
			}
		}
	}

	private void initNotRed(Node current) {
		for (Node notRed : current.getChildren().values()) {
			notRedNodes.add(notRed);
			initNotRed(notRed);
		}
	}

	public void updateRedBlue() {
		Set<Node> newRed = new TreeSet<>(new NodesComparator());
		for (Node red : redNodes) {
			newRed.add(red.findRepresentative());
		}
		redNodes = newRed;
		blueNodes.clear();
		Node candidateBlueNode;
		for (Node redNode : redNodes) {
			for (String label : alphabet) {
				candidateBlueNode = redNode.getMergedChild(label);
				if (candidateBlueNode != null && !redNodes.contains(candidateBlueNode)) {
					blueNodes.add(candidateBlueNode);
				}
			}
		}
	}

	public void promoteBlueToRed(Node blue) {
		blueNodes.remove(blue);
		notRedNodes.remove(blue);
		redNodes.add(blue);
		Node newBlue;
		for (String label : alphabet) {
			newBlue = blue.getMergedChild(label);
			if (newBlue != null) {
				blueNodes.add(newBlue);
			}
		}
	}

	public int getSize() {
		return redNodes.size() + getNotRedNotSinkNodes().size();
	}

	public int getRealSize() {
		return size;
	}

	public int getCountOfWords() {
		return words;
	}

	public int getAlphaSize() {
		return alphaSize;
	}

	public Node getRoot() {
		return root;
	}

	public Set<Integer> getAcceptableNodes() {
		return acceptableNodes;
	}

	public Set<Integer> getRejectableNodes() {
		return rejectableNodes;
	}

	public Set<String> getAlphabet() {
		return alphabet;
	}

	public Set<Integer> getVl(String label) {
		return vlset.get(label);
	}

	// node can be null
	public Node getNode(int i) {
		return indexesOfNodes.get(i);
	}

	public void update(Node red, Node blue) {
		notRedNodes.remove(blue);
		red.resetBackup();
		blue.resetBackup();
		if (red != blue) {
			size--;
			int includeNumber = Math.min(red.getNumber(), blue.getNumber());
			int excludeNumber = Math.max(red.getNumber(), blue.getNumber());

			if (excludeNumber == blue.getNumber()) {
				for (String label : blue.getChildren().keySet()) {
					vlset.get(label).add(includeNumber);
					vlset.get(label).remove(excludeNumber);
				}
			} else {
				for (String label : red.getChildren().keySet()) {
					vlset.get(label).add(includeNumber);
					vlset.get(label).remove(excludeNumber);
				}
			}

			red.setNumber(includeNumber);
			indexesOfNodes.put(includeNumber, red);

			if (acceptableNodes.contains(excludeNumber)) {
				acceptableNodes.add(includeNumber);
				acceptableNodes.remove(excludeNumber);
			}
			if (rejectableNodes.contains(excludeNumber)) {
				rejectableNodes.add(includeNumber);
				rejectableNodes.remove(excludeNumber);
			}

			for (int i = excludeNumber; i < size; i++) {
				Node changingNode = indexesOfNodes.get(i + 1);
				for (String label : changingNode.getChildren().keySet()) {
					vlset.get(label).add(i);
					vlset.get(label).remove(i + 1);
				}
				changingNode.setNumber(i);
				indexesOfNodes.put(i, changingNode);
				if (acceptableNodes.contains(i + 1)) {
					acceptableNodes.add(i);
					acceptableNodes.remove(i + 1);
				}
				if (rejectableNodes.contains(i + 1)) {
					rejectableNodes.add(i);
					rejectableNodes.remove(i + 1);
				}
			}
			indexesOfNodes.remove(size);

			String label;
			Node child;
			for (Iterator<Map.Entry<String, Node>> it = blue.getChildren().entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, Node> e = it.next();
				label = e.getKey();
				child = e.getValue();
				if (red.getChild(label) == null) {
					red.addChild(label, child);
				}
				it.remove();
			}
		}
	}



	private String nextToken(BufferedReader br) throws IOException {
		while (st == null || !st.hasMoreTokens()) {
			String s = br.readLine();
			if (s == null) {
				return "-1";
			}
			st = new StringTokenizer(s);
		}
		return st.nextToken();
	}

	private int nextInt(BufferedReader br) throws IOException {
		return Integer.parseInt(nextToken(br));
	}

	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("digraph Automat {\n");
		s.append("    node [shape = circle];\n");
		s.append("    rankdir=LR;\n");
		s.append("    0 [style = \"bold\"];\n");

		for (int i = 0; i < size; i++) {
			Node state = indexesOfNodes.get(i);
			if (state.isAcceptable()) {
				s.append("    ");
				s.append(state.getNumber());
				s.append(" [peripheries=2]\n");
			}
			if (state.isRejectable()) {
				s.append("    ");
				s.append(state.getNumber());
				s.append(" [peripheries=3]\n");
			}
			for (Map.Entry<String, Node> e : state.getChildren().entrySet()) {
				s.append("    ");
				s.append(state.getNumber());
				s.append(" -> ");
				s.append(e.getValue().getNumber());
				s.append(" [label = \"");
				s.append(e.getKey());
				s.append("\"];\n");
			}
		}
		s.append("}");

		return s.toString();
	}
}