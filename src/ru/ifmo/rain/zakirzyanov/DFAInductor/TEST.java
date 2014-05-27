package ru.ifmo.rain.zakirzyanov.DFAInductor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

public class TEST {
	
	
	private static final int MAX_COLOR = 50;
	private static final boolean USE_SB = true;
	private static final boolean DONT_USE_SB = false;
	private static final String horLine = "--------------------------------------------------------------------------------------\n";
	private String tests = "     |";
	private PrintWriter pw;
	private String resultFile;
	
	public TEST(String resultFile) throws IOException {
		pw = new PrintWriter(resultFile);
		for (int i = 1; i <= MAX_COLOR; i++) {
			tests += i + "        ";
		}
	}
	
	public void test() throws IOException, ParseFormatException {
		
		File dir = new File("tests");
		File[] dirListing = dir.listFiles();
		int i = 1;
		if (dirListing != null) {
			for (File child : dirListing) {
//				if (i < 7) {
//					continue;
//				}
				pw.println(child.getName());
				pw.println(tests);
				pw.print("SB   |");
				int colors = test(child, USE_SB, MAX_COLOR);
				pw.print("WoSB |");
				int colors2 = test(child, DONT_USE_SB, colors);
				assert(colors == colors2);
				pw.print(horLine);
				pw.flush();
				System.out.println(i++ + " passed");
			}
		}
		pw.close();
		
	}
	
	public int test(File file, boolean useSB, int maxColor) throws IOException, ParseFormatException {
		InputStream is = new FileInputStream(file);
		APTA apta = new APTA(is);
		ConsistencyGraph cg = new ConsistencyGraph(apta);
		int colors;
		for (colors = 1; colors <= maxColor; colors++) {
			try {
				String dimacsFile = new DimacsFileGenerator(apta, cg, colors,
						useSB).generateFile();
				SATSolver solver = new SATSolver(apta, cg, colors, dimacsFile, 300, "lingeling.exe");
				long startTime = System.currentTimeMillis();
				if (solver.problemIsSatisfiable()) {
					pw.println((System.currentTimeMillis() - startTime) / 1000.);
					return colors;
				} else {
					String tmp = (System.currentTimeMillis() - startTime) / 1000. + "";
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
	
	class Pair {
		double time;
		int colors;
		
		Pair (double time, int colors) {
			this.time = time;
			this.colors = colors;
		}
	}
}
