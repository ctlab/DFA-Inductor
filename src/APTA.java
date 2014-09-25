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

	private StringTokenizer st = null;

	public APTA() {
		acceptableNodes = new HashSet<>();
		rejectableNodes = new HashSet<>();
		indexesOfNodes = new HashMap<>();
		alphabet = new HashSet<>();
		size = 0;
		root = new Node(size);
		indexesOfNodes.put(size++, root);
	}

	public APTA(InputStream is) throws IOException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			size = 0;
			acceptableNodes = new HashSet<>();
			rejectableNodes = new HashSet<>();
			indexesOfNodes = new HashMap<>();
			alphabet = new HashSet<>();

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
					alphabet.add(label);
					if (currentNode.getChildren().containsKey(label)) {
						currentNode = currentNode.getChildren().get(label);
					} else {
						newNode = new Node(size, label, currentNode);
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
	}

	public boolean isAcceptable(int number) {
		return acceptableNodes.contains(number);
	}

	public boolean isRejectable(int number) {
		return rejectableNodes.contains(number);
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

	// node can be null
	public Node getNode(int i) {
		return indexesOfNodes.get(i);
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
}