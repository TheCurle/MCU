package io.github.xfacthd.rsctrlunit.common.emulator.assembler;

import io.github.xfacthd.rsctrlunit.common.emulator.util.Code;
import io.github.xfacthd.rsctrlunit.common.emulator.assembler.node.*;
import java.util.*;

public abstract class Assembler {
    protected Assembler() { }

    public abstract Code assemble(String name, String source, ErrorPrinter errorPrinter);

    protected abstract List<Node> parseSource(String source, ErrorPrinter errorPrinter);

    protected abstract String[] extractOperands(String[] parts);

    protected abstract boolean validateLabels(List<Node> nodes, ErrorPrinter errorPrinter);

    protected abstract int computeCodeSize(List<Node> nodes, ErrorPrinter errorPrinter);

    protected abstract Code buildRomImage(String name, List<Node> nodes, byte[] codeBytes, ErrorPrinter errorPrinter);

}
