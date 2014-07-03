package ru.ifmo.rain.zakirzyanov.DFAInductor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

public class MainNoisy {
	private static final String[] test = { "0_training.txt.dat", "train-10-0.txt" };
	private static final int MAX_PERCENT = 30;
	private static final String resultFilePath = "ans.dot";

	public static void main(String[] args) throws IOException,
			ContradictionException, TimeoutException, ParseFormatException {
		InputStream is = new FileInputStream(test[0]);
		APTA apta = new APTA(is, APTA.IS_NOISY);
		ConsistencyGraph cg = new ConsistencyGraph();

		for (int percent = 10; percent <= MAX_PERCENT; percent++) {
			System.out.println("======");
			System.out.println("percent: " + percent);
			int colors = apta.getColors();
			try {
				String dimacsFile = new DimacsFileGenerator(apta, cg, colors,
						DimacsFileGenerator.BFS_SB, percent).generateFile();
				SATSolver solver = new SATSolver(apta, cg, colors, dimacsFile,
						900, "lingeling.exe");
				System.out.println("Vars: " + solver.nVars());
				System.out.println("Constraints: " + solver.nConstraints());
				if (solver.problemIsSatisfiable()) {
					System.out.println("The automat with " + percent
							+ "% mistakes was found.");
					Automat automat = solver.getModel();
					PrintWriter pw = new PrintWriter(resultFilePath);
					pw.print(automat + "\n");
					pw.close();
					break;
				} else {
					System.out.println("The automat with " + percent
							+ "% mistakes not found.");
				}
			} catch (ContradictionException e) {
				System.out.println("The automat with " + percent
						+ "% mistakes not found.");
			} catch (TimeoutException e) {
				System.out.println("timeot reached");
				break;
			}
		}
	}
}
