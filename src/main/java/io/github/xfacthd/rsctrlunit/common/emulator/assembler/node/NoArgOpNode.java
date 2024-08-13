package io.github.xfacthd.rsctrlunit.common.emulator.assembler.node;

import io.github.xfacthd.rsctrlunit.common.emulator.core.i8051.I8051Opcode;

public record NoArgOpNode(int line, I8051Opcode opcode) implements OpNode
{
    @Override
    public int appendOperands(byte[] code, int pointer)
    {
        return pointer;
    }

    public static NoArgOpNode create(int line, I8051Opcode opcode, @SuppressWarnings("unused") String[] operands)
    {
        return new NoArgOpNode(line, opcode);
    }
}
