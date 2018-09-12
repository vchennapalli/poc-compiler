/**
 * Starter code for CodeGenerator.java used n the class project in COP5556 Programming Language Principles 
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


package cop5556sp18;

import java.util.HashMap;

import cop5556sp18.Scanner.Kind;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import cop5556sp18.Types;
import cop5556sp18.Types.Type;
import cop5556sp18.RuntimeImageSupport;

import cop5556sp18.AST.ASTNode;
import cop5556sp18.AST.ASTVisitor;
import cop5556sp18.AST.Block;
import cop5556sp18.AST.Declaration;
import cop5556sp18.AST.Expression;
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

import cop5556sp18.CodeGenUtils;

public class CodeGenerator implements ASTVisitor, Opcodes {

	/**
	 * All methods and variable static.
	 */

	static final int Z = 255;

	ClassWriter cw;
	String className;
	String classDesc;
	String sourceFileName;

	MethodVisitor mv; // visitor of method currently under construction

	/** Indicates whether genPrint and genPrintTOS should generate code. */
	final boolean DEVEL;
	final boolean GRADE;

	final int defaultWidth;
	final int defaultHeight;
	// final boolean itf = false;
	
	private Integer decCounter;
	/**
	 * @param DEVEL
	 *            used as parameter to genPrint and genPrintTOS
	 * @param GRADE
	 *            used as parameter to genPrint and genPrintTOS
	 * @param sourceFileName
	 *            name of source file, may be null.
	 * @param defaultWidth
	 *            default width of images
	 * @param defaultHeight
	 *            default height of images
	 */
	public CodeGenerator(boolean DEVEL, boolean GRADE, String sourceFileName,
			int defaultWidth, int defaultHeight) {
		super();
		this.DEVEL = DEVEL;
		this.GRADE = GRADE;
		this.sourceFileName = sourceFileName;
		this.defaultWidth = defaultWidth;
		this.defaultHeight = defaultHeight;
		this.decCounter = 1;
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		// TODO refactor and extend as necessary
		for (ASTNode node : block.decsOrStatements) {
			node.visit(this, null);
		}
		return null;
	}
	
	public void convertTopValueToDouble(Type type) {
		if (type == Type.INTEGER)
			mv.visitInsn(Opcodes.I2D);
		else if (type == Type.FLOAT)
			mv.visitInsn(Opcodes.F2D);
	}
	
	public void convertTopValueToPreviousType(Type type) {
		if (type == Type.INTEGER) 
			mv.visitInsn(Opcodes.D2I);
		else if (type == Type.FLOAT)
			mv.visitInsn(Opcodes.D2F);
	}

	@Override
	public Object visitBooleanLiteral(
			ExpressionBooleanLiteral expressionBooleanLiteral, Object arg)
			throws Exception {
		mv.visitLdcInsn(expressionBooleanLiteral.value);
		return null;
	}

	@Override
	public Object visitDeclaration(Declaration declaration, Object arg)
			throws Exception {
		//String decName = declaration.name;
		Kind decType = declaration.type;
		declaration.setSlot(decCounter++);
		
		if (Types.getType(decType) == Type.IMAGE) {
			if (declaration.width != null && declaration.height != null) {
				declaration.width.visit(this, arg);
				declaration.height.visit(this, arg);
			}
			else if (declaration.width == null && declaration.height == null) {
				mv.visitLdcInsn(this.defaultWidth);
				mv.visitLdcInsn(this.defaultHeight);
			}
			
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, 
					"makeImage", RuntimeImageSupport.makeImageSig, false);
			mv.visitVarInsn(Opcodes.ASTORE, declaration.getSlot());
		}
		this.decCounter++;
		return null;
	}

	@Override
	public Object visitExpressionBinary(ExpressionBinary expressionBinary,
			Object arg) throws Exception {
		Expression e0 = expressionBinary.leftExpression;
		Expression e1 = expressionBinary.rightExpression;
		e0.visit(this, arg);
		e1.visit(this, arg);
		Type e0Type = e0.type;
		Type e1Type = e1.type;
		Kind op = expressionBinary.op;
		
		if (e0Type == Type.FLOAT && e1Type == Type.INTEGER) {
			mv.visitInsn(Opcodes.I2F);
		}
		else if (e0Type == Type.INTEGER && e1Type == Type.FLOAT) {
			mv.visitInsn(Opcodes.SWAP);
			mv.visitInsn(Opcodes.I2F);
			mv.visitInsn(Opcodes.SWAP);
		}
		
		switch(op) {
		case OP_PLUS:
			if (e0Type == Type.INTEGER && e1Type == Type.INTEGER) 
				mv.visitInsn(Opcodes.IADD);
			else
				mv.visitInsn(Opcodes.FADD);
			break;
			
		case OP_MINUS:
			if (e0Type == Type.INTEGER && e1Type == Type.INTEGER) 
				mv.visitInsn(Opcodes.ISUB);
			else 
				mv.visitInsn(Opcodes.FSUB);
			break;
		
		case OP_TIMES:
			if (e0Type == Type.INTEGER && e1Type == Type.INTEGER) 
				mv.visitInsn(Opcodes.IMUL);
			else
				mv.visitInsn(Opcodes.FMUL);
			break;
		
		case OP_DIV:
			if (e0Type == Type.INTEGER && e1Type == Type.INTEGER) 
				mv.visitInsn(Opcodes.IDIV);
			else
				mv.visitInsn(Opcodes.FDIV);
			break;
			
		case OP_MOD: 
			mv.visitInsn(Opcodes.IREM);
			break;
			
		case OP_POWER:
			if (e0Type == Type.INTEGER && e1Type == Type.INTEGER) {
				
				mv.visitInsn(Opcodes.POP);
				convertTopValueToDouble(e0Type);
				e1.visit(this, arg);
				convertTopValueToDouble(e0Type);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "pow", "(DD)D", false);
				convertTopValueToPreviousType(e0Type);
			}
				
			else {
				mv.visitInsn(Opcodes.POP);
				convertTopValueToDouble(Type.FLOAT);
				e1.visit(this, arg);
				convertTopValueToDouble(e1Type);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "pow", "(DD)D", false);
				convertTopValueToPreviousType(Type.FLOAT);
			}
			break;
			
		case OP_AND:
			mv.visitInsn(Opcodes.IAND);
			break;
			
		case OP_OR: 
			mv.visitInsn(Opcodes.IOR);
			break;
			
		case OP_EQ:
			if (e0Type == Type.INTEGER && e1Type == Type.INTEGER) {
				generateComparisonByteCode(Opcodes.IF_ICMPNE, Opcodes.ICONST_1, Opcodes.ICONST_0);
			}
			else if (e0Type == Type.BOOLEAN && e1Type == Type.BOOLEAN) {
				generateComparisonByteCode(Opcodes.IF_ICMPNE, Opcodes.ICONST_1, Opcodes.ICONST_0);
			}
			else {
				mv.visitInsn(Opcodes.FCMPG);
				generateComparisonByteCode(Opcodes.IFEQ, Opcodes.ICONST_0, Opcodes.ICONST_1);
			}
			
			break;
		
		case OP_NEQ:
			if (e0Type == Type.INTEGER && e1Type == Type.INTEGER) {
				generateComparisonByteCode(Opcodes.IF_ICMPEQ, Opcodes.ICONST_1, Opcodes.ICONST_0);
			}
			else if (e0Type == Type.BOOLEAN && e1Type == Type.BOOLEAN) {
				generateComparisonByteCode(Opcodes.IF_ICMPEQ, Opcodes.ICONST_1, Opcodes.ICONST_0);
			}
			else {
				mv.visitInsn(Opcodes.FCMPL);
				generateComparisonByteCode(Opcodes.IFEQ, Opcodes.ICONST_1, Opcodes.ICONST_0);
			}
			
			break;
		
		case OP_GE:
			if (e0Type == Type.INTEGER && e1Type == Type.INTEGER) {
				generateComparisonByteCode(Opcodes.IF_ICMPLT, Opcodes.ICONST_1, Opcodes.ICONST_0);
			}
			else if (e0Type == Type.BOOLEAN && e1Type == Type.BOOLEAN) {
				generateComparisonByteCode(Opcodes.IF_ICMPLT, Opcodes.ICONST_1, Opcodes.ICONST_0);
			}
			else {
				mv.visitInsn(Opcodes.FCMPL);
				generateComparisonByteCode(Opcodes.IFLT, Opcodes.ICONST_1, Opcodes.ICONST_0);
			}
			
			break;
		
		case OP_GT:
			if (e0Type == Type.INTEGER && e1Type == Type.INTEGER) {
				generateComparisonByteCode(Opcodes.IF_ICMPLE, Opcodes.ICONST_1, Opcodes.ICONST_0);
			}
			else if (e0Type == Type.BOOLEAN && e1Type == Type.BOOLEAN) {
				generateComparisonByteCode(Opcodes.IF_ICMPLE, Opcodes.ICONST_1, Opcodes.ICONST_0);
			}
			else {
				mv.visitInsn(Opcodes.FCMPG);
				generateComparisonByteCode(Opcodes.IFGT, Opcodes.ICONST_0, Opcodes.ICONST_1);
			}
			
			break;
		
		case OP_LE:
			if (e0Type == Type.INTEGER && e1Type == Type.INTEGER) {
				generateComparisonByteCode(Opcodes.IF_ICMPGT, Opcodes.ICONST_1, Opcodes.ICONST_0);
			}
			else if (e0Type == Type.BOOLEAN && e1Type == Type.BOOLEAN) {
				generateComparisonByteCode(Opcodes.IF_ICMPGT, Opcodes.ICONST_1, Opcodes.ICONST_0);
			}
			else {
				mv.visitInsn(Opcodes.FCMPG);
				generateComparisonByteCode(Opcodes.IFGT, Opcodes.ICONST_1, Opcodes.ICONST_0);
			}
				
			break;
			
		case OP_LT:
			if (e0Type == Type.INTEGER && e1Type == Type.INTEGER) {
				generateComparisonByteCode(Opcodes.IF_ICMPGE, Opcodes.ICONST_1, Opcodes.ICONST_0);
			}
			else if (e0Type == Type.BOOLEAN && e1Type == Type.BOOLEAN) {
				generateComparisonByteCode(Opcodes.IF_ICMPGE, Opcodes.ICONST_1, Opcodes.ICONST_0);
			}
			else {
				mv.visitInsn(Opcodes.FCMPL);
				generateComparisonByteCode(Opcodes.IFLT, Opcodes.ICONST_0, Opcodes.ICONST_1);
			}
			
			break;	
		}
		return null;
		
	}
	
	public void generateComparisonByteCode(int opcode, int v1, int v2) {
		Label l1 = new Label();
		mv.visitJumpInsn(opcode, l1);
		mv.visitInsn(v1);
		Label l2 = new Label();
		mv.visitJumpInsn(Opcodes.GOTO, l2);
		mv.visitLabel(l1);
		mv.visitInsn(v2);
		mv.visitLabel(l2);	
	}

	@Override
	public Object visitExpressionConditional(
			ExpressionConditional expressionConditional, Object arg)
			throws Exception {
		Label l1 = new Label();
		Label l2 = new Label();
		
		expressionConditional.guard.visit(this, arg);
		mv.visitJumpInsn(Opcodes.IFEQ, l1);
		expressionConditional.trueExpression.visit(this, arg);
		mv.visitJumpInsn(Opcodes.GOTO, l2);
		mv.visitLabel(l1);
		expressionConditional.falseExpression.visit(this, arg);
		mv.visitLabel(l2);
		
		return null;
	}

	@Override
	public Object visitExpressionFloatLiteral(
			ExpressionFloatLiteral expressionFloatLiteral, Object arg)
			throws Exception {
		mv.visitLdcInsn(expressionFloatLiteral.value);
		return null;
	}

	@Override
	public Object visitExpressionFunctionAppWithExpressionArg(
			ExpressionFunctionAppWithExpressionArg expressionFunctionAppWithExpressionArg,
			Object arg) throws Exception {
		expressionFunctionAppWithExpressionArg.e.visit(this, arg);
		Type type = expressionFunctionAppWithExpressionArg.e.type;
		Kind functionName = expressionFunctionAppWithExpressionArg.function;
		HashMap<Kind, String> map = new HashMap<>();
		map.put(Kind.KW_sin, "sin");
		map.put(Kind.KW_cos, "cos");
		map.put(Kind.KW_atan, "atan");
		map.put(Kind.KW_log, "log");
		map.put(Kind.KW_abs, "abs");
		
		switch(functionName) {
		case KW_sin:
		case KW_cos:
		case KW_atan:
		case KW_log:
		case KW_abs:
			convertTopValueToDouble(type);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", map.get(functionName), "(D)D", false);
			convertTopValueToPreviousType(type);
			break;
			
		case KW_red:
			if (type == Type.INTEGER)
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimePixelOps.className, "getRed", 
						RuntimePixelOps.getRedSig, false);
			break;
			
		case KW_blue:
			if (type == Type.INTEGER)
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimePixelOps.className, "getBlue", 
						RuntimePixelOps.getBlueSig, false);
			break;
			
		case KW_green:
			if (type == Type.INTEGER)
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimePixelOps.className, "getGreen", 
						RuntimePixelOps.getGreenSig, false);
			break;
			
		case KW_alpha:
			if (type == Type.INTEGER)
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimePixelOps.className, "getAlpha", 
						RuntimePixelOps.getAlphaSig, false);
			break;
			
		case KW_width:
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "getWidth",
					RuntimeImageSupport.getWidthSig, false);
			break;
			
		case KW_height:
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "getHeight",
					RuntimeImageSupport.getHeightSig, false);
			break;
		
		case KW_int:
			if (type == Type.FLOAT)
				mv.visitInsn(Opcodes.F2I);
			break;
			
		case KW_float:
			if (type == Type.INTEGER)
				mv.visitInsn(Opcodes.I2F);
			break;
		}
		
		return null;
	}
	

	@Override
	public Object visitExpressionFunctionAppWithPixel(
			ExpressionFunctionAppWithPixel expressionFunctionAppWithPixel,
			Object arg) throws Exception {
		Kind name = expressionFunctionAppWithPixel.name;
		Type type = expressionFunctionAppWithPixel.e1.type;
		switch(name) {
			case KW_cart_x:
				expressionFunctionAppWithPixel.e1.visit(this, arg);
				convertTopValueToDouble(type);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "cos", "(D)D", false);
				expressionFunctionAppWithPixel.e0.visit(this, arg);
				convertTopValueToDouble(type);
				mv.visitInsn(Opcodes.DMUL);
				mv.visitInsn(Opcodes.D2I);
				break;
			case KW_cart_y:
				expressionFunctionAppWithPixel.e1.visit(this, arg);
				convertTopValueToDouble(type);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "sin", "(D)D", false);
				expressionFunctionAppWithPixel.e0.visit(this, arg);
				convertTopValueToDouble(type);
				mv.visitInsn(Opcodes.DMUL);
				mv.visitInsn(Opcodes.D2I);
				break;
			case KW_polar_a:
				expressionFunctionAppWithPixel.e1.visit(this, arg);
				convertTopValueToDouble(type);
				expressionFunctionAppWithPixel.e0.visit(this, arg);
				convertTopValueToDouble(type);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "atan2", "(DD)D", false); 
				mv.visitInsn(Opcodes.D2F);
				break;
			case KW_polar_r:
				expressionFunctionAppWithPixel.e1.visit(this, arg);
				convertTopValueToDouble(type);
				expressionFunctionAppWithPixel.e0.visit(this, arg);
				convertTopValueToDouble(type);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "hypot", "(DD)D", false); 
				mv.visitInsn(Opcodes.D2F);	
		}
		
		return  null;
				
	}

	@Override
	public Object visitExpressionIdent(ExpressionIdent expressionIdent,
			Object arg) throws Exception {
		Type type = Types.getType(expressionIdent.dec.type);
		
		switch(type) {
		case INTEGER:
		case BOOLEAN:
			mv.visitVarInsn(ILOAD, expressionIdent.dec.getSlot());
			break;
		case FLOAT:
			mv.visitVarInsn(FLOAD, expressionIdent.dec.getSlot());
			break;
		default:
			mv.visitVarInsn(ALOAD, expressionIdent.dec.getSlot());
		}
		
		return null;
	}

	@Override
	public Object visitExpressionIntegerLiteral(
			ExpressionIntegerLiteral expressionIntegerLiteral, Object arg)
			throws Exception {
		mv.visitLdcInsn(expressionIntegerLiteral.value);
		return null;
	}

	@Override
	public Object visitExpressionPixel(ExpressionPixel expressionPixel,
			Object arg) throws Exception {
		mv.visitVarInsn(Opcodes.ALOAD, expressionPixel.dec.getSlot());
		expressionPixel.pixelSelector.visit(this, arg);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "getPixel",
				RuntimeImageSupport.getPixelSig, false);
		return null;
	}

	@Override
	public Object visitExpressionPixelConstructor(
			ExpressionPixelConstructor expressionPixelConstructor, Object arg)
			throws Exception {
		expressionPixelConstructor.alpha.visit(this, arg);
		expressionPixelConstructor.red.visit(this, arg);
		expressionPixelConstructor.green.visit(this, arg);
		expressionPixelConstructor.blue.visit(this, arg);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimePixelOps.className, "makePixel",
				RuntimePixelOps.makePixelSig, false);
		return null;
		
	}

	@Override
	public Object visitExpressionPredefinedName(
			ExpressionPredefinedName expressionPredefinedName, Object arg)
			throws Exception {
		if (expressionPredefinedName.name == Kind.KW_Z) 
			mv.visitLdcInsn(Z);
		else if (expressionPredefinedName.name == Kind.KW_default_height)
			mv.visitLdcInsn(this.defaultHeight);
		else if (expressionPredefinedName.name == Kind.KW_default_width)
			mv.visitLdcInsn(this.defaultWidth);
		
		return null;
	}

	@Override
	public Object visitExpressionUnary(ExpressionUnary expressionUnary,
			Object arg) throws Exception {
		
		expressionUnary.expression.visit(this, arg);
		Kind op = expressionUnary.op;
		Type expType = expressionUnary.expression.type;
		
		switch (op) {
			case OP_PLUS:
				break;
			case OP_MINUS:
				if (expType == Type.INTEGER)
					mv.visitInsn(Opcodes.INEG);
				else if (expType == Type.FLOAT)
					mv.visitInsn(Opcodes.FNEG);
				break;
			case OP_EXCLAMATION:
				if (expType == Type.BOOLEAN)
					mv.visitInsn(Opcodes.ICONST_1);
				else if (expType == Type.INTEGER)
					mv.visitInsn(Opcodes.ICONST_M1);
				mv.visitInsn(Opcodes.IXOR);
				break;
			default:
				throw new UnsupportedOperationException();
		}
		
		return null;
	}

	@Override
	public Object visitLHSIdent(LHSIdent lhsIdent, Object arg)
			throws Exception {
		Type type = lhsIdent.type;
		
		switch (type) {
		case IMAGE:
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "deepCopy",
					RuntimeImageSupport.deepCopySig, false);
			mv.visitVarInsn(Opcodes.ASTORE, lhsIdent.dec.getSlot());
			break;
		case BOOLEAN:
		case INTEGER:
			mv.visitVarInsn(Opcodes.ISTORE, lhsIdent.dec.getSlot());
			break;
		case FLOAT:
			mv.visitVarInsn(Opcodes.FSTORE, lhsIdent.dec.getSlot());
			break;
		case FILE:
			mv.visitVarInsn(Opcodes.ASTORE, lhsIdent.dec.getSlot());
			break;
		}
			
		return null;
	}

	@Override
	public Object visitLHSPixel(LHSPixel lhsPixel, Object arg)
			throws Exception {
		
		mv.visitVarInsn(Opcodes.ALOAD, lhsPixel.dec.getSlot());
		lhsPixel.pixelSelector.visit(this, arg);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "setPixel",
				RuntimeImageSupport.setPixelSig, false);
		
		return null;
	}

	@Override
	public Object visitLHSSample(LHSSample lhsSample, Object arg)
			throws Exception {
		mv.visitVarInsn(Opcodes.ALOAD, lhsSample.dec.getSlot());
		lhsSample.pixelSelector.visit(this, arg);
		Kind colour = lhsSample.color;
		
		switch(colour) {
		case KW_alpha:
			mv.visitInsn(ICONST_0);
			break;
		case KW_red:
			mv.visitInsn(ICONST_1);
			break;
		case KW_green:
			mv.visitInsn(ICONST_2);
			break;
		case KW_blue:
			mv.visitInsn(ICONST_3);
		}
		
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "updatePixelColor",
				RuntimeImageSupport.updatePixelColorSig, false);
		return null;
	}

	@Override
	public Object visitPixelSelector(PixelSelector pixelSelector, Object arg)
			throws Exception {
		Type type = pixelSelector.ex.type;
		
		pixelSelector.ex.visit(this, arg);
		pixelSelector.ey.visit(this, arg);
		
		if (type == Type.FLOAT) {
			mv.visitInsn(Opcodes.POP);
			convertTopValueToDouble(type);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "cos", "(D)D", false);
			pixelSelector.ex.visit(this, arg);
			convertTopValueToDouble(type);
			mv.visitInsn(DMUL);
			mv.visitInsn(D2I);
			
			pixelSelector.ey.visit(this, arg);
			convertTopValueToDouble(type);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "sin", "(D)D", false);
			pixelSelector.ex.visit(this, arg);
			convertTopValueToDouble(type);
			mv.visitInsn(DMUL);
			mv.visitInsn(D2I);
		}
		return  null;
	}

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		// TODO refactor and extend as necessary
		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		// cw = new ClassWriter(0); //If the call to mv.visitMaxs(1, 1) crashes,
		// it is sometimes helpful to
		// temporarily run it without COMPUTE_FRAMES. You probably
		// won't get a completely correct class file, but
		// you will be able to see the code that was generated.
		className = program.progName;
		classDesc = "L" + className + ";";
		String sourceFileName = (String) arg;
		cw.visit(52, ACC_PUBLIC + ACC_SUPER, className, null,
				"java/lang/Object", null);
		cw.visitSource(sourceFileName, null);

		// create main method
		mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main",
				"([Ljava/lang/String;)V", null, null);
		// initialize
		mv.visitCode();

		// add label before first instruction
		Label mainStart = new Label();
		mv.visitLabel(mainStart);

		CodeGenUtils.genLog(DEVEL, mv, "entering main");

		program.block.visit(this, arg);

		// generates code to add string to log
		CodeGenUtils.genLog(DEVEL, mv, "leaving main");

		// adds the required (by the JVM) return statement to main
		mv.visitInsn(RETURN);

		// adds label at end of code
		Label mainEnd = new Label();
		mv.visitLabel(mainEnd);
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, mainStart,
				mainEnd, 0);
		// Because we use ClassWriter.COMPUTE_FRAMES as a parameter in the
		// constructor,
		// asm will calculate this itself and the parameters are ignored.
		// If you have trouble with failures in this routine, it may be useful
		// to temporarily change the parameter in the ClassWriter constructor
		// from COMPUTE_FRAMES to 0.
		// The generated class file will not be correct, but you will at least be
		// able to see what is in it.
		mv.visitMaxs(0, 0);

		// terminate construction of main method
		mv.visitEnd();

		// terminate class construction
		cw.visitEnd();

		// generate classfile as byte array and return
		return cw.toByteArray();
	}

	@Override
	public Object visitStatementAssign(StatementAssign statementAssign,
			Object arg) throws Exception {
		statementAssign.e.visit(this, arg);
		statementAssign.lhs.visit(this, arg);
		return null;
	}

	@Override
	public Object visitStatementIf(StatementIf statementIf, Object arg)
			throws Exception {
		Label l1 = new Label();
		
		statementIf.guard.visit(this, arg);
		mv.visitJumpInsn(Opcodes.IFEQ, l1);
		statementIf.b.visit(this, arg);
		mv.visitLabel(l1);
		
		return null;
	}

	@Override
	public Object visitStatementInput(StatementInput statementInput, Object arg)
			throws Exception {
		mv.visitVarInsn(ALOAD, 0);
		statementInput.e.visit(this, arg);
		mv.visitInsn(AALOAD);
		statementInput.dec.visit(this,  arg);
		
		Type decType = Types.getType(statementInput.dec.type); 
		
		switch(decType) {
		case INTEGER:
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", false);
			mv.visitVarInsn(Opcodes.ISTORE, statementInput.dec.getSlot());
			break;
		case BOOLEAN:
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "parseBoolean", "(Ljava/lang/String;)Z", false);
			mv.visitVarInsn(Opcodes.ISTORE, statementInput.dec.getSlot());
			break;
		case FLOAT:
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "parseFloat", "(Ljava/lang/String;)F", false);
			mv.visitVarInsn(Opcodes.FSTORE, statementInput.dec.getSlot());
			break;
		case IMAGE:
			if (statementInput.dec.width == null && statementInput.dec.height == null) {
				mv.visitInsn(Opcodes.ACONST_NULL);
				mv.visitInsn(Opcodes.ACONST_NULL);
			}
			else {
				mv.visitTypeInsn(Opcodes.NEW, "java/lang/Integer");
				mv.visitInsn(Opcodes.DUP);
				statementInput.dec.width.visit(this, arg);
				mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V", false);
				
				mv.visitTypeInsn(Opcodes.NEW, "java/lang/Integer");
				mv.visitInsn(Opcodes.DUP);
				statementInput.dec.height.visit(this, arg);
				mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V", false);
			}
			
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "readImage",
					RuntimeImageSupport.readImageSig, false);
			mv.visitVarInsn(Opcodes.ASTORE, statementInput.dec.getSlot());
			break;
		default:
			mv.visitVarInsn(Opcodes.ASTORE, statementInput.dec.getSlot());
		 
		}
		
		return null;
	}

	@Override
	public Object visitStatementShow(StatementShow statementShow, Object arg)
			throws Exception {
		/**
		 * 
		 * For integers, booleans, and floats, generate code to print to
		 * console. For images, generate code to display in a frame.
		 * 
		 * In all cases, invoke CodeGenUtils.genLogTOS(GRADE, mv, type); before
		 * consuming top of stack.
		 */
		statementShow.e.visit(this, arg);
		Type type = statementShow.e.getType();
		switch (type) {
			case INTEGER : {
				CodeGenUtils.genLogTOS(GRADE, mv, type);
				mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out",
						"Ljava/io/PrintStream;");
				mv.visitInsn(Opcodes.SWAP);
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream",
						"println", "(I)V", false);
			}
				break;
				
			case BOOLEAN : {
				CodeGenUtils.genLogTOS(GRADE, mv, type);
				mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", 
						"Ljava/io/PrintStream;");
				mv.visitInsn(Opcodes.SWAP);
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream",
						"println", "(Z)V", false);
			}
				break;
			
			case FLOAT : {
				CodeGenUtils.genLogTOS(GRADE, mv, type);
				mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out",
						"Ljava/io/PrintStream;");
				mv.visitInsn(Opcodes.SWAP);
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream",
						"println", "(F)V", false);
			}
				break;
			
			case FILE: {
				mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out",
						"Ljava/io/PrintStream;");
				mv.visitInsn(Opcodes.SWAP);
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream",
						"println", "(Ljava/lang/String)V", false);
			}
				break;
				
			case IMAGE : {
				CodeGenUtils.genLogTOS(GRADE, mv, type);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, 
						"makeFrame", RuntimeImageSupport.makeFrameSig, false);
				mv.visitInsn(Opcodes.POP);
			}
				break;
		}
		return null;
	}

	@Override
	public Object visitStatementSleep(StatementSleep statementSleep, Object arg)
			throws Exception {
		statementSleep.duration.visit(this, arg);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "toUnsignedLong", "(I)J", false);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "sleep", "(J)V", false);
		return null;
	}

	@Override
	public Object visitStatementWhile(StatementWhile statementWhile, Object arg)
			throws Exception {
		Label l1 = new Label();
		Label l2 = new Label();
		mv.visitJumpInsn(Opcodes.GOTO, l1);
		mv.visitLabel(l2);
		statementWhile.b.visit(this, arg);
		mv.visitLabel(l1);
		statementWhile.guard.visit(this, arg);
		mv.visitJumpInsn(Opcodes.IFNE, l2);
		
		return null;
	}

	@Override
	public Object visitStatementWrite(StatementWrite statementWrite, Object arg)
			throws Exception {
		mv.visitVarInsn(Opcodes.ALOAD, statementWrite.sourceDec.getSlot());
		mv.visitVarInsn(Opcodes.ALOAD, statementWrite.destDec.getSlot());
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, RuntimeImageSupport.className, "write", RuntimeImageSupport.writeSig,
				false);
		return null;
	}

}
