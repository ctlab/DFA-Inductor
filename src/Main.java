import EDSM.EDSMWorker;
import EDSM.EDSMWorker.EDSMHeuristic;
import algorithms.AutomatonBuilder;
import algorithms.BacktrackingSolver;
import algorithms.DimacsFileGenerator;
import algorithms.SATSolver;
import misc.Settings;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;
import structures.APTA;
import structures.Automaton;
import structures.ConsistencyGraph;

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
			"2 - DFS" + ", 3 - clique)", metaVar = "<SB strategy>")
	private int SBStrategy = 1;

	@Option(name = "--timeout", aliases = {"-t"}, usage = "timeout", metaVar = "<timeout>")
	private int timeout = 0;

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

	@Option(name = "--iterativemode", aliases = {"-itm"}, usage = "iterative mode for findK; iterative SAT-solver" +
			"should be used", metaVar = "<iterative mode>", hidden = true, handler = BooleanOptionHandler.class)
	private boolean iterativeMode = false;

	@Option(name = "--iterativesolver", aliases = {"-its"}, usage = "iterative solver",
			metaVar = "<iterative solver>", hidden = true)
	private boolean iterativeSolver = false;

	@Option(name = "--loop", hidden = true, usage = "fixing free transitions into loop",
			handler = BooleanOptionHandler.class)
	private boolean loopMode = false;

	@Option(name = "--atmostone", aliases = {"-amo"}, usage = "at most one constraints encoding." +
			"1 - pairwise, 2 - binary, 3 - commander where m=sqrt(n)," +
			"4 - commander where m=n/2, 5 - product, 6 - sequential," +
			"7 - bimander where m=sqrt(n), 8 - bimander where m=n/2",
			metaVar = "<amo>")
	private int amo = 1;

	@Option(name = "--backtracking", aliases = {"-bt"}, usage = "using backtracking instead of SAT approach",
			forbids = {"--strategy", "-sb", "--solver", "-sat", "--dimacs", "-d", "--atmostone", "-amo", "--percent",
					"-p"}, handler = BooleanOptionHandler.class)
	private boolean backtrackingMode;

	@Option(name = "--pathslowerbound", aliases = {"-plb"}, usage = "lower bound for accepting intersections" +
			"of dictionary words with a node for fanout heuristic")
	private int pathsLowerBound = 25;

	@Option(name = "--symbolpathslowerbound", aliases = {"-splb"}, usage = "lower bound for accepting" +
			"intersections of dictionary words with a node by a symbol")
	private int pathsOnSymbolLowerBound = 10;

	@Option(name = "--runs", usage = "number of EDSM runs", metaVar = "<runs>")
	private int runs = 100;

	@Option(name = "--heuristic", aliases = {"-h"}, usage = "heuristic function for EDSM: 0 - no EDSM," +
			"1 - number of same status merges, 2 - overlap in fanout")
	private int heuristic = 0;

	@Option(name = "--aptabound", aliases = {"-ab"}, usage = "upper bound for states in the partially" +
			"identifiend apta")
	private int aptaBound = 1000;

	@Option(name = "--redbound", aliases = {"-rb"}, usage = "lower bound for number of red states " +
			"in the partially identified apta")
	private int redBound = 50;

	@Option(name = "--randomgreedy", aliases = {"-random"}, usage = "random greedy mode; if it is not" +
			"set - non-random greedy", handler = BooleanOptionHandler.class)
	private boolean randomGreedyMode = false;

	@Option(name = "--sinkmode", aliases = {"-sink"}, usage = "sink mode: 0 - do not use sinks; 1 - " +
			"use reject sink only; 2 - use accept and reject sinks; 3 - use low statistical information sink only;" +
			"4 - use rejecet and low statistical information sink; " +
			"5 - use accept, reject and low statistical information sinks")
	private int sinksMode = 0;

	@Option(name = "--extendfirst", aliases = {"-ef"}, usage = "if set extend red states first if there is exist" +
			"a blue state which cannot be merged with any red, otherwise extend only if no more possible merges" +
			"exist", handler = BooleanOptionHandler.class)
	private boolean extendFirst = true;

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
		Settings.setSettings(
				findAllMode,
				iterativeMode,
				iterativeSolver,
				loopMode,
				backtrackingMode,
				randomGreedyMode,
				extendFirst,
				resultFilePath,
				externalSATSolver,
				dimacsFile,
				logFile,
				file,
				maxSize,
				minSize,
				SBStrategy,
				timeout,
				p,
				findCount,
				amo,
				pathsLowerBound,
				pathsOnSymbolLowerBound,
				runs,
				heuristic,
				aptaBound,
				redBound,
				sinksMode);
		Settings.EDSM_MODE = heuristic > 0;
		boolean EDSMMode = Settings.EDSM_MODE;
		switch (heuristic) {
			case 1:
				Settings.EDSM_HEURISTIC = EDSMHeuristic.Status;
				break;
			case 2:
				Settings.EDSM_HEURISTIC = EDSMHeuristic.Fanout;
				break;
		}

		Settings.NOISY_MODE = p > 0;
		boolean noisyMode = Settings.NOISY_MODE;
		Settings.FIND_ALL_MODE |= Settings.FIND_K > 0;
		boolean findAllMode = Settings.FIND_ALL_MODE;
		Settings.LOOP_MODE |= Settings.FIND_ALL_MODE;
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

			if (EDSMMode) {
				logger.info("EDSM greedy preprocessing started");
				EDSMWorker worker = new EDSMWorker(apta);
				worker.startMerging();
				logger.info("EDSM greedy preprocessing finished");
				logger.info("New APTA size: " + apta.getSize());
				logger.info("Number of red states: " + apta.getRedNodes().size());
			}

			if (!backtrackingMode) {
				if (!noisyMode) {
					logger.info("CG building started");
				}
				ConsistencyGraph cg = new ConsistencyGraph(apta);
				if (!noisyMode) {
					logger.info("CG was successfully built");
				}
				if (!noisyMode) {
					cg.findClique();
					int cliqueSize = cg.getCliqueSize();
					Settings.MINIMUM_SIZE = Math.max(cliqueSize, Settings.MINIMUM_SIZE);
					minSize = Settings.MINIMUM_SIZE;
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
						DimacsFileGenerator dfg = new DimacsFileGenerator(apta, cg, colors);
						dfg.generateFile();
						logger.info("SAT problem in dimacs format successfully generated");
						SATSolver solver = null;
						do {
							if (!(findAllMode && iterativeMode && curDFA > 1)) {
								solver = new SATSolver(apta, colors,
										(int) (timeout - ((System.currentTimeMillis() - fullStartTime)) / 1000.));
							}
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
								Automaton automaton = AutomatonBuilder.build(model, dfg, apta, colors);
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
									logger.info("No more automatons with " + colors + " colors were found! Total: "
											+ (curDFA - 1) + ".");
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
				ConsistencyGraph cg = new ConsistencyGraph(apta);
				logger.info("CG was successfully built");
				cg.findClique();
				int cliqueSize = cg.getCliqueSize();
				Settings.MINIMUM_SIZE = Math.max(cliqueSize, Settings.MINIMUM_SIZE);
				minSize = Settings.MINIMUM_SIZE;
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
								(int) (timeout - ((System.currentTimeMillis() - fullStartTime)) / 1000.));
						logger.info("Backtracking solver successfully initialized");
						startTime = System.currentTimeMillis();
						if (solver.problemIsBacktrackinging()) {
							found = true;
							Set<Automaton> dfas = solver.getAnswer();
							for (Automaton automaton : dfas) {
								String DFAnumber = " number " + String.valueOf(curDFA) + " ";
								logger.info("The automaton" + DFAnumber + "with " + colors + " colors was found! :)");
							}
							logger.info("No more automatons with " + colors + " colors were found! Total: "
									+ dfas.size() + ".");
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
