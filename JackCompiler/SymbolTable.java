import java.util.HashMap;

enum SymbolType {
    STATIC,
    FIELD,
    ARG,
    VAR,
    NONE
}

public class SymbolTable {
    public HashMap<String, String[]> classTable;
    public HashMap<String, String[]> subroutineTable;
    int staticCount, fieldCount, argCount, varCount;

    // Creates new Symbol tables.
    public SymbolTable() {
        classTable = new HashMap<String, String[]>();
        subroutineTable = new HashMap<String, String[]>();
        staticCount = 0;
        fieldCount = 0;
        argCount = 0;
        varCount = 0;
    }

    // Starts a new subroutine scope by resetting the subroutine table.
    public void startSubroutine() {
        subroutineTable = new HashMap<String, String[]>();
        argCount = 0;
        varCount = 0;
    }

    // Defines new identifier of given name, type, and kind and assigns it a running index.
	//      STATIC and FIELD are class scope.
	// 	    ARG and VAR are subroutine scope.
    public void define (String name, String type, SymbolType kind) {
        boolean isClassScope = (kind == SymbolType.STATIC || kind == SymbolType.FIELD);
        if (isClassScope) { // Class scope means looking at the class table.
            int runningIndex;
            if (kind == SymbolType.STATIC) {
                runningIndex = staticCount++;
            } else {
                runningIndex = fieldCount++;
            }

            classTable.put(name, new String[]{type, kind.name(), Integer.toString(runningIndex)});
        } else { // Subroutine scope means looking at the subroutine table.
            int runningIndex;
            if (kind == SymbolType.ARG) {
                runningIndex = argCount++;
            } else {
                runningIndex = varCount++;
            }

            subroutineTable.put(name, new String[]{type, kind.name(), Integer.toString(runningIndex)});            
        }
    }

	// Returns number of variables of given kind already defined in current scope.
    public int varCount(SymbolType kind) {
        if (kind == SymbolType.STATIC) {
            return staticCount;
        } else if (kind == SymbolType.FIELD) {
            return fieldCount;
        } else if (kind == SymbolType.ARG) {
            return  argCount;
        } else {
            return varCount;
        }
    }

	// Returns the kind of the named identifier in the current scope.
    //      Returns NONE if unknown in the current scope.
    public String kindOf(String name) {
        String[] properties = subroutineTable.get(name);
        if (properties == null) {
            properties = classTable.get(name);
        }

        if (properties == null) {
            return null;
        } else {
            return properties[0];
        }
    }

	// Returns the type of the named identifier in the current scope.
    public SymbolType typeOf(String name) {
        String[] properties = subroutineTable.get(name);
        if (properties == null) {
            properties = classTable.get(name);
        }

        if (properties == null) {
            return null;
        } else {
            return SymbolType.valueOf(properties[1]);
        }
    }

	// Returns the index of the named identifier in the current scope.
    public int indexOf(String name) {
        String[] properties = subroutineTable.get(name);
        if (properties == null) {
            properties = classTable.get(name);
        }

        if (properties == null) {
            return -1;
        } else {
            return Integer.parseInt(properties[2]);
        }
    }
}
