package io.github.xfacthd.rsctrlunit.common.emulator.opcode;

import io.github.xfacthd.rsctrlunit.common.emulator.interpreter.Interpreter;

import java.util.function.Consumer;

public interface Opcode {

    String getMnemonic();

    int getOperands();

    int getOperandBytes();

    byte toByte();

    Opcode fromRomByte(byte romByte);

    Consumer<Interpreter.InterpreterContext> getOpcodeFunc();
}
