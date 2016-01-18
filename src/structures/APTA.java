package structures;

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
		redNodes = new HashSet<>();
		notRedNodes = new HashSet<>();
		blueNodes = new HashSet<>();

		root = new Node(size, 1);
		indexesOfNodes.put(size++, root);
		initRedBlue();
	}


	public APTA(APTA other) {
		this.size = other.size;
		this.words = other.words;
		this.alphaSize = other.alphaSize;

		alphabet = new HashSet<>(other.getAlphabet());
		acceptableNodes = new HashSet<>(other.getRejectableNodes());
		rejectableNodes = new HashSet<>(other.getRejectableNodes());
		vlset = new HashMap<>(other.vlset);
		redNodes = new HashSet<>();
		notRedNodes = new HashSet<>();
		blueNodes = new HashSet<>();

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
			newChild = new Node(child.getNumber(), child.getDepth());
			current.addChild(label, newChild);

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
			redNodes = new HashSet<>();
			notRedNodes = new HashSet<>();
			blueNodes = new HashSet<>();

			int lines = nextInt(br);
			words = lines;
			int alphaSize = nextInt(br);
			this.alphaSize = alphaSize;
			root = new Node(size, 1);
			indexesOfNodes.put(size++, root);

			Node currentNode;
			Node newNode;
			String label;
			int depth;
			for (int line = 0; line < lines; line++) {
				currentNode = root;
				depth = 1;
				int status = nextInt(br);
				int len = nextInt(br);
				for (int i = 0; i < len; i++) {
					depth++;
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
						newNode = new Node(size, depth, label, currentNode);
						indexesOfNodes.put(size++, newNode);
						currentNode.addChild(label, newNode);
						currentNode = newNode;
					}
				}
				if (status == 1) {
					acceptableNodes.add(currentNode.getNumber());
					currentNode.setStatus(Node.Status.ACCEPTABLE);
				} else {
					rejectableNodes.add(currentNode.getNumber());
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
		blueNodes.clear();
		for (Node redNode : redNodes) {
			for (Node candidateBlueNode : redNode.getChildren().values()) {
				if (!redNodes.contains(candidateBlueNode)) {
					blueNodes.add(candidateBlueNode);
				}
			}
		}
	}

	public void promoteBlueToRed(Node blue) {
		blueNodes.remove(blue);
		notRedNodes.remove(blue);
		redNodes.add(blue);
		for (Node newBlue : blue.getChildren().values()) {
			blueNodes.add(newBlue);
		}
	}

	public int getSize() {
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

			for (Map.Entry<String, Set<Node>> e : blue.getParents().entrySet()) {
				String label = e.getKey();
				for (Node parent : e.getValue()) {
					if (indexesOfNodes.get(parent.getNumber()) == parent) {
						red.addParent(label, parent);
					}
				}
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