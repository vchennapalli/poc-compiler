package cop5556sp18;
/* *
 * Initial code for SimpleParser for the class project in COP5556 Programming Language Principles 
 * at the University of Florida, Spring 2018.
 * 
 * This software is solely for the educational benefit of students 
 * enrolled in the course during the Spring 2018 semester.  
 * 
 * This software, and any software derived from it,  may not be shared with others or posted to public web sites,
 * either during the course or afterwards.
 * 
 *  @Beverly A. Sanders, 2018
 */

import cop5556sp18.Scanner.Token;
import cop5556sp18.Scanner.Kind;
import static cop5556sp18.Scanner.Kind.*;

import java.util.ArrayList;

import cop5556sp18.AST.*;

public class Parser {
	
	@SuppressWarnings("serial")
	public static class SyntaxException extends Exception {
		Token t;

		public SyntaxException(Token t, String message) {
			super(message);
			this.t = t;
		}
	}

	Scanner scanner;
	Token t;

	Parser(Scanner scanner) {
		this.scanner = scanner;
		t = scanner.nextToken();
	}

	public Program parse() throws SyntaxException {
		Program p = program();
		matchEOF();
		return p;
	}

	
	/*
	 * Program ::= Identifier Block
	 */
	public Program program() throws SyntaxException {
		Token first = t;
		Token progName = match(IDENTIFIER);
		Block block = block();
		return new Program(first, progName, block);
	}
	
	
	/*
	 * Block ::=  { (  (Declaration | Statement) ; )* }
	 */
	
	Kind[] firstDec = { KW_int, KW_boolean, KW_image, KW_float, KW_filename };
	Kind[] firstStatement = {KW_input, KW_write, IDENTIFIER, KW_red, KW_green, KW_blue, KW_alpha, KW_while, KW_if, KW_show, KW_sleep};

	public Block block() throws SyntaxException {
		Token first = t;
		match(LBRACE);
		ArrayList<ASTNode> decsOrStatements = new ArrayList<ASTNode>();
		while (isKind(firstDec)||isKind(firstStatement)) {
			if (isKind(firstDec)) {
				Declaration d = declaration();
				decsOrStatements.add(d);
			} else if (isKind(firstStatement)) {
				Statement s = statement();
				decsOrStatements.add(s);
	     	}
			match(SEMI);
		}
		match(RBRACE);
		return new Block(first, decsOrStatements);
	}
	
	
	/*
	 * Declaration ::= Type Identifier | image IDENTIFIER [Expression, Expression]
	 */
	//TODO Improve
	public Declaration declaration() throws SyntaxException {
		boolean isImage = isKind(KW_image);
		Token first = t;
		Token type = consume(); //type
		Token name = match(IDENTIFIER);
		Expression width = null;
		Expression height = null;
		if (isImage && isKind(LSQUARE)) {
			consume();
			width = expression();
			match(COMMA);
			height = expression();
			match(RSQUARE);
		}
		return new Declaration(first, type, name, width, height);
	}
	
	
	/*
	 * Statement ::= StatementInput | StatementWrite | StatementAssignment | StatementWhile | StatementIf | StatementShow | StatementSleep
	 */	
	public Statement statement() throws SyntaxException {
		Token first = t, destName = null;
		Expression e = null;
		Block b = null;
		switch (t.kind) {
			case KW_input:  			//StatementInput ::= input Identifier from @ Expression
				consume();
				destName = match(IDENTIFIER);
				match(KW_from);
				match(OP_AT);
				e = expression();
				return new StatementInput(first, destName, e);
			case KW_write:			//StatementWrite ::= write Identifier to Identifier
				consume();
				Token sourceName = match(IDENTIFIER);
				match(KW_to);
				destName = match(IDENTIFIER);
				return new StatementWrite(first, sourceName, destName);
			case KW_while:			//StatementWhile ::= while (Expression) Block
				consume();
				match(LPAREN);
				e = expression();
				match(RPAREN);
				b = block();
				return new StatementWhile(first, e, b);
			case KW_if:				//StatementIf ::= if (Expression) Block
				consume();
				match(LPAREN);
				e = expression();
				match(RPAREN);
				b = block();
				return new StatementIf(first, e, b);
			case KW_show:			//StatementShow ::= show Expression
				consume();
				e = expression();
				return new StatementShow(first, e);
			case KW_sleep:			//StatementSleep ::= sleep Expression
				consume();
				e = expression();
				return new StatementSleep(first, e);
			case IDENTIFIER:
			case KW_red:
			case KW_green:
			case KW_blue:
			case KW_alpha:
				LHS lhs = lhs();
				match(OP_ASSIGN);
				e = expression();
				return new StatementAssign(first, lhs, e);
			default:
				throw new SyntaxException(t, "unacceptable token in this format");
		}
	}
	

