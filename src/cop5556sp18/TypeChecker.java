package cop5556sp18;

import cop5556sp18.Scanner.Kind;
import cop5556sp18.Scanner.Token;
import cop5556sp18.AST.ASTNode;
import cop5556sp18.AST.ASTVisitor;
import cop5556sp18.AST.Block;
import cop5556sp18.AST.Declaration;
import cop5556sp18.AST.ExpressionBinary;
import cop5556sp18.AST.ExpressionBooleanLiteral;
import cop5556sp18.AST.ExpressionConditional;
import cop5556sp18.AST.ExpressionFloatLiteral;
import cop5556sp18.AST.ExpressionFunctionAppWithExpressionArg;
import cop5556sp18.AST.ExpressionFunctionAppWithPixel;
import cop5556sp18.AST.ExpressionIdent;
import cop5556sp18.AST.ExpressionIntegerLiteral;
import cop5556sp18.AST.ExpressionPixel;
import cop5556sp18.AST.ExpressionPixelConstructor;
import cop5556sp18.AST.ExpressionPredefinedName;
import cop5556sp18.AST.ExpressionUnary;
import cop5556sp18.AST.LHSIdent;
import cop5556sp18.AST.LHSPixel;
import cop5556sp18.AST.LHSSample;
import cop5556sp18.AST.PixelSelector;
import cop5556sp18.AST.Program;
import cop5556sp18.AST.StatementAssign;
import cop5556sp18.AST.StatementIf;
import cop5556sp18.AST.StatementInput;
import cop5556sp18.AST.StatementShow;
import cop5556sp18.AST.StatementSleep;
import cop5556sp18.AST.StatementWhile;
import cop5556sp18.AST.StatementWrite;

import cop5556sp18.Types;
import cop5556sp18.Types.Type;

public class TypeChecker implements ASTVisitor {

	String errorMsg = "";

	TypeChecker() {
	}

	@SuppressWarnings("serial")
	public static class SemanticException extends Exception {
		Token t;

		public SemanticException(Token t, String message) {
			super(message);
			this.t = t;
		}
	}

	SymbolTable symbolTable = new SymbolTable();

	// Name is only used for naming the output file.
	// Visit the child block to type check program.
	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		program.block.visit(this, arg);
		return null;
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		symbolTable.enterScope();

		for (ASTNode decOrStatement : block.decsOrStatements)
			decOrStatement.visit(this, arg);

