package io.github.xfacthd.rsctrlunit.common.emulator.assembler;

import io.github.xfacthd.rsctrlunit.common.emulator.util.Code;
import io.github.xfacthd.rsctrlunit.common.emulator.assembler.node.*;
import java.util.*;

/**
 * Handles converting assembler notation to machine code bytes.
 * Has some built-in verification features.
 *
 * TODO: Expand as we move to more complicated assembler syntax
 */
public abstract class Assembler {
    protected Assembler() { }

    /**
     * Take a given source code string, assemble it to machine code.
     * @param name The name of the program to assemble
     * @param source The assembler source code to assemble
     * @param errorPrinter An output stream to pass errors in the compilation process, should they happen
     * @return
     */
    public abstract Code assemble(String name, String source, ErrorPrinter errorPrinter);

    protected abstract List<Node> parseSource(String source, ErrorPrinter errorPrinter);

    protected abstract String[] extractOperands(String[] parts);

    protected abstract boolean validateLabels(List<Node> nodes, ErrorPrinter errorPrinter);

    protected abstract int computeCodeSize(List<Node> nodes, ErrorPrinter errorPrinter);

    protected abstract Code buildRomImage(String name, List<Node> nodes, byte[] codeBytes, ErrorPrinter errorPrinter);

}
