package structures;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Automaton {
	private Node start;
	private List<Node> states;
	private List<Node> sinks;

	//TODO: refactor
	public Automaton(Automaton automaton) {
		this(automaton.nonSinksAmount(), automaton.sinksAmount());

		for (int i = 0; i < automaton.getStates().size(); i++) {
			Node thisNode = this.states.get(i);
			Node otherNode = automaton.getStates().get(i);
			thisNode.setStatus(otherNode.getStatus());
			for (Map.Entry<String, Node> entry : otherNode.getChildren().entrySet()) {
				thisNode.addChild(entry.getKey(), this.states.get(entry.getValue().getNumber()));
			}
		}
	}

	public Automaton(int size) {
		int cur = 0;
		this.start = new Node(cur++);
		this.sinks = new ArrayList<>();
		this.states = new ArrayList<>();
		this.states.add(this.start);

		while (cur < size) {
			this.states.add(new Node(cur++));
		}
	}

	public Automaton(int size, int sinks) {
		this(size);

		int cur = nonSinksAmount();
		for (int i = 0; i < sinks; i++) {
			this.sinks.add(new Node(cur++));
		}
	}

	public Automaton(File file) throws IOException {
		this.start = new Node(0);
		this.states = new ArrayList<>();
		this.states.add(this.start);
		this.sinks = new ArrayList<>();

		try (BufferedReader automatonBR = new BufferedReader(new FileReader(file))) {
			Pattern transitionPattern = Pattern.compile("\\s+(\\d+) -> (\\d+) \\[label = \\\"([a-zA-Z0-9-_]+)\\\"\\]\\s*;");
			Pattern acceptingPattern = Pattern.compile("\\s+(\\d+) \\[peripheries=2\\]\\s*");
			Pattern sinkPattern = Pattern.compile("\\s+(\\d+) \\[shape = square\\];\\s*");
			Pattern acceptingSinkPattern = Pattern.compile("\\s+(\\d+) \\[peripheries=2\\] \\[shape = square\\];\\s*");

			String line;
			Matcher matcher;
			while ((line = automatonBR.readLine()) != null) {
				if ((matcher = transitionPattern.matcher(line)).matches()) {
					addTransition(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
							matcher.group(3));
				} else if ((matcher = acceptingPattern.matcher(line)).matches()) {
					getState(Integer.parseInt(matcher.group(1))).setStatus(Node.Status.ACCEPTABLE);
				} else if ((matcher = acceptingSinkPattern.matcher(line)).matches()) {
					getState(Integer.parseInt(matcher.group(1))).setStatus(Node.Status.ACC_SINK);
				} else if ((matcher = sinkPattern.matcher(line)).matches()) {
					getState(Integer.parseInt(matcher.group(1))).setStatus(Node.Status.REJ_SINK);
				}
			}
			Iterator<Node> iter = states.iterator();
			Node cur;
			while(iter.hasNext()) {
				cur = iter.next();
				if (cur.getStatus() == Node.Status.ACC_SINK || cur.getStatus() == Node.Status.REJ_SINK) {
					sinks.add(cur);
					iter.remove();
				}
			}
			for (Node node : states) {
				if (node.getStatus() == Node.Status.COMMON) {
					node.setStatus(Node.Status.REJECTABLE);
				}
			}
		}
	}

	public Node getStart() {
		return start;
	}

	public Node getState(int i) {
		return states.get(i);
	}

	public List<Node> getStates() {
		return states;
	}

	public Node getSink(int i) {
		return sinks.get(i);
	}

	public List<Node> getSinks() {
		return sinks;
	}

	public int size() {
		return states.size();
	}

	public int sinksAmount() {
		return sinks.size();
	}

	public int nonSinksAmount() {
		return states.size();
	}

	public void addTransition(int from, int to, String label) {
		if (from >= states.size()) {
			addState(from);
		}
		if (to >= states.size()) {
			addState(to);
		}
		Node fromNode = states.get(from);
		Node toNode = states.get(to);

		fromNode.addChild(label, toNode);
	}

	public void addTransition2Sink(int from, int to, String label) {
		if (from >= states.size()) {
			addState(from);
		}
		if (to >= sinks.size()) {
			throw new IllegalArgumentException("There are no so much sinks");
		}
		Node fromNode = states.get(from);
		Node toNode = sinks.get(to);

		fromNode.addChild(label, toNode);
	}

	public Node.Status proceedWord(List<String> word) {
		Node curNode = start;
		Node child;
		for (String label : word) {
			child = curNode.getChild(label);
		if (child.getStatus() == Node.Status.ACC_SINK) {
				return Node.Status.ACCEPTABLE;
			} else if (child.getStatus() == Node.Status.REJ_SINK) {
				return Node.Status.REJECTABLE;
			}
			curNode = child;
		}
		return curNode.getStatus();
	}

	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("digraph Automat {\n");
		s.append("    node [shape = circle];\n");
		s.append("    0 [style = \"bold\"];\n");

		for (Node state : states) {
			if (state.isAcceptable()) {
				s.append("    ");
				s.append(state.getNumber());
				s.append(" [peripheries=2]\n");
			}
			for (Entry<String, Node> e : state.getChildren().entrySet()) {
				s.append("    ");
				s.append(state.getNumber());
				s.append(" -> ");
				s.append(e.getValue().getNumber());
				s.append(" [label = \"");
				s.append(e.getKey());
				s.append("\"];\n");
			}
		}
		for (Node sink : sinks) {
			s.append("    ");
			s.append(sink.getNumber());
			if (sink.getSinkType() == Node.SINK_TYPE.ACCEPTING_SINK) {
				s.append(" [peripheries=2]");
			}
			s.append(" [shape = square];\n");
		}
		s.append("}");

		return s.toString();
	}

	private String enumerate() {
		Queue<Node> queue = new LinkedList<>();
		boolean[] visited = new boolean[this.nonSinksAmount()];
		List<String> alphabet = new ArrayList<>(this.getStart().getChildren().keySet());
		Collections.sort(alphabet);
		int cur_num = 0;

		Map<Integer, Integer> enumeration = new HashMap<>();

		queue.add(this.getStart());
		visited[this.getStart().getNumber()] = true;
		Node cur;
		List<Integer> hash = new ArrayList<>();
		while (!queue.isEmpty()) {
			cur = queue.remove();
			enumeration.put(cur.getNumber(), cur_num++);
			for (String label : alphabet) {
				hash.add(cur.getChild(label).getNumber());
			}
			for (String label : alphabet) {
				Node child = cur.getChild(label);
				if (!visited[child.getNumber()]) {
					visited[child.getNumber()] = true;
					queue.add(child);
				}
			}
		}
		for (int i = 0; i < hash.size(); i++) {
			hash.set(i, enumeration.get(hash.get(i)));
		}
		return hash.toString();
	}

	@Override
	public int hashCode() {
		return enumerate().hashCode();
	}

	@Override
	public boolean equals(Object arg) {
		Automaton obj = (Automaton) arg;
		return this.hashCode() == obj.hashCode();
	}

	private boolean addState(int number) {
		int cur = states.size();
		if (cur <= number) {
			while (cur <= number) {
				this.states.add(new Node(cur++));
			}
			return true;
		}
		return false;
	}
}
