package cop5556sp18;

import cop5556sp18.AST.Declaration;
import java.util.HashMap;
import java.util.Stack;
import java.util.LinkedHashMap;
import java.util.ArrayList;

public class SymbolTable {
	
	private HashMap<String, LinkedHashMap<Integer, Declaration>> table = new HashMap<>();
	private Stack<Integer> scope = new Stack<>();
	private int scopeID;
	
	public SymbolTable() {
		scopeID = 0;
		scope.push(0);
	}
	
	public void enterScope() {
		scope.push(++scopeID);
	}
	
	public void leaveScope() {
		scope.pop();
	}
	
	public int getScopeId() {
		return scope.peek();
	}
	
	public boolean insert(String identifier, Declaration dec) {
		int presentScope = getScopeId();
		LinkedHashMap<Integer, Declaration> chain = table.get(identifier);
		
		if (chain == null)
			chain = new LinkedHashMap<>();
		
		if (chain.get(presentScope) == null) {
			chain.put(presentScope, dec);
			table.put(identifier, chain);
			return true;
		}
		return false;
	}
	
	public Declaration lookup(String identifier) {
		ArrayList<Integer> scopeArray = new ArrayList<>(scope);
		LinkedHashMap<Integer, Declaration> chain = table.get(identifier);
		
		if (chain == null)
			return null;
		
		for (int i = scopeArray.size() - 1; i >= 0; i--) {
			if (chain.get(scopeArray.get(i)) != null)
				return chain.get(scopeArray.get(i));
		}
		return null;
	}
	
	public boolean inCurrentScope(String identifier) {
		int presentScope = getScopeId();
		LinkedHashMap<Integer, Declaration> chain = table.get(identifier);
		if (chain == null || chain.get(presentScope) == null)
			return false;
		return true;
	}
}