import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;

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

	@Option(name = "--result", aliases = {"-r"}, usage = "write result automaton to these files <name>.nnn",
			metaVar = "<result files>")
	private String resultFilePath = "test";

	@Option(name = "--filesCount", aliases = {"-fc"}, usage = "count of files", metaVar = "<files count>")
	private int files = 1;

	@Option(name = "--log", aliases = {"-l"}, usage = "write log to this file", metaVar = "<log>")
	private String logFile;

	@Option(name = "--percent", aliases = {"-p"}, usage = "percent of noisy data", metaVar = "<noisy percent>")
	private int p = 0;

	private static Logger logger = Logger.getLogger("Logger");
	
	private boolean noisyMode;

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

		noisyMode = p > 0 ? true : false;

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
		for (int file = 1; file <= files; file++) {
			String suffix = ".";
			if (file < 10) {
				suffix += "00" + file;
			} else if (file < 100) {
				suffix += "0" + file;
			} else {
				suffix += file;
			}

			String fileName = resultFilePath + suffix;
			logger.info("Starting generating file " + fileName);

			List<Node> nodes = new ArrayList<>();
			for (int number = 0; number < size; number++) {
				nodes.add(new Node(number));
			}

			for (int number = 1; number < size; number++) {
				Node curNode = nodes.get(number);
				int parentNum;
				do {
					parentNum = random.nextInt(number);
				} while (nodes.get(parentNum).getChild("0") != null && nodes.get(parentNum).getChild("1") != null);
				Node parentNode = nodes.get(parentNum);
				if (parentNode.getChild("0") != null) {
					parentNode.addChild("1", curNode);
					curNode.addParent("1", parentNode);
					continue;
				}
				if (parentNode.getChild("1") != null) {
					parentNode.addChild("0", curNode);
					curNode.addParent("0", parentNode);
					continue;
				}
				String label = random.nextInt(2) == 1 ? "1" : "0";
				parentNode.addChild(label, curNode);
				curNode.addParent(label, parentNode);
			}

			for (int number = 0; number < size; number++) {
				Node curNode = nodes.get(number);

				int toByZero = random.nextInt(size);
				int toByOne = random.nextInt(size);
				if (curNode.getChild("0") == null) {
					curNode.addChild("0", nodes.get(toByZero));
					nodes.get(toByZero).addParent("0", curNode);
				}
				if (curNode.getChild("1") == null) {
					curNode.addChild("1", nodes.get(toByOne));
					nodes.get(toByOne).addParent("1", curNode);
				}

				Node.Status status;
				if (random.nextInt(2) == 1) {
					status = Node.Status.ACCEPTABLE;
				} else {
					status = Node.Status.REJECTABLE;
				}
				curNode.setStatus(status);
			}

			logger.info("Generating words for: " + fileName);
			try (PrintWriter pw = new PrintWriter(new File(fileName))) {
				pw.println(words + " " + 2);

				Set<String> wordsSet = new HashSet<>();

				String path;
				Node curNode;

				int count = 2;
				int length = 3;
				int word = 0;

				int noisyWords = 0;
				if (noisyMode) {
					noisyWords = words / 100 * p;
				}

				while (word < words) {
					if (word == count) {
						count *= 2;
						length++;
					}
					path = "";
					curNode = nodes.get(0);
					for (int letter = 0; letter < length; letter++) {
						String label = random.nextInt(2) == 1 ? "1" : "0";
						curNode = curNode.getChild(label);
						path += label + " ";
					}
					if (curNode.isAcceptable()) {
						if (words < noisyWords) {
							path = "0 " + length + " " + path;
						} else {
							path = "1 " + length + " " + path;
						}
					} else {
						if (words < noisyWords) {
							path = "1 " + length + " " + path;
						} else {
							path = "0 " + length + " " + path;
						}
					}
					if (!wordsSet.contains(path)) {
						wordsSet.add(path);
						pw.println(path);
						word++;
					}
				}
			} catch (FileNotFoundException e) {
				logger.info("Some problem with result file " + fileName + ": " + e.getMessage());
				e.printStackTrace();
			}


		}
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
