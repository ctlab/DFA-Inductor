import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Main {

	@Option(name = "--sizeup", aliases = { "-su" }, usage = "maximum automaton" + " size", metaVar = "<maxsimum size>",
			required = true)
	private int maxSize;

	@Option(name = "--sizedown", aliases = { "-sd" }, usage = "minimum automaton size", metaVar = "<minimum size>")
	private int minSize = 1;

	@Option(name = "--result", aliases = { "-r" }, usage = "write result automaton to this file",
			metaVar = "<result file>")
	private String resultFilePath = "ans.dot";

	@Option(name = "--strategy", aliases = { "-sb" }, usage = "symmetry breaking strategy (0 - none, 1 - BFS, " +
			"2 - clique)", metaVar = "<SB strategy>")
	private int SBStrategy = 1;

	@Option(name = "--timeout", aliases = { "-t" }, usage = "timeout", metaVar = "<timeout>")
	private int timeout = 300;

	@Option(name = "--solver", aliases = { "-sat" }, usage = "external SAT solver. using sat4j by default",
			metaVar = "<SAT solver>")
	private String externalSATSolver;

	@Option(name = "--dimacs", aliases = { "-d" }, usage = "write dimacs file with CNF to this file",
			metaVar = "<dimacs file>")
	private String dimacsFile = "dimacsFile.cnf";

	@Option(name = "--log", aliases = { "-l" }, usage = "write log to this file", metaVar = "<log>")
	private String logFile;

	@Option(name = "--noisy", aliases = { "-n" }, usage = "noisy mode", metaVar = "<noisy mode>",
			handler = BooleanOptionHandler.class, depends = { "--percent" })
	private boolean noisyMode;

	@Option(name = "--percent", aliases = { "-p" }, usage = "percent of noisy data", metaVar = "<noisy percent>",
			hidden = true, depends = { "--noisy" })
	private int p = 0;

	@Argument(usage = "dictionary files", metaVar = "files", required = true)
	private List<String> files = new ArrayList<>();

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

		for (String file : files) {
			try (InputStream is = new FileInputStream(file)) {
				logger.info("Working with file \"" + file + "\" started");

				APTA apta;
				if (noisyMode) {
					apta = new APTA(is, true);
				} else {
					apta = new APTA(is, false);
				}
				logger.info("APTA successfully builded");

				ConsistencyGraph cg;
				if (noisyMode) {
					cg = new ConsistencyGraph();
				} else {
					cg = new ConsistencyGraph(apta);
					logger.info("CG successfully builded");
				}

				for (int colors = minSize; colors <= maxSize; colors++) {
					logger.info("Try to build automaton with " + colors + " colors");
					long startTime = 0;
					try {
						new DimacsFileGenerator(apta, cg, colors, SBStrategy, p, dimacsFile).generateFile();
						logger.info("SAT problem in dimacs format successfully generated");

						SATSolver solver = new SATSolver(apta, cg, colors, dimacsFile, timeout, externalSATSolver);
						logger.info("SAT solver successfully initialized");

						logger.info("Vars in the SAT problem: " + solver.nVars());
						logger.info("Constraints in the SAT problem: " + solver.nConstraints());

						startTime = System.currentTimeMillis();
						if (solver.problemIsSatisfiable()) {
							logger.info("The automaton with " + colors + " colors was found! :)");
							logger.info("Execution time: " + (System.currentTimeMillis() - startTime) / 1000.);
							Automat automat = solver.getModel();
							try (PrintWriter pw = new PrintWriter(resultFilePath)) {
								pw.print(automat + "\n");
							} catch (IOException e) {
								logger.info("Problem with result file: " + e.getMessage());
							}
							break;
						} else {
							logger.info("The automaton with " + colors + " colors wasn't found! :(");
							logger.info("Execution time: " + (System.currentTimeMillis() - startTime) / 1000.);

						}
					} catch (ContradictionException e) {
						logger.info("The automaton with " + colors + " colors wasn't found! :(");
						logger.info("Execution time: " + (System.currentTimeMillis() - startTime) / 1000.);

					} catch (TimeoutException e) {
						logger.info("Timeout" + timeout + " seconds was reached");
					} catch (IOException e) {
						logger.warning("Some problem with generating dimacs file: " + e.getMessage());
						return;
					} catch (ParseFormatException e) {
						logger.warning("Some problem with parsing dimacs file:" +
								" " + e.getMessage());
					}
				}
				logger.info("Working with file \"" + file + "\" finished\n");
			} catch (IOException e) {
				logger.warning("Some unexpected problem with file \"" + file + "\":" + e.getMessage());
				continue;
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
		new Main().run(args);
	}

}
