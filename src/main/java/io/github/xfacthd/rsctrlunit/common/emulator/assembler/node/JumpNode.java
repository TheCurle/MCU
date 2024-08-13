package io.github.xfacthd.rsctrlunit.common.emulator.assembler.node;

import io.github.xfacthd.rsctrlunit.common.emulator.core.i8051.I8051Opcode;

public record JumpNode(int line, I8051Opcode opcode, String label, byte... operands) implements OpNode
{
    @Override
    public int appendOperands(byte[] code, int pointer)
    {
        for (byte operand : operands)
        {
            code[pointer] = operand;
            pointer++;
        }
        if (opcode == I8051Opcode.LJMP || opcode == I8051Opcode.LCALL)
        {
            return pointer + 2;
        }
        return pointer + 1;
    }
}
