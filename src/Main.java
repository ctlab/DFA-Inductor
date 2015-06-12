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
import java.util.Locale;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Main {

	@Option(name = "--sizeup", aliases = {"-su"}, usage = "maximum automaton" + " size", metaVar = "<maxsimum size>",
			required = true)
	private int maxSize;

	@Option(name = "--sizedown", aliases = {"-sd"}, usage = "minimum automaton size", metaVar = "<minimum size>")
	private int minSize = 1;

	@Option(name = "--result", aliases = {"-r"}, usage = "write result automaton to this file with extension .dot",
			metaVar = "<result file>")
	private String resultFilePath = "ans";

	@Option(name = "--strategy", aliases = {"-sb"}, usage = "symmetry breaking strategy (0 - none, 1 - BFS, " +
			"2 - DFS" + "3 - clique)", metaVar = "<SB strategy>")
	private int SBStrategy = 1;

	@Option(name = "--timeout", aliases = {"-t"}, usage = "timeout", metaVar = "<timeout>")
	private int timeout = 600;

	@Option(name = "--solver", aliases = {"-sat"}, usage = "external SAT solver. using sat4j by default",
			metaVar = "<SAT solver>")
	private String externalSATSolver;

	@Option(name = "--dimacs", aliases = {"-d"}, usage = "write dimacs file with CNF to this file",
			metaVar = "<dimacs file>")
	private String dimacsFile = "dimacsFile.cnf";

	@Option(name = "--log", aliases = {"-l"}, usage = "write log to this file", metaVar = "<log>")
	private String logFile;

	@Option(name = "--percent", aliases = {"-p"}, usage = "percent of noisy data", metaVar = "<noisy percent>")
	private int p = 0;

	@Option(name = "--findall", aliases = {"-a"}, usage = "find all mode", metaVar = "<find all>",
			handler = BooleanOptionHandler.class)
	private boolean findAllMode;

	@Option(name = "--find", aliases = {"-f"}, usage = "find COUNT or less", metaVar = "<find>")
	private int findCount = 0;

	@Option(name = "--atmostone", aliases = {"-amo"}, usage = "bimander or pairwise at most one", metaVar = "<amo>")
	private boolean isBimander;

	@Option(name = "--backtracking", aliases = {"-bt"}, usage = "using backtracking instead of SAT approach",
			forbids = {"--strategy", "-sb", "--solver", "-sat", "--dimacs", "-d", "--atmostone", "-amo", "--percent",
					"-p"})
	private boolean backtrackingMode;

	@Argument(usage = "dictionary file", metaVar = "<file>", required = true)
	private String file;

	private static Logger logger = Logger.getLogger("Logger");

	private void launch(String... args) {
		long fullStartTime = System.currentTimeMillis();
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.print("Usage:");
			parser.printSingleLineUsage(System.err);
			System.err.println();
			parser.printUsage(System.err);
			return;
		}

		boolean noisyMode = p > 0;
		findAllMode |= findCount > 0;
		if (SBStrategy == 3 && noisyMode) {
			System.err.println("You can't use CLIQUE symmetry breaking strategy during solving " +
					"noisy DFA building problem");
		}

		if (logFile != null) {
			try {
				FileHandler fh = new FileHandler(logFile, false);
				logger.addHandler(fh);
				fh.setFormatter(new SimpleFormatter());
				logger.setUseParentHandlers(false);
				System.out.println("Log file: " + logFile);
			} catch (Exception e) {
				System.err.println("Problem with the log file: " + logFile + ". " + e.getMessage());
				return;
			}
		}

		int curDFA = 1;

		try (InputStream is = new FileInputStream(file)) {
			logger.info("Working with file \"" + file + "\" started");

			APTA apta = new APTA(is);
			logger.info("APTA was successfully built");

			logger.info("APTA size: " + apta.getSize());
			logger.info("Ends in APTA: " + (apta.getAcceptableNodes().size() + apta.getRejectableNodes().size()));
			logger.info("Count of words: " + apta.getCountOfWords());
			if (!backtrackingMode) {
				ConsistencyGraph cg = new ConsistencyGraph(apta, noisyMode);
				if (!noisyMode) {
					logger.info("CG was successfully built");
				}
				if (!noisyMode) {
					cg.findClique();
					int cliqueSize = cg.getCliqueSize();
					minSize = Math.max(cliqueSize, minSize);
					logger.info("Clique was found. Its size is " + cliqueSize + ".");
					logger.info("Searching will be started from size " + minSize + ".");
				}
				boolean found = false;
				for (int colors = minSize; colors <= maxSize && !found; colors++) {
					logger.info("Try to build automaton with " + colors + " colors");
					long startTime = 0;
					try {
						if ((System.currentTimeMillis() - fullStartTime) / 1000. > timeout) {
							if (colors == minSize) {
								throw new TimeoutException();
							}
							break;
						}
						DimacsFileGenerator dfg = new DimacsFileGenerator(apta, cg, colors, SBStrategy, p, dimacsFile);
						dfg.generateFile(isBimander);
						logger.info("SAT problem in dimacs format successfully generated");
						do {
							SATSolver solver = new SATSolver(apta, colors, dimacsFile,
									(int) (timeout - ((System.currentTimeMillis() - fullStartTime)) / 1000.), externalSATSolver);
							logger.info("SAT solver successfully initialized");

							logger.info("Vars in the SAT problem: " + solver.nVars());
							logger.info("Constraints in the SAT problem: " + solver.nConstraints());

							startTime = System.currentTimeMillis();
							String DFAnumber = " ";
							if (findAllMode) {
								DFAnumber = " number " + String.valueOf(curDFA) + " ";
							}
							if (solver.problemIsSatisfiable()) {
								found = true;
								logger.info("The automaton" + DFAnumber + "with " + colors + " colors was found! :)");
								logger.info("Execution time: " + (System.currentTimeMillis() - startTime) / 1000.);
								int[] model = null;
								try {
									model = solver.getModel();
								} catch (Exception e) {
									logger.warning("Some problem with SATSolver. Shouldn't be here. " +
											"Exception: " + e.getMessage());
								}
								Automaton automaton = AutomatonBuilder.build(model, dfg, apta, colors, noisyMode);
								String fullResultFilePath = resultFilePath;
								if (findAllMode) {
									fullResultFilePath += fineNumber(curDFA);
								}
								fullResultFilePath += ".dot";
								try (PrintWriter pw = new PrintWriter(fullResultFilePath)) {
									pw.print(automaton + "\n");
								} catch (IOException e) {
									logger.info("Problem with result file: " + e.getMessage());
								}
								if (findAllMode) {
									dfg.banSolution(automaton, model);
									curDFA++;
									if (findCount > 0 && curDFA > findCount) {
										break;
									}
								} else {
									break;
								}
							} else {
								if (findAllMode && found) {
									logger.info("No more automatons with " + colors + " colors were found! :(");
								} else {
									logger.info("The automaton with " + colors + " colors wasn't found! :(");
								}
								logger.info("Execution time: " + (System.currentTimeMillis() - startTime) / 1000.);
								break;
							}
						} while (true);
					} catch (ContradictionException e) {
						logger.info("The automaton with " + colors + " colors wasn't found! :(");
						logger.info("Execution time: " + (System.currentTimeMillis() - startTime) / 1000.);
					} catch (TimeoutException e) {
						logger.info("Timeout " + timeout + " seconds was reached");
						logger.info("Execution time: " + timeout);
						break;
					} catch (IOException e) {
						logger.warning("Some problem with generating dimacs file: " + e.getMessage());
						return;
					} catch (ParseFormatException e) {
						logger.warning("Some problem with parsing dimacs file: " + e.getMessage());
					}
				}
			} else {
				ConsistencyGraph cg = new ConsistencyGraph(apta, noisyMode);
				logger.info("CG was successfully built");
				cg.findClique();
				int cliqueSize = cg.getCliqueSize();
				minSize = Math.max(cliqueSize, minSize);
				logger.info("Clique was found. Its size is " + cliqueSize + ".");
				logger.info("Searching will be started from size " + minSize + ".");
				boolean found = false;
				for (int colors = minSize; colors <= maxSize && !found; colors++) {
					logger.info("Try to build automaton with " + colors + " colors");
					long startTime = 0;
					try {
						if ((System.currentTimeMillis() - fullStartTime) / 1000. > timeout) {
							if (colors == minSize) {
								throw new TimeoutException();
							}
							break;
						}
						BacktrackingSolver solver = new BacktrackingSolver(apta, colors,
								(int) (timeout - ((System.currentTimeMillis() - fullStartTime)) / 1000.), findAllMode);
						logger.info("Backtracking solver successfully initialized");
						startTime = System.currentTimeMillis();
						if (solver.problemIsBacktrackinging()) {
							found = true;
							Set<Automaton> dfas = solver.getAnswer();
							if (dfas.size() == 1) {
								logger.info("The automaton" + " with " + colors + " colors was found! :)");
							} else {
								logger.info(dfas.size() + " automatons" + " with " + colors + " colors were found! :)");
							}
							logger.info("Execution time: " + (System.currentTimeMillis() - startTime) / 1000.);
							for (Automaton automaton : dfas) {
								String fullResultFilePath = resultFilePath;
								if (findAllMode) {
									fullResultFilePath += fineNumber(curDFA++);
								}
								fullResultFilePath += ".dot";
								try (PrintWriter pw = new PrintWriter(fullResultFilePath)) {
									pw.print(automaton + "\n");
								} catch (IOException e) {
									logger.info("Problem with result file: " + e.getMessage());
								}
							}
							if (timeout < (System.currentTimeMillis() - fullStartTime) / 1000) {
								throw new TimeoutException();
							}
						} else {
							logger.info("The automaton with " + colors + " colors wasn't found! :(");
							logger.info("Execution time: " + (System.currentTimeMillis() - startTime) / 1000.);
						}
					} catch (TimeoutException e) {
						logger.info("Timeout " + timeout + " seconds was reached");
						logger.info("Execution time: " + timeout);
						break;
					}
				}
			}
			logger.info("Working with file \"" + file + "\" finished\n");
		} catch (IOException e) {
			logger.warning("Some unexpected problem with file \"" + file + "\":" + e.getMessage());
		}
		logger.info("Full time: " + (System.currentTimeMillis() - fullStartTime) / 1000.);
	}

	private String fineNumber(int number) {
		return (number < 10) ? "000" + number :
				number < 100 ? "00" + number :
				number < 1000 ? "0" + number : String.valueOf(number);
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
