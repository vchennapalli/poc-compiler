package cop5556sp18;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import cop5556sp18.Parser;
import cop5556sp18.Scanner;
import cop5556sp18.AST.ASTVisitor;
import cop5556sp18.AST.Program;
import cop5556sp18.TypeChecker.SemanticException;

public class TypeCheckerTest {

	/*
	 * set Junit to be able to catch exceptions
	 */
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	/**
	 * Prints objects in a way that is easy to turn on and off
	 */
	static final boolean doPrint = true;

	private void show(Object input) {
		if (doPrint) {
			System.out.println(input.toString());
		}
	}

	/**
	 * Scans, parses, and type checks the input string
	 * 
	 * @param input
	 * @throws Exception
	 */
	void typeCheck(String input) throws Exception {
		show(input);
		// instantiate a Scanner and scan input
		Scanner scanner = new Scanner(input).scan();
		show(scanner);
		// instantiate a Parser and parse input to obtain and AST
		Program ast = new Parser(scanner).parse();
		show(ast);
		// instantiate a TypeChecker and visit the ast to perform type checking and
		// decorate the AST.
		ASTVisitor v = new TypeChecker();
		ast.visit(v, null);
	}



	/**
	 * Simple test case with an almost empty program.
	 * 
	 * @throws Exception
	 */
	
	
	
	
	@Test
	public void emptyProg() throws Exception {
		String input = "emptyProg{}";
		typeCheck(input);
	}

	@Test
	public void expression1() throws Exception {
		String input = "prog {show 3+4;}";
		typeCheck(input);
	}
	
