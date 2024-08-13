package io.github.xfacthd.rsctrlunit.common.emulator.util;

import io.github.xfacthd.rsctrlunit.common.emulator.assembler.node.Node;
import io.github.xfacthd.rsctrlunit.common.emulator.core.i8051.I8051Opcode;
import org.jetbrains.annotations.Nullable;

public interface NodeParser
{
    @Nullable
    Node parse(int line, I8051Opcode op, String[] operands);
}
