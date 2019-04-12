import java.io.*;
import java.util.HashMap;

public class MiniCCPreProcessor {

	public void run(String inputFile, String outputFile) {
		if (!checkFile(inputFile)) {
			return;
		}
		StringBuilder processed = new StringBuilder();
		File file = new File(inputFile);

		BufferedReader reader;
		String line;
		int i;
		try {
			reader = new BufferedReader(new FileReader(file));

			label364:
			while (true) {
				int start_0;
				int end;
				do {
					do {
						do {
							if ((line = reader.readLine()) == null) {
								reader.close();
								break label364;
							}
						} while (line.isEmpty());
					} while (line.contains("#i") || line.contains("#u"));
					start_0 = line.indexOf("//");
					i = line.indexOf("/*");
					end = line.indexOf("*/");
				} while (start_0 == 0);

				if (start_0 > 0 && start_0 > i) {
					line = line.substring(0, start_0);
				} else if (i >= 0 && end >= 0 && end - i >= 2) {
					line = line.substring(0, i);
				} else if (i >= 0 && end == -1) {
					line = line.substring(0, i);

					do {
						String l = reader.readLine();
						end = l.indexOf("*/");
					} while (end == -1);
				}

				processed.append(line);
				processed.append("\r\n");
			}
		} catch (Exception var14) {
			var14.printStackTrace();
		}
		createAndWriteFile(outputFile, processed.toString());

		file = new File(outputFile);
		processed = new StringBuilder();
		try {
			reader = new BufferedReader(new FileReader(file));
			HashMap<String, String> map = new HashMap<>();

			label162:
			while (true) {
				while (true) {
					if ((line = reader.readLine()) == null) {
						break label162;
					}

					i = line.indexOf("#define");
					String[] strarray = line.split("\\s+");
					if (i == 0) {
						map.put(strarray[1], strarray[2]);
					} else {
						for (i = 0; i < strarray.length; ++i) {
							if (map.containsKey(strarray[i])) {
								strarray[i] = map.get(strarray[i]);
							}

							processed.append(strarray[i]);
							processed.append(" ");
						}

						processed.append("\r\n");
					}
				}
			}
		} catch (Exception var12) {
			var12.printStackTrace();
		}
		createAndWriteFile(outputFile, processed.toString());
	}

	public static boolean checkFile(String filePath) {
		File file = new File(filePath);
		if (!file.exists()) {
			System.out.println("file " + file + " does not exist:");
			return false;
		} else {
			return true;
		}
	}

	public static void createAndWriteFile(String file, String content) {
		try {
			FileWriter fw = new FileWriter(file, false);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);
			bw.close();
			fw.close();
		} catch (Exception var4) {
			var4.printStackTrace();
		}

	}
}
