package flands;

import java.util.Arrays;

/**
 * A simple expression parser and evaluator. Accepted identifiers must start with a
 * letter, and consist of letters, digits, and underscores; numbers start with
 * a digit; accepted symbols are <code>+ - * / ( )</code>. Once parsed,
 * the expression can be evaluated by calling <code>getRoot().evaluate()</code>,
 * passing it an object that can translate each identifier into a value.
 * In practice, the only Resolver implementation is Node, which looks for
 * matching variables for the current section.
 * <p>
 * SetVarNode extends the implementation of Resolver to recognise 'armour', 'weapon',
 * 'stamina', 'crew', 'shards', and ability names.
 * <p>
 * Constructed from hazy memories of Compiler Construction and a simple EBNF grammar.
 * 
 * @author Jonathan Mann
 */
public class Expression {
	public static interface Resolver {
		public int resolveIdentifier(String name);
	}

	public static abstract class ExpNode {
		static final int NO_OP = 0;
		public abstract int evaluate(Resolver r);
	}

	private String text;
	private int off;
	private ExpNode root;
	
	public Expression(String text) {
		this.text = format(text);
		off = 0;
		root = expr1();
	}
	
	public ExpNode getRoot() {
		return root;
	}
	
	private static char endch = '$';
	private String format(String text) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < text.length(); i++)
			if (!Character.isWhitespace(text.charAt(i)))
				sb.append(text.charAt(i));
		
		if (sb.charAt(sb.length()-1) != endch)
			sb.append(endch);
		
		return sb.toString();
	}
	
	private void printError(String message, int index) {
		System.err.println(message + text.substring(0, text.length()-1));
		char[] spaces = new char[message.length() + index];
		Arrays.fill(spaces, ' ');
		System.err.println(new String(spaces) + "^");
	}
	
	private ExpNode expr1() {
		ExpNode n1 = expr2();
		if (text.charAt(off) == '+') {
			off++;
			return new Expr1(n1, expr1(), Expr1.PLUS_OP);
		}
		else if (text.charAt(off) == '-') {
			off++;
			return new Expr1(n1, expr1(), Expr1.MINUS_OP);
		}
		else
			return n1;
	}
	
	private ExpNode expr2() {
		ExpNode n1 = expr3();
		if (text.charAt(off) == '*') {
			off++;
			return new Expr2(n1, expr2(), Expr2.MULT_OP);
		}
		else if (text.charAt(off) == '/') {
			off++;
			return new Expr2(n1, expr2(), Expr2.DIVIDE_OP);
		}
		else
			return n1;
	}
	
	private ExpNode expr3() {
		int lead;
		if (text.charAt(off) == '-') {
			lead = Expr3.MINUS_LEAD;
			off++;
		}
		else {
			lead = Expr3.PLUS_LEAD;
			if (text.charAt(off) == '+')
				off++;
		}
		
		ExpNode n;
		if (text.charAt(off) == '(') {
			off++;
			n = expr1();
			if (text.charAt(off) == ')')
				off++; // expected
			else
				printError("Expected ')' at index " + off + ": ", off);
		}
		else if (Character.isDigit(text.charAt(off))) {
			n = num();
		}
		else if (Character.isLetter(text.charAt(off))) {
			n = ident();
		}
		else {
			printError("Unexpected char at index " + off + ": ", off);
			n = new Num(0);
		}
		
		return new Expr3(n, lead);
	}
	
	private ExpNode num() {
		int end = off;
		int val = 0;
		while (Character.isDigit(text.charAt(++end))) ;
		try {
			val = Integer.parseInt(text.substring(off, end));
		}
		catch (NumberFormatException nfe) {
			nfe.printStackTrace();
		}
		
		off = end;
		return new Num(val);
	}
	
	private ExpNode ident() {
		int end = off;
		while (true) {
			char ch = text.charAt(++end);
			if (!Character.isLetter(ch) &&
				!Character.isDigit(ch) &&
				ch != '_')
				break;
		}
		
		String id = text.substring(off, end);
		off = end;
		return new Ident(id);
	}
	
	private static class Expr1 extends ExpNode {
		private ExpNode n1, n2 = null;
		static final int PLUS_OP = 1;
		static final int MINUS_OP = 2;
		private int op;
		
		Expr1(ExpNode n1, ExpNode n2, int op) {
			this.n1 = n1;
			this.n2 = n2;
			this.op = op;
		}
		
		public int evaluate(Resolver r) {
			switch (op) {
			case NO_OP:
				return n1.evaluate(r);
			case PLUS_OP:
				return n1.evaluate(r) + n2.evaluate(r);
			case MINUS_OP:
				return n1.evaluate(r) - n2.evaluate(r);
			}
			return 0;
		}
		
		public String toString() {
			String s1 = n1.toString();
			switch (op) {
			case NO_OP:
				return s1;
			case PLUS_OP:
				return "(" + s1 + " + " + n2.toString() + ")";
			case MINUS_OP:
				return "(" + s1 + " - " + n2.toString() + ")";
			}
			return "err";
		}
	}
	
	private static class Expr2 extends ExpNode {
		private ExpNode n1, n2 = null;
		static final int MULT_OP = 1;
		static final int DIVIDE_OP = 2;
		private int op;
		
		Expr2(ExpNode n1, ExpNode n2, int op) {
			this.n1 = n1;
			this.n2 = n2;
			this.op = op;
		}
		
		public int evaluate(Resolver r) {
			switch (op) {
			case NO_OP:
				return n1.evaluate(r);
			case MULT_OP:
				return n1.evaluate(r) * n2.evaluate(r);
			case DIVIDE_OP:
				return n1.evaluate(r) / n2.evaluate(r);
			}
			return 0;
		}
		
		public String toString() {
			String s1 = n1.toString();
			switch (op) {
			case NO_OP:
				return s1;
			case MULT_OP:
				return "(" + s1 + " * " + n2.toString() + ")";
			case DIVIDE_OP:
				return "(" + s1 + " / " + n2.toString() + ")";
			}
			return "err";
		}
	}
	
	private static class Expr3 extends ExpNode {
		private ExpNode n;
		
		static final int PLUS_LEAD = 0;
		static final int MINUS_LEAD = 1;
		private int lead;

		public Expr3(ExpNode n, int lead) {
			this.n = n;
			this.lead = lead;
		}
		
		public int evaluate(Resolver r) {
			switch (lead) {
			case PLUS_LEAD:
				return n.evaluate(r);
			case MINUS_LEAD:
				return -n.evaluate(r);
			}
			return 0;
		}
		
		public String toString() {
			String s = n.toString();
			return (lead == MINUS_LEAD ? "-" + s : s);
		}
	}
	
	private static class Num extends ExpNode {
		private int val;
		
		Num(int val) {
			this.val = val;
		}
		
		public int evaluate(Resolver r) {
			return val;
		}
		
		public String toString() {
			return Integer.toString(val);
		}
	}
	
	private static class Ident extends ExpNode {
		private String ident;
		
		Ident(String ident) {
			this.ident = ident;
		}
		
		public int evaluate(Resolver r) {
			return r.resolveIdentifier(ident);
		}
		
		public String toString() {
			return ident;
		}
	}
	
	public static void main(String args[]) {
		for (int a = 0; a < args.length; a++) {
			String arg = args[a];
			Expression exp = new Expression(arg);
			System.out.println("Expression " + a + "=" + exp.getRoot());
		}
	}
}
