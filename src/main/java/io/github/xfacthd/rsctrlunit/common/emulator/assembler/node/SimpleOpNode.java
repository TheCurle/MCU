package io.github.xfacthd.rsctrlunit.common.emulator.assembler.node;

import io.github.xfacthd.rsctrlunit.common.emulator.core.i8051.I8051Opcode;

public record SimpleOpNode(int line, I8051Opcode opcode, byte... operands) implements OpNode
{
    @Override
    public int appendOperands(byte[] code, int pointer)
    {
        for (byte operand : operands)
        {
            code[pointer] = operand;
            pointer++;
        }
        return pointer;
    }
}
