import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.lang.reflect.Method;

public class BITMiniCC {

	public static void main(String[] args) throws Exception {
		if(args.length < 1){
			System.out.println("Usage: BITMiniCC <file.c>");
			return;
		}
		if (!args[0].endsWith(".c")) {
			System.out.println("Incorrect file name: " + args[0]);
			return;
		}

		String[] step = {"Preprocess", "Lexical analysis", "Parse",
				"Semantic analysis", "Internal code generating",
				"Optimizing", "Code generating"};
		String[] suffix = {".c", ".pp.c", ".token.xml", ".tree.xml",
				".tree2.xml", ".ic.xml", ".ic2.xml", ".code.s"};
		String[] defaultClass = {"MiniCCPreProcessor", "MiniCCScanner",
				"MiniCCParser", "MiniCCSemantic", "MiniCCICGen",
				"MiniCCOptimizer", "MiniCCCodeGen"};

		System.out.println("Start to compile ...");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse("config.xml");
		Element phases = (Element) document.getElementsByTagName("phase").item(0);
		NodeList nodeList = phases.getElementsByTagName("phase");

		String outputFile = args[0];
		for (int i = 0; i < nodeList.getLength() && i < defaultClass.length; i++) {
			Element phase = (Element) nodeList.item(i);
			String skip = phase.getAttribute("skip");
			if (skip.equals("true")) break;
			String type = phase.getAttribute("type");
			String path = phase.getAttribute("path");

			String inputFile = outputFile;
			outputFile = inputFile.replace(suffix[i], suffix[i + 1]);

			if (type.equals("java")) {
				if (path.equals("")) path = defaultClass[i];
				Class<?> c = Class.forName(path);
				Method method = c.getMethod("run", String.class, String.class);
				method.invoke(c.newInstance(), inputFile, outputFile);
			} else {
				Runtime runtime = Runtime.getRuntime();
				String command = path + " " + inputFile + " " + outputFile;
				if (type.equals("python")) command = "python " + command;
				Process p = runtime.exec(command);
				p.wait();
			}
			System.out.println("Step " + (i + 1) + ": " + step[i] + " finished.");
		}
		System.out.println("Compiling completed.");
	}
}
