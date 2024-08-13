package io.github.xfacthd.rsctrlunit.common.emulator.assembler.node;

import io.github.xfacthd.rsctrlunit.common.emulator.core.i8051.I8051Opcode;

public interface OpNode extends Node
{
    I8051Opcode opcode();

    int appendOperands(byte[] code, int pointer);
}