	/*
	 * StatementAssignment ::= LHS := Expression
	 * LHS ::= Identifier | Identifier PixelSelector | Color (IDENTIFIER PixelSelector)
	 */
	
	Kind[] color = {KW_red, KW_green, KW_blue, KW_alpha};
	
	public LHS lhs() throws SyntaxException {
		Token first = t;
		if (isKind(color)) {
			Token color = consume();
			match(LPAREN);
			Token name = match(IDENTIFIER);
			PixelSelector pixel = pixelSelector();
			match(RPAREN);
			return new LHSSample(first, name, pixel, color);
		}
			
		Token name = match(IDENTIFIER);
		if (isKind(LSQUARE)) {
			PixelSelector pixelSelector = pixelSelector();
			return new LHSPixel(first, name, pixelSelector);
		}
		return new LHSIdent(first, name);
	}
	
	
	/*
	 * PixelSelector ::= [Expression, Expression]
	 */
	public PixelSelector pixelSelector() throws SyntaxException {
		Token first = t;
		match(LSQUARE);
		Expression ex = expression();
		match(COMMA);
		Expression ey = expression();
		match(RSQUARE);
		return new PixelSelector(first, ex, ey);
	}
	
	
	//TODO revise
	/*
	 * Expression ::= OrExpression ? Expression : Expression | OrExpression
	 */
	public Expression expression() throws SyntaxException {
		Token first = t;
		Expression eor = orExpression();
		if (isKind(OP_QUESTION)) {
			consume();
			Expression etrue = expression();
			match(OP_COLON);
			Expression efalse = expression();
			eor = new ExpressionConditional(first, eor, etrue, efalse);
		}
		return eor;
	}
	
	
	/*
	 * OrExpression ::= AndExpression ( | AndExpression )* 
	 */
	public Expression orExpression() throws SyntaxException {
		Token first = t;
		Expression eleft = andExpression();
		while (isKind(OP_OR)) {
			Token op = consume();
			Expression eright = andExpression();
			eleft = new ExpressionBinary(first, eleft, op, eright);
		}
		return eleft;
	}
	
	
	/*
	 * OrExpression ::= EqExpression ( & EqExpression ) *
	 */
	public Expression andExpression() throws SyntaxException {
		Token first = t;
		Expression eleft = eqExpression();
		while (isKind(OP_AND)) {
			Token op = consume();
			Expression eright = eqExpression();
			eleft = new ExpressionBinary(first, eleft, op, eright);
		}
		return eleft;
	}
	
	
	/*
	 * EqExpression::= RelExpression (( == | != ) RelExpression)*
	 */
	public Expression eqExpression() throws SyntaxException {
		Token first = t;
		Expression eleft = relExpression();
		while (isKind(OP_EQ) || isKind(OP_NEQ)) {
			Token op = consume();
			Expression eright = relExpression();
			eleft = new ExpressionBinary(first, eleft, op, eright);
		}
		return eleft;
	}
	
	
	/*
	 * RelExpression ::= AddExpression ((<  |  > | <= | >=) AddExpression)*
	 */

	Kind[] INEQ_OP = {OP_GT, OP_LT, OP_GE, OP_LE};
	