	@Test
	public void expression12() throws Exception {
		String input = "prog {show 3+4;}";
		typeCheck(input);
	}
	@Test
	public void expression2_fail() throws Exception {
		String input = "prog { show true+4; }"; //error, incompatible types in binary expression
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void declaration0() throws Exception {
		String input = "prog {image a[12, 12]; image a;}"; //redeclaration
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		}
		catch(SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void declaration1() throws Exception {
		String input = "prog {image a; show a;}";
		typeCheck(input);
	}
	
	@Test
	public void statementShow0() throws Exception {
		String input = "prog {show a;}"; //no declaration before use
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		}
		catch(SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void statementShow1() throws Exception {
		String input = "prog {image a[3,4]; show a;}"; 
		typeCheck(input);
	}
	
	@Test
	public void statementWrite0() throws Exception {
		String input = "prog {image a[3,4]; int b; write a to b;}"; 
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		}
		catch(SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void statementWrite1() throws Exception {
		String input = "prog {int a; int b; write a to b;}"; 
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		}
		catch(SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void statementWrite2() throws Exception {
		String input = "prog {image a[3,4]; filename b; write a to b;}"; 
		typeCheck(input);
	}
	
	@Test
	public void statementInput0() throws Exception {
		String input = "prog {int a; int b; input a from @ b;}"; 
		typeCheck(input);
	}
	
	@Test
	public void statementInput1() throws Exception {
		String input = "prog {int a; float b; input a from @ b;}"; 
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		}
		catch(SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void statementInput2() throws Exception {
		String input = "prog {int a; int b; input a from @ 1+2;}"; 
		typeCheck(input);
	}
	
	@Test
	public void statementInput3() throws Exception {
		String input = "prog {int a; int b; input a from @ 1*2+3;}"; 
		typeCheck(input);
	}
	
	@Test
	public void statementAssignment0() throws Exception {
		String input = "prog {int b; b := 3;}"; 
		typeCheck(input);
	}
	
	@Test
	public void simpleImage() throws Exception {
		String input = "X{ image im[1,2]; }";
		typeCheck(input);
	}
	
	@Test
	public void simpleImageFail() throws Exception {
		String input = "X{ image im[1.0, 2]; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void nestedDec1() throws Exception {
		String input = "X{ int x; int y; while (x == y) {int x;}; }";
		typeCheck(input);
	}
	
	@Test
	public void nestedDec2() throws Exception {
		String input = "X{ int x; int z; while (x == y) {int x;}; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
		public void nestedDec3() throws Exception {
			String input = "X{ int x; int y; while (x == y) { show x;}; }";
			typeCheck(input);
	}
	
	@Test
	public void nestedDec4() throws Exception {
		String input = "X{ int x; int y; while (x == y) { int z;}; show z;}";
		thrown.expect(SemanticException.class);
		try { typeCheck(input); } catch (SemanticException e) { show(e); throw e; }
	}

	@Test
	public void visitDeclaration1() throws Exception {
		String input = "prog { int x; }";
		typeCheck(input);
	}
	
	@Test
	public void visitDeclaration2() throws Exception {
		String input = "prog { int x; int x;}";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitDeclaration3() throws Exception {
		String input = "prog { int x; int y; int z; z:=x+y;}";
		typeCheck(input);
	}
	
	@Test
	public void visitDeclaration4() throws Exception {
		String input = "prog { image a[1.2,3]; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	// scope - start
	
	@Test
	public void visitDeclaration5() throws Exception {
		String input = "prog { int x; while(true) { int x; }; }";
		typeCheck(input);
	}
	
	@Test
	public void visitDeclaration6() throws Exception {
		String input = "prog { int x; while(true) { int z; z := x; }; }";
		typeCheck(input);
	}
	
	@Test
	public void visitDeclaration7() throws Exception {
		String input = "prog { if (true) { int z; }; int y; y := z; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	// scope - end
	
	@Test
	public void visitStatementInput1() throws Exception {
		String input = "prog { image abc; input abc from @ 23;}";
		typeCheck(input);
	}
	
	@Test
	public void visitStatementInput2() throws Exception {
		String input = "prog { input abc1 from @ 23;}";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitStatementInput3() throws Exception {
		String input = "prog { image abc2; input abc2 from @ 2.5;}";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitStatementInput4() throws Exception {
		String input = "prog { input abc3 from @ 23; image abc3;}";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitStatementWrite1() throws Exception {
		String input = "prog { image writeSrc1; filename writeDest1; write writeSrc1 to writeDest1; }";
		typeCheck(input);
	}
	
	@Test
	public void visitStatementWrite2() throws Exception {
		String input = "prog { filename writeDest2; write writeSrc2 to writeDest2; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitStatementWrite3() throws Exception {
		String input = "prog { image writeSrc3; write writeSrc3 to writeDest3; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitStatementWrite4() throws Exception {
		String input = "prog { int writeSrc4; filename writeDest4; write writeSrc4 to writeDest4; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitStatementWrite5() throws Exception {
		String input = "prog { image writeSrc5; int writeDest5; write writeSrc5 to writeDest5; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitStatementAssign1() throws Exception {
		String input = "prog { image bird1; image bird2; int x; int y; blue(bird1[x,y]):=red(bird2[x,y]); }";
		typeCheck(input);
	}
	
	@Test
	public void visitStatementAssign2() throws Exception {
		String input = "prog { int assign1; float assign2; assign1:=assign2; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitStatementWhile1() throws Exception {
		String input = "prog { while(true) {}; }";
		typeCheck(input);
	}
	
	@Test
	public void visitStatementWhile2() throws Exception {
		String input = "prog { while(false) {}; }";
		typeCheck(input);
	}
	
	@Test
	public void visitStatementWhile3() throws Exception {
		String input = "prog { while(0) {}; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitStatementIf1() throws Exception {
		String input = "prog { if(true) {}; }";
		typeCheck(input);
	}
	
	@Test
	public void visitStatementIf2() throws Exception {
		String input = "prog { if(false) {}; }";
		typeCheck(input);
	}
	
	@Test
	public void visitStatementIf3() throws Exception {
		String input = "prog { if(0) {}; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitStatementShow1() throws Exception {
		String input = "prog { int show1; boolean show2; float show3; image show4; show show1; show show2; show show3; show show4; }";
		typeCheck(input);
	}
	
	@Test
	public void visitStatementShow2() throws Exception {
		String input = "prog { filename show5; show show5; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitStatementSleep1() throws Exception {
		String input = "prog { int sleep1; sleep sleep1; }";
		typeCheck(input);
	}
	
	@Test
	public void visitStatementSleep2() throws Exception {
		String input = "prog { float sleep2; sleep sleep2; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitStatementSleep3() throws Exception {
		String input = "prog { image sleep3; sleep sleep3; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitLHSIdent1() throws Exception {
		String input = "prog { int lhs1; lhs1 := 10; }";
		typeCheck(input);
	}
	
	@Test
	public void visitLHSIdent2() throws Exception {
		String input = "prog { lhs2 := 10; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitLHSPixel1() throws Exception {
		String input = "prog { image lhspixel1[10,20]; image lhspixel2[20,30]; lhspixel1[1,1] := lhspixel2[2,2]; }";
		typeCheck(input);
	}
	
	@Test
	public void visitLHSPixel2() throws Exception {
		String input = "prog { image lhspixel2[20,30]; lhspixel1[1,2] := lhspixel2[2,3]; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitLHSPixel3() throws Exception {
		String input = "prog { int lhspixel1; image lhspixel2[20,30]; lhspixel1[1,2] := lhspixel2[2,3]; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitLHSSample1() throws Exception {
		String input = "prog { image lhssample1[10,20]; image lhssample2[20,30]; int x; int y; red(lhssample1[x,y]) :=  green(lhssample2[x,y]); }";
		typeCheck(input);
	}
	
	@Test
	public void visitLHSSample2() throws Exception {
		String input = "prog { image lhssample2[20,30]; int x; int y; red(lhssample1[x,y]) :=  green(lhssample2[x,y]); }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitLHSSample3() throws Exception {
		String input = "prog { int lhssample1; image lhssample2[20,30]; int x; int y; red(lhssample1[x,y]) :=  green(lhssample2[x,y]); }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitPixelSelector1() throws Exception {
		String input = "prog { image pixelSelector1; image pixelSelector2; pixelSelector1[1,1] := pixelSelector2[5,5]; }";
		typeCheck(input);
	}
	
	@Test
	public void visitPixelSelector2() throws Exception {
		String input = "prog { image pixelSelector1; image pixelSelector2; pixelSelector1[1,2] := pixelSelector2[5,5.0]; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitPixelSelector3() throws Exception {
		String input = "prog { image pixelSelector1; image pixelSelector2; pixelSelector1[1,2] := pixelSelector2[true,false]; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitExpressionConditional1() throws Exception {
		String input = "prog { int a; a:= 2 < 3 ? 1 : 0; }";
		typeCheck(input);
	}
	
	@Test
	public void visitExpressionConditional2() throws Exception {
		String input = "prog { int a; a:= 2 < 3 ? 1.0 : 0; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitExpressionConditional3() throws Exception {
		String input = "prog { int a; a:= 5+3 ? 1 : 0; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitExpressionBinary1() throws Exception {
		String input = "prog { int integer; "
				+ "integer := integer + integer;"
				+ "integer := integer - integer;"
				+ "integer := integer * integer;"
				+ "integer := integer / integer;"
				+ "integer := integer % integer;"
				+ "integer := integer ** integer;"
				+ "integer := integer & integer;"
				+ "integer := integer | integer;"
				+ "}";
		typeCheck(input);
	}
	
	@Test
	public void visitExpressionBinary2() throws Exception {
		String input = "prog { float decimal; "
				+ "decimal := decimal + decimal;"
				+ "decimal := decimal - decimal;"
				+ "decimal := decimal * decimal;"
				+ "decimal := decimal / decimal;"
				+ "decimal := decimal ** decimal;"
				+ "}";
		typeCheck(input);
	}
	
	@Test
	public void visitExpressionBinary3() throws Exception {
		String input = "prog { float decimal; decimal := decimal & decimal; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitExpressionBinary4() throws Exception {
		String input = "prog { float decimal; decimal := decimal | decimal; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitExpressionBinary5() throws Exception {
		String input = "prog { float decimal; int integer;"
				+ "decimal := decimal + integer;"
				+ "decimal := decimal - integer;"
				+ "decimal := decimal * integer;"
				+ "decimal := decimal / integer;"
				+ "decimal := decimal ** integer;"
				+ "}";
		typeCheck(input);
	}
	
	@Test
	public void visitExpressionBinary6() throws Exception {
		String input = "prog { float decimal; int integer;"
				+ "decimal := integer + decimal;"
				+ "decimal := integer - decimal;"
				+ "decimal := integer * decimal;"
				+ "decimal := integer / decimal;"
				+ "decimal := integer ** decimal;"
				+ "}";
		typeCheck(input);
	}
	
	@Test
	public void visitExpressionBinary7() throws Exception {
		String input = "prog { boolean bool;"
				+ "bool := bool & bool;"
				+ "bool := bool | bool;"
				+ "}";
		typeCheck(input);
	}
	
	@Test
	public void visitExpressionBinary8() throws Exception {
		String input = "prog { int integer; boolean bool;"
				+ "bool := integer == integer;"
				+ "bool := integer != integer;"
				+ "bool := integer > integer;"
				+ "bool := integer >= integer;"
				+ "bool := integer < integer;"
				+ "bool := integer <= integer;"
				+ "}";
		typeCheck(input);
	}
	
	@Test
	public void visitExpressionBinary9() throws Exception {
		String input = "prog { float decimal; boolean bool;"
				+ "bool := decimal == decimal;"
				+ "bool := decimal != decimal;"
				+ "bool := decimal > decimal;"
				+ "bool := decimal >= decimal;"
				+ "bool := decimal < decimal;"
				+ "bool := decimal <= decimal;"
				+ "}";
		typeCheck(input);
	}
	
	@Test
	public void visitExpressionBinary10() throws Exception {
		String input = "prog { boolean bool;"
				+ "bool := bool == bool;"
				+ "bool := bool != bool;"
				+ "bool := bool > bool;"
				+ "bool := bool >= bool;"
				+ "bool := bool < bool;"
				+ "bool := bool <= bool;"
				+ "}";
		typeCheck(input);
	}
	
	@Test
	public void visitExpressionBinary11() throws Exception {
		String input = "prog { boolean bool; bool := bool + bool; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitExpressionBinary12() throws Exception {
		String input = "prog { image im; im := im < im; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitExpressionBinary13() throws Exception {
		String input = "prog { float decimal; int integer;"
				+ "decimal := integer % decimal;"
				+ "}";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitExpressionUnary1() throws Exception {
		String input = "prog { boolean a; a:= !true; }";
		typeCheck(input);
	}
	
	@Test
	public void visitExpressionPixelConstructor1() throws Exception {
		String input = "prog { image im; im[0,0] := <<255,255,0,0>>; }";
		typeCheck(input);
	}
	
	@Test
	public void ExpressionPixelConstructor2() throws Exception {
		String input = "prog { image im; im[0,0] := <<255,255,0,1.0>>; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitExpressionIdent1() throws Exception {
		String input = "prog { int a; a:= 10; int b; b := a < 20 ? 1 : 0; }";
		typeCheck(input);
	}
	
	@Test
	public void visitExpressionIdent2() throws Exception {
		String input = "prog { int b; b := a < 20 ? 1 : 0; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitExpressionPixel1() throws Exception {
		String input = "prog { image pixelSelector1; pixelSelector1[1,1] := pixelSelector2[5,5]; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitExpressionFunctionAppWithExpressionArg1() throws Exception {
		String input = "prog { int a; a:= abs(1); a:= red(1); a:= green(1); a:= blue(1); a:= alpha(1); a := int(1.0); a:= int(1);}";
		typeCheck(input);
	}
	
	@Test
	public void visitExpressionFunctionAppWithExpressionArg2() throws Exception {
		String input = "prog { int a; a := sin(a); }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitExpressionFunctionAppWithExpressionArg3() throws Exception {
		String input = "prog { int a; a := cos(a); }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitExpressionFunctionAppWithExpressionArg4() throws Exception {
		String input = "prog { int a; a := atan(a); }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitExpressionFunctionAppWithExpressionArg5() throws Exception {
		String input = "prog { int a; a := log(a); }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitExpressionFunctionAppWithExpressionArg6() throws Exception {
		String input = "prog { int a; a := width(a); }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitExpressionFunctionAppWithExpressionArg7() throws Exception {
		String input = "prog { int a; a := height(a); }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitExpressionFunctionAppWithExpressionArg8() throws Exception {
		String input = "prog { float a; a:= abs(a); a:= sin(a); a:= cos(a); a:= atan(a); a:= log(a); a:=float(1); a:=float(1.0); }";
		typeCheck(input);
	}
	
	@Test
	public void visitExpressionFunctionAppWithExpressionArg9() throws Exception {
		String input = "prog { image a; int b; b:= width(a); b:= height(a); }";
		typeCheck(input);
	}
	
	@Test
	public void visitExpressionFunctionAppWithPixel1() throws Exception {
		String input = "prog { float a; int b; b := cart_x[a,a]; b := cart_y[a,a]; a := polar_a[b,b]; a := polar_r[b,b];}";
		typeCheck(input);
	}
	
	@Test
	public void visitExpressionFunctionAppWithPixel2() throws Exception {
		String input = "prog { float a; int b; b := cart_x[a,b]; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitExpressionFunctionAppWithPixel3() throws Exception {
		String input = "prog { float a; int b; b := polar_a[a,b]; }";
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	@Test
	public void visitExpressionPredefinedName1() throws Exception {
		String input = "prog { show(Z); }";
		typeCheck(input);
	}
}
