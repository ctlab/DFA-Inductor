import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class DictionaryGeneratorMain {

	@Option(name = "--size", aliases = {"-s"}, usage = "size of automaton", metaVar = "<size>", required = true)
	private int size;

	@Option(name = "--wordsCount", aliases = {"-wc"}, usage = "number of words", metaVar = "<words count>",
			required = true)
	private int words;

	@Option(name = "--result", aliases = {"-r"}, usage = "write result automaton to this file",
			metaVar = "<result file>")
	private String resultFilePath = "test.dct";

	@Option(name = "--log", aliases = {"-l"}, usage = "write log to this file", metaVar = "<log>")
	private String logFile;

	@Option(name = "--percent", aliases = {"-p"}, usage = "percent of noisy data", metaVar = "<noisy percent>")
	private int p = 0;

	@Option(name = "--alphabet", aliases = {"-a"}, usage = "alphabet size", metaVar = "<alphabet size>")
	private int alphabetSize = 2;

	@Option(name = "--startlenght", aliases = {"-sl"}, usage = "length of the smallest generated word",
			metaVar = "<smallest word length>")
	private int startLength = 3;

	private static Logger logger = Logger.getLogger("Logger");

	private void launch(String... args) {
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.print("Usage ");
			parser.printSingleLineUsage(System.err);
			System.err.println();
			parser.printUsage(System.err);
			return;
		}

		boolean noisyMode = p > 0;

		if (logFile != null) {
			try {
				FileHandler fh = new FileHandler(logFile, false);
				logger.addHandler(fh);
				fh.setFormatter(new SimpleFormatter());
				logger.setUseParentHandlers(false);
				System.out.println("Log file: " + logFile);
			} catch (Exception e) {
				System.err.println("Problem with log file: " + logFile + ". " + e.getMessage());
				return;
			}
		}

		Random random = new Random();
		logger.info("Starting generating file " + resultFilePath);
		Automaton automaton = new Automaton(size);
		for (int number = 1; number < size; number++) {
			int parentNum;
			do {
				parentNum = random.nextInt(number);
			} while (!hasFreeEdge(automaton, parentNum));
			Node parentNode = automaton.getState(parentNum);
			int label;
			do {
				label = random.nextInt(alphabetSize);
			} while (parentNode.getChild(String.valueOf(label)) != null);
			automaton.addTransition(parentNum, number, String.valueOf(label));
		}

		for (int number = 0; number < size; number++) {
			Node curNode = automaton.getState(number);

			for (int i = 0; i < alphabetSize; i++) {
				if (curNode.getChild(String.valueOf(i)) == null) {
					automaton.addTransition(number, random.nextInt(size), String.valueOf(i));
				}
			}

			Node.Status status;
			if (random.nextInt(2) == 1) {
				status = Node.Status.ACCEPTABLE;
			} else {
				status = Node.Status.REJECTABLE;
			}
			curNode.setStatus(status);
		}
		logger.info("Automaton succesfully generated: \n" + automaton.toString());
		logger.info("Generating words for: " + resultFilePath);
		try (PrintWriter pw = new PrintWriter(new File(resultFilePath))) {
			pw.println(words + " " + alphabetSize);

			Set<String> wordsSet = new HashSet<>();
			StringBuilder path;
			Node curNode;

			int WordsLessThenCurrentLengthCount = 2;
			int length = startLength;
			int currentWordNumber = 0;

			int noisyWords = 0;
			int noisyWordsCount = noisyMode ? (int) Math.round((words / 100.0) * p) : 0;

			while (currentWordNumber < words) {
				if (currentWordNumber == WordsLessThenCurrentLengthCount) {
					WordsLessThenCurrentLengthCount *= 2;
					length++;
				}
				path = new StringBuilder();
				curNode = automaton.getStart();
				for (int letter = 0; letter < length; letter++) {
					String label = String.valueOf(random.nextInt(alphabetSize));
					curNode = curNode.getChild(label);
					path.append(label).append(" ");
				}
				if (curNode.isAcceptable()) {
					path = new StringBuilder("1 ").append(length).append(" ").append(path);
				} else {
					path = new StringBuilder("0 ").append(length).append(" ").append(path);
				}
				if (!wordsSet.contains(path.toString())) {
					wordsSet.add(path.toString());
					currentWordNumber++;
				}
			}

			List<String> wordsList = new ArrayList<>();
			wordsList.addAll(wordsSet);
			Collections.shuffle(wordsList);
			Set<String> changedWords = new HashSet<>();
			Iterator<String> iter = wordsList.iterator();
			while (iter.hasNext()) {
				String word = iter.next();
				if (noisyWords < noisyWordsCount) {
					changedWords.add(((word.charAt(0) == '1') ? "0" : "1") + word.substring(1));
					noisyWords++;
					iter.remove();
				} else {
					break;
				}
			}

			wordsList.addAll(changedWords);
			Collections.shuffle(wordsList);
			for (String word : wordsList) {
				pw.println(word);
			}
		} catch (FileNotFoundException e) {
			logger.info("Some problem with result file " + resultFilePath + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	private boolean hasFreeEdge(Automaton automaton, int nodeNumber) {
		boolean ans = false;
		for (int i = 0; i < alphabetSize; i++) {
			ans |= (automaton.getState(nodeNumber).getChild(String.valueOf(i)) == null);
		}
		return ans;
	}

	private void run(String... args) {
		Locale.setDefault(Locale.US);
		try {
			launch(args);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String... args) {
		new DictionaryGeneratorMain().run(args);
	}

}