	public Expression relExpression() throws SyntaxException {
		Token first = t;
		Expression eleft = addExpression();
		while (isKind(INEQ_OP)) {
			Token op = consume();
			Expression eright = addExpression();
			eleft = new ExpressionBinary(first, eleft, op, eright);
		}	
		return eleft;
	}
	
	
	/*
	 * AddExpression ::= MultExpression ((+ | -) MultExpression)*
	 */
	public Expression addExpression() throws SyntaxException {
		Token first = t;
		Expression eleft = multExpression();
		while (isKind(OP_PLUS) || isKind(OP_MINUS)) {
			Token op = consume();
			Expression eright = multExpression();
			eleft = new ExpressionBinary(first, eleft, op, eright);
		}
		return eleft;
	}
	
	
	/*
	 * MultExpression := PowerExpression ((* | / | % ) PowerExpression)*
	 */
	public Expression multExpression() throws SyntaxException {
		Token first = t;
		Expression eleft = powerExpression();
		while (isKind(OP_TIMES) || isKind(OP_DIV) || isKind(OP_MOD)) {
			Token op = consume();
			Expression eright = powerExpression();
			eleft = new ExpressionBinary(first, eleft, op, eright);
		}
		return eleft;
	}
	
	
	/*
	 * PowerExpression := UnaryExpression (** PowerExpression | ε)
	 */
	public Expression powerExpression() throws SyntaxException {
		Token first = t;
		Expression eleft = unaryExpression();
		if (isKind(OP_POWER)) {
			Token op = consume();
			Expression eright = powerExpression();
			eleft = new ExpressionBinary(first, eleft, op, eright);
		}
		return eleft;
	}
	
	
	/*
	 * UnaryExpression ::= + UnaryExpression | - UnaryExpression | UnaryExpressionNotPlusMinus
	 */
	public Expression unaryExpression() throws SyntaxException {
		Token first = t;
		if (isKind(OP_PLUS) || isKind(OP_MINUS) || isKind(OP_EXCLAMATION)) {
			Token op = consume();
			Expression e = unaryExpression();
			return new ExpressionUnary(first, op, e);
		}
		else
			return primary();
	}
	
	
	/*
	 * Primary ::= INTEGER_LITERAL | BOOLEAN_LITERAL | FLOAT_LITERAL | (Expression) | FunctionApplication 
	 * | IDENTIFIER | PixelExpression| PredefinedName | PixelConstructor
	 */
	public Expression primary() throws SyntaxException {
		Token first = t, name = null;
		Expression e = null;
		switch (t.kind) {
			case INTEGER_LITERAL:
				Token intLit = consume();
				return new ExpressionIntegerLiteral(first, intLit);
			case FLOAT_LITERAL:
				Token floatLit = consume();
				return new ExpressionFloatLiteral(first, floatLit);
			case BOOLEAN_LITERAL:
				Token boolLit = consume();
				return new ExpressionBooleanLiteral(first, boolLit);
				
			case LPAREN:		//(Expression)
				consume();
				e = expression();
				match(RPAREN);
				return e;
			
			case KW_sin:		//FunctionApplication ::= FunctionName (Expression) | FunctionName [Expression, Expression]
			case KW_cos:
			case KW_atan:
			case KW_abs:
			case KW_log:
			case KW_cart_x:
			case KW_cart_y:
			case KW_polar_a:
			case KW_polar_r:
			case KW_int:
			case KW_float:
			case KW_width:
			case KW_height:
			case KW_red:
			case KW_green:
			case KW_blue:
			case KW_alpha:
				name = consume();
				
				if (isKind(LPAREN)) {
					consume();
					e = expression();
					match(RPAREN);
					return new ExpressionFunctionAppWithExpressionArg(first, name, e);
				}
				else if (isKind(LSQUARE)) {
					consume();
					Expression ex = expression();
					match(COMMA);
					Expression ey = expression();
					match(RSQUARE);
					return new ExpressionFunctionAppWithPixel(first, name, ex, ey);
				}
				else {
					throw new SyntaxException(t, "expecting a LPAREN or LSQUARE");
					//return null; //TODO Check it
				}
				
			case IDENTIFIER: 	//Identifier and PixelExpression ::= Identifier PixelSelector
				name = consume();
				if (isKind(LSQUARE)) {
					PixelSelector pixel = pixelSelector();
					return new ExpressionPixel(first, name, pixel);
				}
				return new ExpressionIdent(first, name);
				
			case KW_Z:			//PredefinedName ::= Z | default_height | default_width
			case KW_default_height:
			case KW_default_width:
				name = consume();
				return new ExpressionPredefinedName(first, name);
				
			case LPIXEL: 		//PixelConstructor ::= <<Expression, Expression, Expression, Expression>>
				consume();
				Expression alpha = expression();
				match(COMMA);
				Expression red = expression();
				match(COMMA);
				Expression green = expression();
				match(COMMA);
				Expression blue = expression();
				match(RPIXEL);
				return new ExpressionPixelConstructor(first, alpha, red, green, blue);
				
			default:
				throw new SyntaxException(t, "unacceptable token in this format");
		}
	}

	
	protected boolean isKind(Kind kind) {
		return t.kind == kind;
	}

	
	protected boolean isKind(Kind... kinds) {
		for (Kind k : kinds) {
			if (k == t.kind)
				return true;
		}
		return false;
	}

	/**
	 * Precondition: kind != EOF
	 * 
	 * @param kind
	 * @return
	 * @throws SyntaxException
	 */
	private Token match(Kind kind) throws SyntaxException {
		Token tmp = t;
		if (isKind(kind)) {
			consume();
			return tmp;
		}
		throw new SyntaxException(t,"Syntax Error: Expected " + kind + " at " + t.pos + " but instead got " + t.kind);
	}

	
	private Token consume() throws SyntaxException {
		Token tmp = t;
		if (isKind(EOF)) {
			throw new SyntaxException(t,"Syntax Error: Expected " + t.kind + " at " + t.pos + " but instead got " + EOF + " token");   
			//Note that EOF should be matched by the matchEOF method which is called only in parse().  
			//Anywhere else is an error. */
		}
		t = scanner.nextToken();
		return tmp;
	}


	/**
	 * Only for check at end of program. Does not "consume" EOF so no attempt to get
	 * nonexistent next Token.
	 * 
	 * @return
	 * @throws SyntaxException
	 */
	private Token matchEOF() throws SyntaxException {
		if (isKind(EOF)) {
			return t;
		}
		throw new SyntaxException(t,"Syntax Error"); //TODO  give a better error message!
	}
}