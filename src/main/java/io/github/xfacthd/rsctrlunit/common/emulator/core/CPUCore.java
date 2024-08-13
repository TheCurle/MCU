package io.github.xfacthd.rsctrlunit.common.emulator.core;

import io.github.xfacthd.rsctrlunit.common.emulator.assembler.Assembler;
import io.github.xfacthd.rsctrlunit.common.emulator.core.i8051.I8051Assembler;
import io.github.xfacthd.rsctrlunit.common.emulator.core.i8051.I8051Disassembler;
import io.github.xfacthd.rsctrlunit.common.emulator.core.i8051.I8051Opcode;
import io.github.xfacthd.rsctrlunit.common.emulator.disassembler.Disassembler;
import io.github.xfacthd.rsctrlunit.common.emulator.opcode.Opcode;


/**
 * Data required to emulate a given CPU core.
 */
public enum CPUCore {
    CPU8051("8051", "Intel", new I8051Assembler(), new I8051Disassembler(), new int[] {0, 0}, I8051Opcode.values()),
    CPU8080("8080", "Intel", null, null, new int[] {0, 0}),
    CPU8085("8085", "Intel", null, null, new int[] {0, 0}),
    CPUZ80("Z80", "Zilog", null, null, new int[] {0, 0});

    public String name;
    public String manufacturer;

    public int[] registers;

    public Assembler assembler;
    public Disassembler disassembler;

    public Opcode[] opcodes;

    CPUCore(String name, String manufacturer, Assembler assembler, Disassembler disassembler, int[] registers, Opcode... opcodes) {
        this.name = name;
        this.manufacturer = manufacturer;
        this.assembler = assembler;
        this.registers = registers;
        this.disassembler = disassembler;
        this.opcodes = opcodes;
    }

}