		symbolTable.leaveScope();
		return null;
	}

	@Override
	public Object visitDeclaration(Declaration declaration, Object arg) throws Exception {
		if (symbolTable.inCurrentScope(declaration.name)) {
			errorMsg = "Name already used in the present scope";
			throw new SemanticException(declaration.firstToken, errorMsg);
		}

		if (declaration.width != null) {
			declaration.width.visit(this, arg);
			if (!(declaration.width.type == Type.INTEGER && Types.getType(declaration.type) == Type.IMAGE)) {
				errorMsg = "Improper semantics.";
				throw new SemanticException(declaration.firstToken, errorMsg);
			}
		}

		if (declaration.height != null) {
			declaration.height.visit(this, arg);
			if (!(declaration.height.type == Type.INTEGER && Types.getType(declaration.type) == Type.IMAGE)) {
				errorMsg = "Improper semantics.";
				throw new SemanticException(declaration.firstToken, errorMsg);
			}
		}

		if (!((declaration.height == null) == (declaration.width == null))) {
			errorMsg = "Improper semantics.";
			throw new SemanticException(declaration.firstToken, errorMsg);
		}

		symbolTable.insert(declaration.name, declaration);
		return null;
	}

	@Override
	public Object visitStatementWrite(StatementWrite statementWrite, Object arg) throws Exception {

		statementWrite.sourceDec = symbolTable.lookup(statementWrite.sourceName);

		if (statementWrite.sourceDec == null) {
			errorMsg = "source declaration is null";
			throw new SemanticException(statementWrite.firstToken, errorMsg);
		}

		statementWrite.destDec = symbolTable.lookup(statementWrite.destName);

		if (statementWrite.destDec == null) {
			errorMsg = "dest declaration is null";
			throw new SemanticException(statementWrite.firstToken, errorMsg);
		}

		if (Types.getType(statementWrite.sourceDec.type) != Type.IMAGE) {
			errorMsg = "source declaration type is not image";
			throw new SemanticException(statementWrite.firstToken, errorMsg);
		}

		if (Types.getType(statementWrite.destDec.type) != Type.FILE) {
			errorMsg = "dest declaration type is not file";
			throw new SemanticException(statementWrite.firstToken, errorMsg);
		}

		return null;
	}

	@Override
	public Object visitStatementInput(StatementInput statementInput, Object arg) throws Exception {

		statementInput.dec = symbolTable.lookup(statementInput.destName);

		if (statementInput.dec == null) {
			errorMsg = "declaration is null";
			throw new SemanticException(statementInput.firstToken, errorMsg);
		}

		statementInput.e.visit(this, arg);

		if (statementInput.e.type != Type.INTEGER) {
			errorMsg = "statement input expression isn't of int type";
			throw new SemanticException(statementInput.firstToken, errorMsg);
		}

		return null;
	}

	@Override
	public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
		pixelSelector.ex.visit(this, arg);
		pixelSelector.ey.visit(this, arg);

		if (pixelSelector.ex.type != pixelSelector.ey.type) {
			errorMsg = "pixel selector parameters aren't of the same type";
			throw new SemanticException(pixelSelector.firstToken, errorMsg);
		}

		if ((pixelSelector.ex.type != Type.INTEGER) && (pixelSelector.ex.type != Type.FLOAT)) {
			errorMsg = "pixel selector parameters aren't of float or int type";
			throw new SemanticException(pixelSelector.firstToken, errorMsg);
		}

		return null;
	}

	@Override
	public Object visitExpressionConditional(ExpressionConditional expressionConditional, Object arg) throws Exception {
		expressionConditional.guard.visit(this, arg);
		expressionConditional.trueExpression.visit(this, arg);
		expressionConditional.falseExpression.visit(this, arg);

		if (expressionConditional.guard.type != Type.BOOLEAN) {
			errorMsg = "The guard parameter type isn't boolean";
			throw new SemanticException(expressionConditional.firstToken, errorMsg);
		}

		if (expressionConditional.trueExpression.type != expressionConditional.falseExpression.type) {
			errorMsg = "The true and false expression parameters type aren't the same";
			throw new SemanticException(expressionConditional.firstToken, errorMsg);
		}

		expressionConditional.type = expressionConditional.trueExpression.type;

		return null;
	}

	@Override
	public Object visitExpressionBinary(ExpressionBinary eb, Object arg) throws Exception {

		Kind[] III = { Kind.OP_PLUS, Kind.OP_MINUS, Kind.OP_TIMES, Kind.OP_DIV, Kind.OP_MOD, Kind.OP_POWER, Kind.OP_OR,
				Kind.OP_AND };
		Kind[] FkF = { Kind.OP_PLUS, Kind.OP_MINUS, Kind.OP_TIMES, Kind.OP_DIV, Kind.OP_POWER };
		Kind[] kkB = { Kind.OP_EQ, Kind.OP_NEQ, Kind.OP_GT, Kind.OP_LT, Kind.OP_LE, Kind.OP_GE };
		Kind[] BBB = { Kind.OP_AND, Kind.OP_OR };

		eb.type = null;
		eb.leftExpression.visit(this, arg);
		eb.rightExpression.visit(this, arg);

		if ((eb.leftExpression.type == Type.INTEGER) && (eb.rightExpression.type == Type.INTEGER)) {
			for (Kind k : III)
				if (k == eb.op) {
					eb.type = Type.INTEGER;
					break;
				}

			if (eb.type == null)
				for (Kind k : kkB)
					if (k == eb.op) {
						eb.type = Type.BOOLEAN;
						break;
					}
		}

		else if (((eb.leftExpression.type == Type.INTEGER) && (eb.rightExpression.type == Type.FLOAT))
				|| ((eb.leftExpression.type == Type.FLOAT) && (eb.rightExpression.type == Type.INTEGER))
				|| ((eb.leftExpression.type == Type.FLOAT) && (eb.rightExpression.type == Type.FLOAT))) {

			for (Kind k : FkF) {
				if (k == eb.op) {
					eb.type = Type.FLOAT;
					break;
				}
			}

			if ((eb.type == null) && (eb.leftExpression.type == Type.FLOAT)
					&& (eb.rightExpression.type == Type.FLOAT)) {
				for (Kind k : kkB) {
					if (k == eb.op) {
						eb.type = Type.BOOLEAN;
						break;
					}
				}
			}
		}

		else if ((eb.leftExpression.type == Type.BOOLEAN) && (eb.rightExpression.type == Type.BOOLEAN)) {
			for (Kind k : kkB) {
				if (k == eb.op) {
					eb.type = Type.BOOLEAN;
					break;
				}
			}

			if (eb.type == null) {
				for (Kind k : BBB) {
					if (k == eb.op) {
						eb.type = Type.BOOLEAN;
						break;
					}
				}
			}
		}

		if (eb.type == null) {
			errorMsg = "incompatible types on expression binary";
			throw new SemanticException(eb.firstToken, errorMsg);
		}

		return null;
	}

	@Override
	public Object visitExpressionUnary(ExpressionUnary expressionUnary, Object arg) throws Exception {

		expressionUnary.expression.visit(this, arg);
		// TODO Doubt
		expressionUnary.type = expressionUnary.expression.type;

		return null;
	}

	@Override
	public Object visitExpressionIntegerLiteral(ExpressionIntegerLiteral expressionIntegerLiteral, Object arg)
			throws Exception {

		expressionIntegerLiteral.type = Type.INTEGER;

		return null;
	}

	@Override
	public Object visitBooleanLiteral(ExpressionBooleanLiteral expressionBooleanLiteral, Object arg) throws Exception {

		expressionBooleanLiteral.type = Type.BOOLEAN;

		return null;
	}

	@Override
	public Object visitExpressionPredefinedName(ExpressionPredefinedName expressionPredefinedName, Object arg)
			throws Exception {

		expressionPredefinedName.type = Type.INTEGER;
		return null;
	}

	@Override
	public Object visitExpressionFloatLiteral(ExpressionFloatLiteral expressionFloatLiteral, Object arg)
			throws Exception {

		expressionFloatLiteral.type = Type.FLOAT;

		return null;
	}

	@Override
	public Object visitExpressionFunctionAppWithExpressionArg(
			ExpressionFunctionAppWithExpressionArg efawe, Object arg)
			throws Exception {
		
		efawe.e.visit(this, arg);
		
		errorMsg = "incompatible types";
		Type etype = efawe.e.type;
		
		switch (efawe.function) {
		case KW_red:
		case KW_blue:
		case KW_green:
		case KW_alpha:
			if (etype.equals(Type.INTEGER))
				efawe.type = etype;
			else
				throw new SemanticException(efawe.firstToken, errorMsg);
			break;
		case KW_abs:
			if (etype.equals(Type.INTEGER) || (etype.equals(Type.FLOAT)))
				efawe.type = etype;
			else 
				throw new SemanticException(efawe.firstToken, errorMsg);
			break;
		case KW_width:
		case KW_height:
			if (etype.equals(Type.IMAGE))
				efawe.type = Type.INTEGER;
			else
				throw new SemanticException(efawe.firstToken, errorMsg);
			break;
		case KW_sin:
		case KW_cos:
		case KW_atan:
		case KW_log:
			if (etype.equals(Type.FLOAT))
				efawe.type = etype;
			else
				throw new SemanticException(efawe.firstToken, errorMsg);
			break;
		case KW_float:
			if (etype.equals(Type.FLOAT) || etype.equals(Type.INTEGER))
				efawe.type = Type.FLOAT;
			else
				throw new SemanticException(efawe.firstToken, errorMsg);
			break;
		case KW_int:
			if (etype.equals(Type.FLOAT) || etype.equals(Type.INTEGER))
				efawe.type = Type.INTEGER;
			else
				throw new SemanticException(efawe.firstToken, errorMsg);
			break;
		default:
			throw new SemanticException(efawe.firstToken, errorMsg);	
		}
		
		return null;
	}

	@Override
	public Object visitExpressionFunctionAppWithPixel(ExpressionFunctionAppWithPixel efawp, Object arg)
			throws Exception {

		efawp.e0.visit(this, arg);
		efawp.e1.visit(this, arg);

		if (efawp.name == Kind.KW_cart_x || efawp.name == Kind.KW_cart_y) {
			if (efawp.e0.type != Type.FLOAT || efawp.e1.type != Type.FLOAT) {
				errorMsg = "Expressions inside expression function app with pixel aren't matching";
				throw new SemanticException(efawp.firstToken, errorMsg);
			}
			efawp.type = Type.INTEGER;
		}

		else if (efawp.name == Kind.KW_polar_a || efawp.name == Kind.KW_polar_r) {
			if (efawp.e0.type != Type.INTEGER || efawp.e1.type != Type.INTEGER) {
				errorMsg = "Expressions inside expression function app with pixel aren't matching";
				throw new SemanticException(efawp.firstToken, errorMsg);
			}
			efawp.type = Type.FLOAT;
		}

		return null;
	}

	@Override
	public Object visitExpressionPixelConstructor(ExpressionPixelConstructor epc, Object arg) throws Exception {

		epc.alpha.visit(this, arg);
		epc.red.visit(this, arg);
		epc.blue.visit(this, arg);
		epc.green.visit(this, arg);

		if ((epc.alpha.type != Type.INTEGER) || (epc.blue.type != Type.INTEGER) || (epc.red.type != Type.INTEGER)
				|| (epc.green.type != Type.INTEGER)) {
			errorMsg = "Not all the parameters of exp pixel constructor are integers";
			throw new SemanticException(epc.firstToken, errorMsg);

		}

		epc.type = Type.INTEGER;

		return null;
	}

	@Override
	public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws Exception {
		statementAssign.lhs.visit(this, arg);
		statementAssign.e.visit(this, arg);

		System.out.print(statementAssign.e.type + " " + statementAssign.lhs.type);
		if (statementAssign.lhs.type != statementAssign.e.type) {
			errorMsg = "LHS and expression types aren't matching";
			throw new SemanticException(statementAssign.firstToken, errorMsg);
		}

		return null;
	}

	@Override
	public Object visitStatementShow(StatementShow statementShow, Object arg) throws Exception {
		statementShow.e.visit(this, arg);

		if ((statementShow.e.type != Type.BOOLEAN) && (statementShow.e.type != Type.INTEGER)
				&& (statementShow.e.type != Type.FLOAT) && (statementShow.e.type != Type.IMAGE)) {
			errorMsg = "expression in show condition is giving improper type.";
			throw new SemanticException(statementShow.firstToken, errorMsg);
		}

		return null;
	}

	@Override
	public Object visitExpressionPixel(ExpressionPixel expressionPixel, Object arg) throws Exception {

		expressionPixel.pixelSelector.visit(this, arg);
		expressionPixel.dec = symbolTable.lookup(expressionPixel.name);

		if (expressionPixel.dec == null) {
			errorMsg = "expression pixel declaration is null";
			throw new SemanticException(expressionPixel.firstToken, errorMsg);
		}

		if (Types.getType(expressionPixel.dec.type) != Type.IMAGE) {
			errorMsg = "expression pixel declaration type isn't image";
			throw new SemanticException(expressionPixel.firstToken, errorMsg);
		}

		expressionPixel.type = Type.INTEGER;

		return null;
	}

	@Override
	public Object visitExpressionIdent(ExpressionIdent expressionIdent, Object arg) throws Exception {

		expressionIdent.dec = symbolTable.lookup(expressionIdent.name);

		if (expressionIdent.dec == null) {
			errorMsg = "expression ident declaration is null";
			throw new SemanticException(expressionIdent.firstToken, errorMsg);
		}

		expressionIdent.type = Types.getType(expressionIdent.dec.type);

		return null;
	}

	@Override
	public Object visitLHSSample(LHSSample lhsSample, Object arg) throws Exception {

		lhsSample.pixelSelector.visit(this, arg);
		// TODO: lhsSample.color.visit(this, arg;)

		lhsSample.dec = symbolTable.lookup(lhsSample.name);

		if (lhsSample.dec == null) {
			errorMsg = "LHS Sample declaration is null";
			throw new SemanticException(lhsSample.firstToken, errorMsg);
		}

		if (Types.getType(lhsSample.dec.type) != Type.IMAGE) {
			errorMsg = "LHS Sample declaration type isn't image";
			throw new SemanticException(lhsSample.firstToken, errorMsg);
		}

		lhsSample.type = Type.INTEGER;

		return null;
	}

	@Override
	public Object visitLHSPixel(LHSPixel lhsPixel, Object arg) throws Exception {

		lhsPixel.pixelSelector.visit(this, arg);

		lhsPixel.dec = symbolTable.lookup(lhsPixel.name);

		if (lhsPixel.dec == null) {
			errorMsg = "LHS Pixel declaration is null";
			throw new SemanticException(lhsPixel.firstToken, errorMsg);
		}

		if (Types.getType(lhsPixel.dec.type) != Type.IMAGE) {
			errorMsg = "LHS Pixel declaration type isn't image";
			throw new SemanticException(lhsPixel.firstToken, errorMsg);
		}

		lhsPixel.type = Type.INTEGER;

		return null;
	}

	@Override
	public Object visitLHSIdent(LHSIdent lhsIdent, Object arg) throws Exception {
		lhsIdent.dec = symbolTable.lookup(lhsIdent.name);

		if (lhsIdent.dec == null) {
			errorMsg = "LHS Ident declaration is null";
			throw new SemanticException(lhsIdent.firstToken, errorMsg);
		}

		lhsIdent.type = Types.getType(lhsIdent.dec.type);

		return null;
	}

	@Override
	public Object visitStatementIf(StatementIf statementIf, Object arg) throws Exception {
		statementIf.guard.visit(this, arg);
		statementIf.b.visit(this, arg);

		if (statementIf.guard.type != Type.BOOLEAN) {
			errorMsg = "expression in if condition isn't giving bool type.";
			throw new SemanticException(statementIf.firstToken, errorMsg);
		}

		return null;
	}

	@Override
	public Object visitStatementWhile(StatementWhile statementWhile, Object arg) throws Exception {
		statementWhile.guard.visit(this, arg);
		statementWhile.b.visit(this, arg);

		if (statementWhile.guard.type != Type.BOOLEAN) {
			errorMsg = "expression in while condition isn't giving bool type.";
			throw new SemanticException(statementWhile.firstToken, errorMsg);
		}

		return null;
	}

	@Override
	public Object visitStatementSleep(StatementSleep statementSleep, Object arg) throws Exception {
		statementSleep.duration.visit(this, arg);

		if (statementSleep.duration.type != Type.INTEGER) {
			errorMsg = "expression in sleep condition isn't giving integer type.";
			throw new SemanticException(statementSleep.firstToken, errorMsg);
		}

		return null;
	}
}
