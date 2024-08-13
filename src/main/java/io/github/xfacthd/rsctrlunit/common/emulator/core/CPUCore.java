package io.github.xfacthd.rsctrlunit.common.emulator.core;

import io.github.xfacthd.rsctrlunit.common.emulator.assembler.Assembler;
import io.github.xfacthd.rsctrlunit.common.emulator.core.i8051.I8051Assembler;
import io.github.xfacthd.rsctrlunit.common.emulator.core.i8051.I8051Disassembler;
import io.github.xfacthd.rsctrlunit.common.emulator.core.i8051.I8051Opcode;
import io.github.xfacthd.rsctrlunit.common.emulator.disassembler.Disassembler;
import io.github.xfacthd.rsctrlunit.common.emulator.opcode.Opcode;

import java.util.function.Function;


/**
 * Data required to emulate a given CPU core.
 */
public enum CPUCore {
    CPU8051("8051", "Intel", new I8051Assembler(), new I8051Disassembler(), new int[] {0, 0}, b -> I8051Opcode.OPCODES[b & 0xFF]),
    CPU8080("8080", "Intel", null, null, new int[] {0, 0}, b -> null),
    CPU8085("8085", "Intel", null, null, new int[] {0, 0}, b -> null),
    CPUZ80("Z80", "Zilog", null, null, new int[] {0, 0}, b -> null);

    public String name;
    public String manufacturer;

    public int[] registers;

    public Assembler assembler;
    public Disassembler disassembler;

    public Function<Byte, Opcode> opcodeFunc;

    CPUCore(String name, String manufacturer, Assembler assembler, Disassembler disassembler, int[] registers, Function<Byte, Opcode> opcodeFunc) {
        this.name = name;
        this.manufacturer = manufacturer;
        this.assembler = assembler;
        this.registers = registers;
        this.disassembler = disassembler;
        this.opcodeFunc = opcodeFunc;
    }

}
