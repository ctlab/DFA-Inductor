package ru.ifmo.rain.zakirzyanov.DFAInductor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

public class TEST {

	private static final int MAX_COLOR = 20;
	private static final String horLine = "--------------------------------------------------------------------------------------\n";
	private String tests = "       |";
	private PrintWriter pw;

	public TEST(String resultFile) throws IOException {
		pw = new PrintWriter(resultFile);
		for (int i = 1; i <= MAX_COLOR; i++) {
			tests += i;
			if (i < 10) {
				tests += " ";
			}
			tests += "       ";
		}
	}

	public void test() throws IOException, ParseFormatException {

		File dir = new File("tests");
		File[] dirListing = dir.listFiles();
		int i = 1;
		if (dirListing != null) {
			for (File child : dirListing) {
				// if (i < 7) {
				// continue;
				// }
				pw.println(child.getName());

				pw.println(tests);

				pw.print("HOLLAND|");
				int colors = hollandTest(child, MAX_COLOR);
				System.out.print(colors + " ");

				pw.print("CLIQUE |");
				int colors2 = test(child, DimacsFileGenerator.CLIQUE_SB,
						MAX_COLOR);
				System.out.print(colors2 + " ");
				assert (colors == colors2);

				pw.print("BFS    |");
				int colors3 = test(child, DimacsFileGenerator.BFS_SB, MAX_COLOR);
				System.out.print(colors3 + " ");
				assert (colors == colors3);

				pw.print("WoSB   |");
				int colors4 = test(child, DimacsFileGenerator.WITHOUT_SB,
						MAX_COLOR);
				System.out.println(colors4);
				assert (colors == colors4);
				APTA apta = new APTA(new FileInputStream(child), APTA.IS_NOT_NOISY);
				pw.println("apta size = " + apta.getSize() + ", words count = "
						+ apta.getCountOfWords());
				pw.print(horLine);
				pw.flush();
				System.out.println(i++ + " passed");
			}
		}
		pw.close();

	}

	public int test(File file, int SB, int maxColor) throws IOException,
			ParseFormatException {
		InputStream is = new FileInputStream(file);
		APTA apta = new APTA(is, APTA.IS_NOT_NOISY);
		ConsistencyGraph cg = new ConsistencyGraph(apta);
		int colors;
		for (colors = 1; colors <= maxColor; colors++) {
			try {
				String dimacsFile = new DimacsFileGenerator(apta, cg, colors,
						SB).generateFile();
				SATSolver solver = new SATSolver(apta, cg, colors, dimacsFile,
						300, "lingeling.exe");
				long startTime = System.currentTimeMillis();
				if (solver.problemIsSatisfiable()) {
					pw.println((System.currentTimeMillis() - startTime) / 1000.);
					pw.println("Vars: " + solver.nVars() + ", clauses: "
							+ solver.nConstraints());
					return colors;
				} else {
					String tmp = (System.currentTimeMillis() - startTime)
							/ 1000. + "";
					int len = tmp.length();
					while (len < 9) {
						tmp += " ";
						len++;
					}
					pw.print(tmp);
				}
			} catch (ContradictionException e) {

			} catch (TimeoutException e) {
				pw.print("TL       ");
			}
		}
		return colors;
	}

	public int hollandTest(File file, int maxColor) throws IOException {
		String cmd = "d:\\Prog\\java\\DFASAT\\dfasat_c++\\dfasat.exe -h=1 -m=1 -k="
				+ (maxColor + 2)
				+ " ..\\DFA-Inductor\\tests\\"
				+ file.getName() + " ..\\DFA-Inductor\\lingeling.exe";
		// Process process = Runtime.getRuntime().exec(cmd);
		ProcessBuilder proBuilder = new ProcessBuilder(cmd.split(" "));
		Process process = proBuilder.start();
		BufferedReader br = new BufferedReader(new InputStreamReader(
				process.getInputStream()));
		String line, header = null;
		int colors = 1;
		while ((line = br.readLine()) != null) {
			if (line.startsWith("header:")) {
				header = new String(line);
			}
			if (line.startsWith("s ")) {
				System.out.println(colors++);
				boolean satisfiable = line.equals("s SATISFIABLE");
				line = br.readLine();
				double time = Double.parseDouble(line.split(" ")[1]);
				String tmp = Double.toString(time);
				int len = tmp.length();
				while (len < 9) {
					tmp += " ";
					len++;
				}
				if (line.equals("s UNKNOWN")) {
					pw.print("TL       ");
				} else {
					pw.print(tmp);
				}
				if (satisfiable) {
					pw.println();
					pw.println("Vars: " + header.split(" ")[3] + ", clauses: "
							+ header.split(" ")[4]);
					line = br.readLine();
					br.close();
					return Integer.parseInt(line.split(" ")[1]) - 2;
				}
			}
		}
		br.close();
		return -1;
	}

	class Pair {
		double time;
		int colors;

		Pair(double time, int colors) {
			this.time = time;
			this.colors = colors;
		}
	}
}
