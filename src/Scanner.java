import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class Scanner {

	private List<String> keywords;
	private Document document;
	private Element tokens;
	private int number;
	private int line;
	private boolean verbose;

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.out.println("Missing parameters.");
			return;
		}
		Scanner scanner = new Scanner(true);
		scanner.run(args[0], args[1]);
	}

	public Scanner() {
		verbose = false;
	}

	public Scanner(boolean verbose) {
		this.verbose = verbose;
	}

	public void run(String inputFile, String outputFile) throws Exception {
		FileReader fileReader = new FileReader(inputFile);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		PushbackReader reader = new PushbackReader(bufferedReader);
		createXML(inputFile);
		keywords = initKeyword();
		number = 0;
		line = 1;

		int state = 0;
		StringBuilder word = new StringBuilder();
		Class<?> cl = this.getClass();
		boolean loop = true;
		for (int r = reader.read(); loop; r = reader.read()) {
			char c = (char)r;
			if (r == -1) {
				c = '\r';
				loop = false;
			}
			if (c == '\r') line++;
			if (state == 0 && isWhitespace(c)) continue;

			word.append(c);
			Method action = cl.getMethod("state" + state, char.class);
			state = (int) action.invoke(null, c);

			if (isAcceptState(state)) {
				if (isBackState(state)) {
					reader.unread(c);
					if (c == '\r') line--;
					word.setLength(word.length() - 1);
				}
				dealAcceptState(state, word.toString());
				word.setLength(0);
				state = 0;
			}
		}
		writeXML(outputFile);
	}

	private boolean isBackState(int state) {
		Integer[] s = {-1, -2, -4, -5, -6};
		return Arrays.asList(s).contains(state);
	}

	private boolean isAcceptState(int state) {
		Integer[] s = {-1, -2, -4, -5, -6,
				20, 23, 24, 26, -6, 98};
		return Arrays.asList(s).contains(state);
	}

	private String getWordType(int state, String word) {
		switch (state) {
			case -1:
				if (keywords.contains(word))
					return "keyword";
				else return "identifier";
			case -2: return "constant";
			case 20: return "constant";
			case 23: return "string";
			case -4: return "delimiter";
			case 24: return "delimiter";
			case -5: return "operator";
			case 26: return "operator";
			case -6: return "illegal";
			case 98: return "illegal";
		}
		return "unknown";
	}


	public static int state0(char c) {
		if (isWhitespace(c)) return 0;
		if ((isLetter(c) || c == '_') &&
				!isCharPrefix(c)) return 1;
		if (isCharPrefix(c)) return 35;
		if (c == '\'') return 17;
		if (c == '\"') return 21;

		if (isNonzeroDigit(c)) return 2;
		if (c == '0') return 3;
		if (c == '.') return 11;

		if (isDelimiter(c)) return 24;
		if (isSampleOperator(c)) return 26;
		if (isNormalOperator(c)) return 32;
		if (c == '+') return 27;
		if (c == '-') return 25;
		if (c == '&') return 28;
		if (c == '|') return 29;
		if (c == '<') return 30;
		if (c == '>') return 31;
		return 99;
	}

	public static int state1(char c) {
		if (isLetter(c) || isDigit(c) || c == '_') return 1;
		if (isTerminalChar(c)) return -1;
		return 99;
	}

	public static int state2(char c) {
		if (isDigit(c)) return 2;
		if (c == '.') return 6;
		if (c == 'e' || c == 'E') return 8;

		if (c == 'u' || c == 'U') return 47;
		if (c == 'l') return 48;
		if (c == 'L') return 49;

		if (isTerminalChar(c) && c != '.') return -2;
		return 99;
	}

	public static int state3(char c) {
		if (isOctalDigit(c)) return 3;
		if (c == '8' || c == '9') return 7;
		if (c == '.') return 6;

		if (c == 'u' || c == 'U') return 47;
		if (c == 'l') return 48;
		if (c == 'L') return 49;

		if (c == 'x' || c == 'X') return 4;
		if (c == 'e' || c == 'E') return 8;
		if (isTerminalChar(c) && c != '.') return -2;
		return 99;
	}

	public static int state4(char c) {
		if (isHexDigit(c)) return 5;
		if (c == '.') return 12;
		if (isTerminalChar(c) && c != '.') return -6;
		return 99;
	}

	public static int state5(char c) {
		if (isHexDigit(c)) return 5;
		if (c == '.') return 13;
		if (c == 'p' || c == 'P') return 14;

		if (c == 'u' || c == 'U') return 47;
		if (c == 'l') return 48;
		if (c == 'L') return 49;

		if (isTerminalChar(c) && c != '.') return -2;
		return 99;
	}

	public static int state6(char c) {
		if (isDigit(c)) return 6;
		if (c == 'e' || c == 'E') return 8;
		if (c == 'l' || c == 'f' ||
				c == 'L' || c == 'F') return 34;
		if (isTerminalChar(c)) return -2;
		return 99;
	}

	public static int state7(char c) {
		if (isDigit(c)) return 7;
		if (c == '.') return 6;
		if (c == 'e' || c == 'E') return 8;
		if (isTerminalChar(c)) return 100;
		return 99;
	}

	public static int state8(char c) {
		if (c == '+' || c == '-') return 9;
		if (isDigit(c)) return 10;
		if (isTerminalChar(c)) return 100;
		return 99;
	}

	public static int state9(char c) {
		if (isDigit(c)) return 10;
		if (isTerminalChar(c)) return 100;
		return 99;
	}

	public static int state10(char c) {
		if (isDigit(c)) return 10;
		if (c == 'l' || c == 'f' ||
				c == 'L' || c == 'F') return 34;
		if (isTerminalChar(c) && c != '.') return -2;
		return 99;
	}

	public static int state11(char c) {
		if (isDigit(c)) return 6;
		return -4;
	}

	public static int state12(char c) {
		if (isHexDigit(c)) return 13;
		if (isTerminalChar(c)) return -6;
		return 99;
	}

	public static int state13(char c) {
		if (isHexDigit(c)) return 13;
		if (c == 'p' || c == 'P') return 14;

		if (isTerminalChar(c) ||
				c == '\'' || c == '"') return -6;
		return 99;
	}

	public static int state14(char c) {
		if (c == '+' || c == '-') return 15;
		if (isDigit(c)) return 16;

		if (isTerminalChar(c) ||
				c == '\'' || c == '"') return -6;
		return 99;
	}

	public static int state15(char c) {
		if (isDigit(c)) return 16;

		if (isTerminalChar(c) ||
				c == '\'' || c == '"') return -6;
		return 99;
	}

	public static int state16(char c) {
		if (isDigit(c)) return 16;
		if (c == 'l' || c == 'f' ||
				c == 'L' || c == 'F') return 34;
		if (isTerminalChar(c) && c != '.') return -2;
		return 99;
	}

	public static int state17(char c) {
		if (c == '\\') return 18;
		if (c == '\'') return 98;
		if (c == '\r' || c == '\n') return -6;
		return 19;
	}

	public static int state18(char c) {
		if (isEscapedChar(c)) return 19;
		if (isOctalDigit(c)) return 36;
		if (c == 'x') return 39;
		if (c == '\r' || c == '\n') return -6;
		return 97;
	}

	public static int state19(char c) {
		if (c == '\'') return 20;
		return 97;
	}

	public static int state21(char c) {
		if (c == '\\') return 22;
		if (c == '"') return 23;
		if (c == '\r' || c == '\n') return -6;
		return 21;
	}

	public static int state22(char c) {
		if (isEscapedChar(c)) return 21;
		if (isOctalDigit(c)) return 42;
		if (c == 'x') return 45;
		if (c == '\r' || c == '\n') return -6;
		return 96;
	}

	public static int state25(char c) {
		if (c == '>') return 24;
		if (c == '-' || c == '=') return 26;
		return -5;
	}

	public static int state27(char c) {
		if (c=='+' || c == '=') return 26;
		return -5;
	}

	public static int state28(char c) {
		if (c == '&' || c == '=') return 26;
		return -5;
	}

	public static int state29(char c) {
		if (c == '|' || c == '=') return 26;
		return -5;
	}

	public static int state30(char c) {
		if (c == '=') return 26;
		if (c == '<') return 33;
		return -5;
	}

	public static int state31(char c) {
		if (c == '=') return 26;
		if (c == '>') return 33;
		return -5;
	}

	public static int state32(char c) {
		if (c == '=') return 26;
		return -5;
	}

	public static int state33(char c) {
		if (c == '=') return 26;
		return -5;
	}
	
	public static int state34(char c) {
		if (isTerminalChar(c) && c != '.') return -2;
		return 99;
	}

	public static int state35(char c) {
		if (c == '8') return 41;
		if (c == '\'') return 17;
		if (c == '"') return 21;
		if (isLetter(c) || isDigit(c) || c == '_') return 1;
		if (isTerminalChar(c)) return -1;
		return 99;
	}

	public static int state36(char c) {
		if (c == '\'') return 20;
		if (isOctalDigit(c)) return 37;
		return 97;
	}

	public static int state37(char c) {
		if (c == '\'') return 20;
		if (isOctalDigit(c)) return 38;
		return 97;
	}

	public static int state38(char c) {
		if (c == '\'') return 20;
		return 97;
	}

	public static int state39(char c) {
		if (isHexDigit(c)) return 40;
		return 97;
	}

	public static int state40(char c) {
		if (isHexDigit(c)) return 40;
		if (c == '\'') return 20;
		return 97;
	}

	public static int state41(char c) {
		if (c == '"') return 21;
		if (isLetter(c) || isDigit(c) || c == '_') return 1;
		if (isTerminalChar(c)) return -1;
		return 99;
	}

	public static int state42(char c) {
		if (isOctalDigit(c)) return 43;
		if (c == '\\') return 22;
		if (c == '"') return 23;
		if (c == '\r' || c == '\n') return 96;
		return 21;
	}

	public static int state43(char c) {
		if (isOctalDigit(c)) return 44;
		if (c == '\\') return 22;
		if (c == '"') return 23;
		if (c == '\r' || c == '\n') return 96;
		return 21;
	}

	public static int state44(char c) {
		if (c == '\\') return 22;
		if (c == '"') return 23;
		if (c == '\r' || c == '\n') return 96;
		if (isOctalDigit(c)) return 96;
		return 21;
	}

	public static int state45(char c) {
		if (isHexDigit(c)) return 46;
		return 96;
	}

	public static int state46(char c) {
		if (isHexDigit(c)) return 46;
		if (c == '\\') return 22;
		if (c == '"') return 23;
		if (c == '\r' || c == '\n') return 96;
		return 21;
	}

	public static int state47(char c) {
		if (c == 'l') return 50;
		if (c == 'L') return 51;
		if (isTerminalChar(c) && c != '.') return -2;
		return 99;
	}

	public static int state48(char c) {
		if (c == 'l') return 52;
		if (c == 'u' || c == 'U') return 53;
		if (isTerminalChar(c) && c != '.') return -2;
		return 99;
	}

	public static int state49(char c) {
		if (c == 'L') return 52;
		if (c == 'u' || c == 'U') return 53;
		if (isTerminalChar(c) && c != '.') return -2;
		return 99;
	}

	public static int state50(char c) {
		if (c == 'l') return 53;
		if (isTerminalChar(c) && c != '.') return -2;
		return 99;
	}

	public static int state51(char c) {
		if (c == 'L') return 53;
		if (isTerminalChar(c) && c != '.') return -2;
		return 99;
	}

	public static int state52(char c) {
		if (c == 'u' || c == 'U') return 53;
		if (isTerminalChar(c) && c != '.') return -2;
		return 99;
	}

	public static int state53(char c) {
		if (isTerminalChar(c) && c != '.') return -2;
		return 99;
	}

	public static int state96(char c) {
		if (c == '"') return 98;
		if (c == '\r' || c == '\n') return -6;
		return 96;
	}

	public static int state97(char c) {
		if (c == '\'') return 98;
		if (c == '\r' || c == '\n') return -6;
		return 97;
	}

	public static int state99(char c) {
		if (isTerminalChar(c)) return -6;
		return 99;
	}


	private static boolean isLetter(char c) {
		return c >= 'a' && c <= 'z' ||
				c >= 'A' && c <= 'Z';
	}

	private static boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	private static boolean isNonzeroDigit(char c) {
		return c >= '1' && c <= '9';
	}

	private static boolean isOctalDigit(char c) {
		return c >= '0' && c <= '9';
	}

	private static boolean isHexDigit(char c) {
		return (c >= '0' && c <= '9') ||
				(c >= 'a' && c <= 'f') ||
				(c >= 'A' && c <= 'F');
	}

	private static boolean isCharPrefix(char c) {
		Character[] s = {'L', 'u', 'U'};
		return Arrays.asList(s).contains(c);
	}

	private static boolean isEscapedChar(char c) {
		Character[] s = {'\'', '"', '?', '\\',
				'a', 'b', 'f', 'n', 'r', 't', 'v'};
		return Arrays.asList(s).contains(c);
	}

	private static boolean isDelimiter(char c) {
		Character[] s = {'(', ')', '[', ']', '{', '}', ';'};
		return Arrays.asList(s).contains(c);
	}

	private static boolean isOperator(char c) {
		Character[] s = {'+', '-', '*', '/', '%',
				'&', '|', '^', '~', '>', '<', '=', '!',
				',', '?', ':', '!', '^', '='};
		return Arrays.asList(s).contains(c);
	}

	private static boolean isNormalOperator(char c) {
		Character[] s = {'*', '/', '%', '!', '^', '='};
		return Arrays.asList(s).contains(c);
	}

	private static boolean isSampleOperator(char c) {
		Character[] s = {'?', ':', ',', '~'};
		return Arrays.asList(s).contains(c);
	}

	private static boolean isWhitespace(char c) {
		Character[] s = {' ', '\t', '\r', '\n'};
		return Arrays.asList(s).contains(c);
	}

	private static boolean isTerminalChar(char c) {
		return isWhitespace(c) || isDelimiter(c) || isOperator(c)
				|| c == '.' || c == '\'' || c == '"' || c == '\\';
	}


	private static List<String> initKeyword() {
		String words[] = {"auto", "break", "case", "char", "const",
				"continue", "default", "do", "double", "else", "enum",
				"extern", "float", "for", "goto", "if", "inline", "int",
				"long", "register", "restrict", "return", "short", "signed",
				"sizeof", "static", "struct", "switch", "typedef",
				"union", "unsigned", "void", "volatile", "while",
				"_Alignas", "_Alignof", "_Atomic", "_Bool", "_Complex",
				"_Generic", "_Imaginary", "_Noreturn", "_Static_assert",
				"_Thread_local"};
		return Arrays.asList(words);
	}

	private void createXML(String inputFile) throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		document = builder.newDocument();

		Element project = document.createElement("project");
		project.setAttribute("name", inputFile);
		document.appendChild(project);
		tokens = document.createElement("tokens");
		project.appendChild(tokens);
	}

	private void dealAcceptState(int state, String word) {
		String type = getWordType(state, word);
		if (verbose) System.out.printf("%-2d %-5s %s\n", line, word, type);
		Element token = document.createElement("token");

		Element numberElement = document.createElement("number");
		Text numberText = document.createTextNode(String.valueOf(++number));
		numberElement.appendChild(numberText);
		token.appendChild(numberElement);

		Element valueElement = document.createElement("value");
		Text valueText = document.createTextNode(word);
		valueElement.appendChild(valueText);
		token.appendChild(valueElement);

		Element typeElement = document.createElement("type");
		Text typeText = document.createTextNode(type);
		typeElement.appendChild(typeText);
		token.appendChild(typeElement);

		Element lineElement = document.createElement("line");
		Text lineText = document.createTextNode(String.valueOf(line));
		lineElement.appendChild(lineText);
		token.appendChild(lineElement);

		Element validElement = document.createElement("valid");
		Text validText = document.createTextNode("true");
		validElement.appendChild(validText);
		token.appendChild(validElement);

		tokens.appendChild(token);
	}

	private void writeXML(String outputFile) throws TransformerException, IOException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		transformerFactory.setAttribute("indent-number", 2);

		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		DOMSource domSource = new DOMSource(document);
		FileWriter fileWriter = new FileWriter(outputFile);
		StreamResult streamResult = new StreamResult(fileWriter);
		transformer.transform(domSource, streamResult);
	}
}
