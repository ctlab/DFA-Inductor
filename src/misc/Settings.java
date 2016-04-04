package misc;

import EDSM.EDSMWorker;

public final class Settings {

	private Settings(){};

	public static boolean FIND_ALL_MODE;
	public static boolean ITERATIVE_MODE;
	public static boolean ITERATIVE_SOLVER;
	public static boolean LOOP_MODE;
	public static boolean BACKTRACKING_MODE;
	public static boolean RANDOM_GREEDY_MODE;
	public static boolean EDSM_MODE;
	public static boolean NOISY_MODE;
	public static boolean EXTEND_FIRST;

	public static String RESULT_FILE_PATH;
	public static String EXTERNAL_SAT_SOLVER;
	public static String DIMACS_FILE;
	public static String LOG_FILE;
	public static String OUTPUT;

	public static int MAXIMUM_SIZE;
	public static int MINIMUM_SIZE;
	public static int SB_STRATEGY;
	public static int TIMEOUT;
	public static int ERRORS_PERCENT;
	public static int FIND_K;
	public static int AT_MOST_ONE_OPTION;
	public static int PATHS_LOWER_BOUND;
	public static int PATHS_ON_SYMBOL_LOWER_BOUND;
	public static int RUNS;
	public static int HEURISTIC;
	public static int APTA_BOUND;
	public static int RED_BOUND;
	public static int SINKS_MODE;


	public static EDSMWorker.EDSMHeuristic EDSM_HEURISTIC = null;

	public static void setSettings(
			boolean FIND_ALL_MODE,
			boolean ITERATIVE_MODE,
			boolean ITERATIVE_SOLVER,
			boolean LOOP_MODE,
			boolean BACKTRACKING_MODE,
			boolean RANDOM_GREEDY_MODE,
			boolean EXTEND_FIRST,
			String RESULT_FILE_PATH,
			String EXTERNAL_SAT_SOLVER,
			String DIMACS_FILE,
			String LOG_FILE,
			String OUTPUT,
			int MAXIMUM_SIZE,
			int MINIMUM_SIZE,
			int SB_STRATEGY,
			int TIMEOUT,
			int ERRORS_PERCENT,
			int FIND_K,
			int AT_MOST_ONE_OPTION,
			int PATHS_LOWER_BOUND,
			int PATHS_ON_SYMBOL_LOWER_BOUND,
			int RUNS,
			int HEURISTIC,
			int APTA_BOUND,
			int RED_BOUND,
			int SINKS_MODE
	) {
		Settings.FIND_ALL_MODE = FIND_ALL_MODE;
		Settings.ITERATIVE_MODE = ITERATIVE_MODE;
		Settings.ITERATIVE_SOLVER = ITERATIVE_SOLVER;
		Settings.LOOP_MODE = LOOP_MODE;
		Settings.BACKTRACKING_MODE = BACKTRACKING_MODE;
		Settings.RANDOM_GREEDY_MODE = RANDOM_GREEDY_MODE;
		Settings.EXTEND_FIRST = EXTEND_FIRST;
		Settings.RESULT_FILE_PATH = RESULT_FILE_PATH;
		Settings.EXTERNAL_SAT_SOLVER = EXTERNAL_SAT_SOLVER;
		Settings.DIMACS_FILE = DIMACS_FILE;
		Settings.LOG_FILE = LOG_FILE;
		Settings.OUTPUT = OUTPUT;
		Settings.MAXIMUM_SIZE = MAXIMUM_SIZE;
		Settings.MINIMUM_SIZE = MINIMUM_SIZE;
		Settings.SB_STRATEGY = SB_STRATEGY;
		Settings.TIMEOUT = TIMEOUT;
		Settings.ERRORS_PERCENT = ERRORS_PERCENT;
		Settings.FIND_K = FIND_K;
		Settings.AT_MOST_ONE_OPTION = AT_MOST_ONE_OPTION;
		Settings.PATHS_LOWER_BOUND = PATHS_LOWER_BOUND;
		Settings.PATHS_ON_SYMBOL_LOWER_BOUND = PATHS_ON_SYMBOL_LOWER_BOUND;
		Settings.RUNS = RUNS;
		Settings.HEURISTIC = HEURISTIC;
		Settings.APTA_BOUND = APTA_BOUND;
		Settings.RED_BOUND = RED_BOUND;
		Settings.SINKS_MODE = SINKS_MODE;
	}

	public static int getSinksAmount() {
		switch (Settings.SINKS_MODE) {
			case 0:
				return 0;
			case 1:
			case 3:
				return 1;
			case 2:
				return 2;
			case 4:
				return 3;
		}
		return 0;
	}
}
