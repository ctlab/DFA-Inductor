package ru.ifmo.rain.zakirzyanov.DFAInductor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

public class Main {

	private static final String[] test = { "0_training.txt.dat",
			"01_training.txt.dat", "1_training.txt.dat", "10_training.txt.dat",
			"50_training.txt.dat" };
	private static final int MAX_COLORS = 30;
	private static final String resultFilePath = "ans.dot";

	public static void main(String[] args) throws IOException,
			ContradictionException, TimeoutException, ParseFormatException {
		InputStream is = new FileInputStream(test[2]);
		APTA apta = new APTA(is);
		ConsistencyGraph cg = new ConsistencyGraph(apta);

		for (int colors = 1; colors <= MAX_COLORS; colors++) {
			System.out.println("======");
			System.out.println("colors: " + colors);
			try {
				String dimacsFile = new DimacsFileGenerator(apta, cg, colors).generateFile();
				SATSolver solver = new SATSolver(apta, cg, colors, dimacsFile);
				System.out.println("Vars: " + solver.nVars());
				System.out.println("Constraints: " + solver.nConstraints());
				if (solver.problemIsSatisfiable()) {
					System.out.println("The automat with " + colors
							+ " colors was found.");
					Automat automat = solver.getModel();
					PrintWriter pw = new PrintWriter(resultFilePath);
					pw.print(automat + "\n");
					pw.close();
					break;
				} else {
					System.out.println("The automat with " + colors
							+ " colors not found.");
				}
			} catch (ContradictionException e) {
				System.out.println("no");
			}
		}
	}
}
