package io.github.xfacthd.rsctrlunit.common.emulator.core.i8051;

import io.github.xfacthd.rsctrlunit.common.emulator.assembler.node.*;
import io.github.xfacthd.rsctrlunit.common.emulator.interpreter.Interpreter;
import io.github.xfacthd.rsctrlunit.common.emulator.opcode.Opcode;
import io.github.xfacthd.rsctrlunit.common.emulator.opcode.OpcodeHelpers;
import io.github.xfacthd.rsctrlunit.common.emulator.opcode.ParseHelpers;
import io.github.xfacthd.rsctrlunit.common.emulator.util.BitWriteMode;
import io.github.xfacthd.rsctrlunit.common.emulator.util.Constants;
import io.github.xfacthd.rsctrlunit.common.emulator.util.NodeParser;
import net.minecraft.Util;

import java.util.*;
import java.util.function.Consumer;

public enum I8051Opcode implements Opcode
{
    // Irregular 0x00-0x03
    NOP             ("NOP",     0, 0, NoArgOpNode::create, ctx -> {}),
    AJMP_000        ("AJMP",    1, 1, ParseHelpers.makeOneLabelArgJumpParser(), ctx -> {
        int address = OpcodeHelpers.calculateAjmpAddress(ctx.romByte, ctx.readRomAndIncrementPC());
        ctx.setProgramCounter(OpcodeHelpers.calculateAjmpTarget(ctx.programCounter, address));
    }),
    LJMP            ("LJMP",    1, 2, ParseHelpers.makeOneLabelArgJumpParser(), ctx -> {
        int upper = ctx.readRomAndIncrementPC() & 0xFF;
        int lower = ctx.readRomAndIncrementPC() & 0xFF;
        ctx.setProgramCounter((upper << 8) | lower);
    }),
    RR              ("RR",      1, 0, ParseHelpers.makeOneConstArgParser("A"), ctx ->
        OpcodeHelpers.readModifyWriteAccumulator(ctx.ram, (modRam, value) ->
        {
            int newMsb = (value << 7) & 0b10000000;
            return newMsb | (value >> 1);
        })
    ),
    //Regular 0x04-0x0F
    INC_ACC         ("INC",     1, 0, ParseHelpers.makeOneConstArgParser("A"), ctx -> OpcodeHelpers.readModifyWriteAccumulator(ctx.ram, (modRam, value) -> value + 1)),
    INC_MEM         ("INC",     1, 1, ParseHelpers.makeOneAddressArgParser(), ctx -> OpcodeHelpers.readModifyWriteMemory(ctx.ram, ctx.readRomAndIncrementPC(), (modRam, value) -> value + 1)),
    INC_IR0         ("INC",     1, 0, ParseHelpers.makeOneConstArgParser("@R0"), ctx -> OpcodeHelpers.readModifyWriteRegisterIndirect(ctx.ram, ctx.romByte & 0x1, (modRam, value) -> value + 1)),
    INC_IR1         ("INC",     1, 0, ParseHelpers.makeOneConstArgParser("@R1"), ctx -> OpcodeHelpers.readModifyWriteRegisterIndirect(ctx.ram, ctx.romByte & 0x1, (modRam, value) -> value + 1)),
    INC_DR0         ("INC",     1, 0, ParseHelpers.makeOneConstArgParser("R0"), ctx -> OpcodeHelpers.readModifyWriteRegister(ctx.ram, ctx.romByte & 0b00000111, (modRam, value) -> value + 1)),
    INC_DR1         ("INC",     1, 0, ParseHelpers.makeOneConstArgParser("R1"), ctx -> OpcodeHelpers.readModifyWriteRegister(ctx.ram, ctx.romByte & 0b00000111, (modRam, value) -> value + 1)),
    INC_DR2         ("INC",     1, 0, ParseHelpers.makeOneConstArgParser("R2"), ctx -> OpcodeHelpers.readModifyWriteRegister(ctx.ram, ctx.romByte & 0b00000111, (modRam, value) -> value + 1)),
    INC_DR3         ("INC",     1, 0, ParseHelpers.makeOneConstArgParser("R3"), ctx -> OpcodeHelpers.readModifyWriteRegister(ctx.ram, ctx.romByte & 0b00000111, (modRam, value) -> value + 1)),
    INC_DR4         ("INC",     1, 0, ParseHelpers.makeOneConstArgParser("R4"), ctx -> OpcodeHelpers.readModifyWriteRegister(ctx.ram, ctx.romByte & 0b00000111, (modRam, value) -> value + 1)),
    INC_DR5         ("INC",     1, 0, ParseHelpers.makeOneConstArgParser("R5"), ctx -> OpcodeHelpers.readModifyWriteRegister(ctx.ram, ctx.romByte & 0b00000111, (modRam, value) -> value + 1)),
    INC_DR6         ("INC",     1, 0, ParseHelpers.makeOneConstArgParser("R6"), ctx -> OpcodeHelpers.readModifyWriteRegister(ctx.ram, ctx.romByte & 0b00000111, (modRam, value) -> value + 1)),
    INC_DR7         ("INC",     1, 0, ParseHelpers.makeOneConstArgParser("R7"), ctx -> OpcodeHelpers.readModifyWriteRegister(ctx.ram, ctx.romByte & 0b00000111, (modRam, value) -> value + 1)),
    //Irregular 0x10-0x13
    JBC             ("JBC",     2, 2, ParseHelpers.makeOneBitArgJumpParser(), ctx ->
    {
        byte bitAddress = ctx.readRomAndIncrementPC();
        int offset = ctx.readRomAndIncrementPC();
        if (ctx.ram.readBit(bitAddress, true))
        {
            ctx.ram.writeBit(bitAddress, BitWriteMode.CLEAR);
            ctx.setProgramCounter(ctx.programCounter + offset);
        }
    }),
    ACALL_000       ("ACALL",   1, 1, ParseHelpers.makeOneLabelArgJumpParser(), ctx -> {
        int address = OpcodeHelpers.calculateAjmpAddress(ctx.romByte, ctx.readRomAndIncrementPC());
        ctx.pushStateBeforeCall();
        ctx.setProgramCounter(OpcodeHelpers.calculateAjmpTarget(ctx.programCounter, address));
    }),
    LCALL           ("LCALL",   1, 2, ParseHelpers.makeOneLabelArgJumpParser(), ctx -> {
        int upper = ctx.readRomAndIncrementPC() & 0xFF;
        int lower = ctx.readRomAndIncrementPC() & 0xFF;
        ctx.pushStateBeforeCall();
        ctx.setProgramCounter((upper << 8) | lower);
    }),
    RRC             ("RRC",     1, 0, ParseHelpers.makeOneConstArgParser("A"), ctx ->
        OpcodeHelpers.readModifyWriteAccumulator(ctx.ram, (modRam, value) ->
        {
            int newMsb = modRam.readBit(Constants.BIT_ADDRESS_CARRY) ? 0b10000000 : 0;
            boolean newCarry = (value & 0x1) != 0;
            modRam.writeBit(Constants.BIT_ADDRESS_CARRY, BitWriteMode.of(newCarry));
            return newMsb | (value >> 1);
        })
    ),
    //Regular 0x14-0x1F
    DEC_ACC         ("DEC",     1, 0, ParseHelpers.makeOneConstArgParser("A"), ctx -> OpcodeHelpers.readModifyWriteAccumulator(ctx.ram, (modRam, value) -> value - 1)),
    DEC_MEM         ("DEC",     1, 1, ParseHelpers.makeOneAddressArgParser(), ctx -> OpcodeHelpers.readModifyWriteMemory(ctx.ram, ctx.readRomAndIncrementPC(), (modRam, value) -> value - 1)),
    DEC_IR0         ("DEC",     1, 0, ParseHelpers.makeOneConstArgParser("@R0"), ctx -> OpcodeHelpers.readModifyWriteRegisterIndirect(ctx.ram, ctx.romByte & 0x1, (modRam, value) -> value - 1)),
    DEC_IR1         ("DEC",     1, 0, ParseHelpers.makeOneConstArgParser("@R1"), ctx -> OpcodeHelpers.readModifyWriteRegisterIndirect(ctx.ram, ctx.romByte & 0x1, (modRam, value) -> value - 1)),
    DEC_DR0         ("DEC",     1, 0, ParseHelpers.makeOneConstArgParser("R0"), ctx -> OpcodeHelpers.readModifyWriteRegister(ctx.ram, ctx.romByte & 0b00000111, (modRam, value) -> value - 1)),
    DEC_DR1         ("DEC",     1, 0, ParseHelpers.makeOneConstArgParser("R1"), ctx -> OpcodeHelpers.readModifyWriteRegister(ctx.ram, ctx.romByte & 0b00000111, (modRam, value) -> value - 1)),
    DEC_DR2         ("DEC",     1, 0, ParseHelpers.makeOneConstArgParser("R2"), ctx -> OpcodeHelpers.readModifyWriteRegister(ctx.ram, ctx.romByte & 0b00000111, (modRam, value) -> value - 1)),
    DEC_DR3         ("DEC",     1, 0, ParseHelpers.makeOneConstArgParser("R3"), ctx -> OpcodeHelpers.readModifyWriteRegister(ctx.ram, ctx.romByte & 0b00000111, (modRam, value) -> value - 1)),
    DEC_DR4         ("DEC",     1, 0, ParseHelpers.makeOneConstArgParser("R4"), ctx -> OpcodeHelpers.readModifyWriteRegister(ctx.ram, ctx.romByte & 0b00000111, (modRam, value) -> value - 1)),
    DEC_DR5         ("DEC",     1, 0, ParseHelpers.makeOneConstArgParser("R5"), ctx -> OpcodeHelpers.readModifyWriteRegister(ctx.ram, ctx.romByte & 0b00000111, (modRam, value) -> value - 1)),
    DEC_DR6         ("DEC",     1, 0, ParseHelpers.makeOneConstArgParser("R6"), ctx -> OpcodeHelpers.readModifyWriteRegister(ctx.ram, ctx.romByte & 0b00000111, (modRam, value) -> value - 1)),
    DEC_DR7         ("DEC",     1, 0, ParseHelpers.makeOneConstArgParser("R7"), ctx -> OpcodeHelpers.readModifyWriteRegister(ctx.ram, ctx.romByte & 0b00000111, (modRam, value) -> value - 1)),
    //Irregular 0x20-0x23
    JB              ("JB",      2, 2, ParseHelpers.makeOneBitArgJumpParser(), ctx -> {
        byte bitAddress = ctx.readRomAndIncrementPC();
        int offset = ctx.readRomAndIncrementPC();
        if (ctx.ram.readBit(bitAddress))
        {
            ctx.setProgramCounter(ctx.programCounter + offset);
        }
    }),
    AJMP_001        ("AJMP",    1, 1, ParseHelpers.makeOneLabelArgJumpParser(), ctx -> {
        int address = OpcodeHelpers.calculateAjmpAddress(ctx.romByte, ctx.readRomAndIncrementPC());
        ctx.setProgramCounter(OpcodeHelpers.calculateAjmpTarget(ctx.programCounter, address));
    }),
    RET             ("RET",     0, 0, NoArgOpNode::create, ctx -> {
        int upper = ctx.popStack() & 0xFF;
        int lower = ctx.popStack() & 0xFF;
        ctx.setProgramCounter((upper << 8) | lower);
    }),
    RL              ("RL",      1, 0, ParseHelpers.makeOneConstArgParser("A"), ctx ->
            OpcodeHelpers.readModifyWriteAccumulator(ctx.ram, (modRam, value) ->
            {
                int newLsb = (value >> 7) & 0x1;
                return newLsb | (value << 1);
            })),
    //Regular 0x24-0x2F
    ADD_IMM         ("ADD",     2, 1, ParseHelpers.makeTwoArgOneConstOneImmediateParser("A", false), ctx -> OpcodeHelpers.add(ctx.ram, ctx.readRomAndIncrementPC(), 0)),
    ADD_MEM         ("ADD",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("A", false), ctx -> OpcodeHelpers.add(ctx.ram, ctx.ram.readByte(ctx.readRomAndIncrementPC() & 0xFF), 0)),
    ADD_IR0         ("ADD",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "@R0"), ctx -> OpcodeHelpers.add(ctx.ram, OpcodeHelpers.readRegisterIndirect(ctx.ram, ctx.romByte & 0x1), 0)),
    ADD_IR1         ("ADD",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "@R1"), ctx -> OpcodeHelpers.add(ctx.ram, OpcodeHelpers.readRegisterIndirect(ctx.ram, ctx.romByte & 0x1), 0)),
    ADD_DR0         ("ADD",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R0"), ctx -> OpcodeHelpers.add(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111), 0)),
    ADD_DR1         ("ADD",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R1"), ctx -> OpcodeHelpers.add(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111), 0)),
    ADD_DR2         ("ADD",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R2"), ctx -> OpcodeHelpers.add(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111), 0)),
    ADD_DR3         ("ADD",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R3"), ctx -> OpcodeHelpers.add(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111), 0)),
    ADD_DR4         ("ADD",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R4"), ctx -> OpcodeHelpers.add(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111), 0)),
    ADD_DR5         ("ADD",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R5"), ctx -> OpcodeHelpers.add(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111), 0)),
    ADD_DR6         ("ADD",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R6"), ctx -> OpcodeHelpers.add(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111), 0)),
    ADD_DR7         ("ADD",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R7"), ctx -> OpcodeHelpers.add(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111), 0)),
    //Irregular 0x30-0x33
    JNB             ("JNB",     2, 2, ParseHelpers.makeOneBitArgJumpParser(), ctx -> {
        byte bitAddress = ctx.readRomAndIncrementPC();
        int offset = ctx.readRomAndIncrementPC();
        if (!ctx.ram.readBit(bitAddress))
        {
            ctx.setProgramCounter(ctx.programCounter + offset);
        }
    }),
    ACALL_001       ("ACALL",   1, 1, ParseHelpers.makeOneLabelArgJumpParser(), ctx -> {
        int address = OpcodeHelpers.calculateAjmpAddress(ctx.romByte, ctx.readRomAndIncrementPC());
        ctx.pushStateBeforeCall();
        ctx.setProgramCounter(OpcodeHelpers.calculateAjmpTarget(ctx.programCounter, address));
    }),
    RETI            ("RETI",    0, 0, NoArgOpNode::create, ctx -> {
        int upper = ctx.popStack() & 0xFF;
        int lower = ctx.popStack() & 0xFF;
        ctx.setProgramCounter((upper << 8) | lower);
        ctx.interrupts.returnFromIsr();
    }),
    RLC             ("RLC",     1, 0, ParseHelpers.makeOneConstArgParser("A"), ctx ->
        OpcodeHelpers.readModifyWriteAccumulator(ctx.ram, (modRam, value) ->
        {
            int newLsb = modRam.readBit(Constants.BIT_ADDRESS_CARRY) ? 0x1 : 0;
            boolean newCarry = (value & 0b10000000) != 0;
            modRam.writeBit(Constants.BIT_ADDRESS_CARRY, BitWriteMode.of(newCarry));
            return newLsb | (value << 1);
        })
    ),
    //Regular 0x34-0x3F
    ADDC_IMM        ("ADDC",    2, 1, ParseHelpers.makeTwoArgOneConstOneImmediateParser("A", false), ctx -> OpcodeHelpers.addc(ctx.ram, ctx.readRomAndIncrementPC())),
    ADDC_MEM        ("ADDC",    2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("A", false), ctx -> OpcodeHelpers.addc(ctx.ram, ctx.ram.readByte(ctx.readRomAndIncrementPC() & 0xFF))),
    ADDC_IR0        ("ADDC",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "@R0"), ctx -> OpcodeHelpers.addc(ctx.ram, OpcodeHelpers.readRegisterIndirect(ctx.ram, ctx.romByte & 0x1))),
    ADDC_IR1        ("ADDC",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "@R1"), ctx -> OpcodeHelpers.addc(ctx.ram, OpcodeHelpers.readRegisterIndirect(ctx.ram, ctx.romByte & 0x1))),
    ADDC_DR0        ("ADDC",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "R0"), ctx -> OpcodeHelpers.addc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    ADDC_DR1        ("ADDC",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "R1"), ctx -> OpcodeHelpers.addc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    ADDC_DR2        ("ADDC",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "R2"), ctx -> OpcodeHelpers.addc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    ADDC_DR3        ("ADDC",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "R3"), ctx -> OpcodeHelpers.addc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    ADDC_DR4        ("ADDC",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "R4"), ctx -> OpcodeHelpers.addc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    ADDC_DR5        ("ADDC",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "R5"), ctx -> OpcodeHelpers.addc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    ADDC_DR6        ("ADDC",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "R6"), ctx -> OpcodeHelpers.addc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    ADDC_DR7        ("ADDC",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "R7"), ctx -> OpcodeHelpers.addc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    //Irregular 0x40-0x43
    JC              ("JC",      1, 1, ParseHelpers.makeOneLabelArgJumpParser(), ctx -> {
        int offset = ctx.readRomAndIncrementPC();
        if (ctx.ram.readBit(Constants.BIT_ADDRESS_CARRY))
        {
            ctx.setProgramCounter(ctx.programCounter + offset);
        }
    }),
    AJMP_010        ("AJMP",    1, 1, ParseHelpers.makeOneLabelArgJumpParser(), ctx -> {
        int address = OpcodeHelpers.calculateAjmpAddress(ctx.romByte, ctx.readRomAndIncrementPC());
        ctx.setProgramCounter(OpcodeHelpers.calculateAjmpTarget(ctx.programCounter, address));
    }),
    ORL_MEM_ACC     ("ORL",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("A", true), ctx -> OpcodeHelpers.orMem(ctx.ram, ctx.readRomAndIncrementPC() & 0xFF, ctx.ram.readByte(Constants.ADDRESS_ACCUMULATOR))),
    ORL_MEM_IMM     ("ORL",     2, 2, ParseHelpers.makeTwoArgOneAddressOneImmediateParser(), ctx -> OpcodeHelpers.orMem(ctx.ram, ctx.readRomAndIncrementPC() & 0xFF, ctx.readRomAndIncrementPC())),
    //Regular 0x44-0x4F
    ORL_IMM         ("ORL",     2, 1, ParseHelpers.makeTwoArgOneConstOneImmediateParser("A", false), 1, ctx -> OpcodeHelpers.orAcc(ctx.ram, ctx.readRomAndIncrementPC())),
    ORL_MEM         ("ORL",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("A", false), ctx -> OpcodeHelpers.orAcc(ctx.ram, ctx.ram.readByte(ctx.readRomAndIncrementPC() & 0xFF))),
    ORL_IR0         ("ORL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "@R0"), ctx -> OpcodeHelpers.orAcc(ctx.ram, OpcodeHelpers.readRegisterIndirect(ctx.ram, ctx.romByte & 0x1))),
    ORL_IR1         ("ORL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "@R1"), ctx -> OpcodeHelpers.orAcc(ctx.ram, OpcodeHelpers.readRegisterIndirect(ctx.ram, ctx.romByte & 0x1))),
    ORL_DR0         ("ORL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R0"), ctx -> OpcodeHelpers.orAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    ORL_DR1         ("ORL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R1"), ctx -> OpcodeHelpers.orAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    ORL_DR2         ("ORL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R2"), ctx -> OpcodeHelpers.orAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    ORL_DR3         ("ORL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R3"), ctx -> OpcodeHelpers.orAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    ORL_DR4         ("ORL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R4"), ctx -> OpcodeHelpers.orAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    ORL_DR5         ("ORL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R5"), ctx -> OpcodeHelpers.orAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    ORL_DR6         ("ORL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R6"), ctx -> OpcodeHelpers.orAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    ORL_DR7         ("ORL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R7"), ctx -> OpcodeHelpers.orAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    //Irregular 0x50-0x53
    JNC             ("JNC",     1, 1, ParseHelpers.makeOneLabelArgJumpParser(), ctx -> {
        int offset = ctx.readRomAndIncrementPC();
        if (!ctx.ram.readBit(Constants.BIT_ADDRESS_CARRY))
        {
            ctx.setProgramCounter(ctx.programCounter + offset);
        }
    }),
    ACALL_010       ("ACALL",   1, 1, ParseHelpers.makeOneLabelArgJumpParser(), ctx -> {
        int address = OpcodeHelpers.calculateAjmpAddress(ctx.romByte, ctx.readRomAndIncrementPC());
        ctx.pushStateBeforeCall();
        ctx.setProgramCounter(OpcodeHelpers.calculateAjmpTarget(ctx.programCounter, address));
    }),
    ANL_MEM_ACC     ("ANL",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("A", true), ctx -> OpcodeHelpers.andMem(ctx.ram, ctx.readRomAndIncrementPC() & 0xFF, ctx.ram.readByte(Constants.ADDRESS_ACCUMULATOR))),
    ANL_MEM_IMM     ("ANL",     2, 2, ParseHelpers.makeTwoArgOneAddressOneImmediateParser(), ctx -> OpcodeHelpers.andMem(ctx.ram, ctx.readRomAndIncrementPC() & 0xFF, ctx.readRomAndIncrementPC())),
    //Regular 0x54-0x5F
    ANL_IMM         ("ANL",     2, 1, ParseHelpers.makeTwoArgOneConstOneImmediateParser("A", false), 1, ctx -> OpcodeHelpers.andAcc(ctx.ram, ctx.readRomAndIncrementPC())),
    ANL_MEM         ("ANL",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("A", false), ctx -> OpcodeHelpers.andAcc(ctx.ram, ctx.ram.readByte(ctx.readRomAndIncrementPC() & 0xFF))),
    ANL_IR0         ("ANL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "@R0"), ctx -> OpcodeHelpers.andAcc(ctx.ram, OpcodeHelpers.readRegisterIndirect(ctx.ram, ctx.romByte & 0x1))),
    ANL_IR1         ("ANL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "@R1"), ctx -> OpcodeHelpers.andAcc(ctx.ram, OpcodeHelpers.readRegisterIndirect(ctx.ram, ctx.romByte & 0x1))),
    ANL_DR0         ("ANL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R0"), ctx -> OpcodeHelpers.andAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    ANL_DR1         ("ANL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R1"), ctx -> OpcodeHelpers.andAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    ANL_DR2         ("ANL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R2"), ctx -> OpcodeHelpers.andAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    ANL_DR3         ("ANL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R3"), ctx -> OpcodeHelpers.andAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    ANL_DR4         ("ANL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R4"), ctx -> OpcodeHelpers.andAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    ANL_DR5         ("ANL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R5"), ctx -> OpcodeHelpers.andAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    ANL_DR6         ("ANL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R6"), ctx -> OpcodeHelpers.andAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    ANL_DR7         ("ANL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R7"), ctx -> OpcodeHelpers.andAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    //Irregular 0x60-0x63
    JZ              ("JZ",      1, 1, ParseHelpers.makeOneLabelArgJumpParser(), ctx -> {
        int offset = ctx.readRomAndIncrementPC();
        if (ctx.ram.read(Constants.ADDRESS_ACCUMULATOR) == 0)
        {
            ctx.setProgramCounter(ctx.programCounter + offset);
        }
    }),
    AJMP_011        ("AJMP",    1, 1, ParseHelpers.makeOneLabelArgJumpParser(), ctx -> {
        int address = OpcodeHelpers.calculateAjmpAddress(ctx.romByte, ctx.readRomAndIncrementPC());
        ctx.setProgramCounter(OpcodeHelpers.calculateAjmpTarget(ctx.programCounter, address));
    }),
    XRL_MEM_ACC     ("XRL",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("A", true), ctx -> OpcodeHelpers.xorMem(ctx.ram, ctx.readRomAndIncrementPC() & 0xFF, ctx.ram.readByte(Constants.ADDRESS_ACCUMULATOR))),
    XRL_MEM_IMM     ("XRL",     2, 2, ParseHelpers.makeTwoArgOneAddressOneImmediateParser(), ctx -> OpcodeHelpers.xorMem(ctx.ram, ctx.readRomAndIncrementPC() & 0xFF, ctx.readRomAndIncrementPC())),
    //Regular 0x64-0x6F
    XRL_IMM         ("XRL",     2, 1, ParseHelpers.makeTwoArgOneConstOneImmediateParser("A", false), 1, ctx -> OpcodeHelpers.xorAcc(ctx.ram, ctx.readRomAndIncrementPC())),
    XRL_MEM         ("XRL",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("A", false), ctx -> OpcodeHelpers.xorAcc(ctx.ram, ctx.ram.readByte(ctx.readRomAndIncrementPC() & 0xFF))),
    XRL_IR0         ("XRL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "@R0"), ctx -> OpcodeHelpers.xorAcc(ctx.ram, OpcodeHelpers.readRegisterIndirect(ctx.ram, ctx.romByte & 0x1))),
    XRL_IR1         ("XRL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "@R1"), ctx -> OpcodeHelpers.xorAcc(ctx.ram, OpcodeHelpers.readRegisterIndirect(ctx.ram, ctx.romByte & 0x1))),
    XRL_DR0         ("XRL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R0"), ctx -> OpcodeHelpers.xorAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    XRL_DR1         ("XRL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R1"), ctx -> OpcodeHelpers.xorAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    XRL_DR2         ("XRL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R2"), ctx -> OpcodeHelpers.xorAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    XRL_DR3         ("XRL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R3"), ctx -> OpcodeHelpers.xorAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    XRL_DR4         ("XRL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R4"), ctx -> OpcodeHelpers.xorAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    XRL_DR5         ("XRL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R5"), ctx -> OpcodeHelpers.xorAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    XRL_DR6         ("XRL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R6"), ctx -> OpcodeHelpers.xorAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    XRL_DR7         ("XRL",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R7"), ctx -> OpcodeHelpers.xorAcc(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    //Irregular 0x70-0x73
    JNZ             ("JNZ",     1, 1, ParseHelpers.makeOneLabelArgJumpParser(), ctx -> {
        int offset = ctx.readRomAndIncrementPC();
        if (ctx.ram.read(Constants.ADDRESS_ACCUMULATOR) != 0)
        {
            ctx.setProgramCounter(ctx.programCounter + offset);
        }
    }),
    ACALL_011       ("ACALL",   1, 1, ParseHelpers.makeOneLabelArgJumpParser(), ctx -> {
        int address = OpcodeHelpers.calculateAjmpAddress(ctx.romByte, ctx.readRomAndIncrementPC());
        ctx.pushStateBeforeCall();
        ctx.setProgramCounter(OpcodeHelpers.calculateAjmpTarget(ctx.programCounter, address));
    }),
    ORL_C_BIT       ("ORL",     2, 1, ParseHelpers.makeTwoArgOneConstOneBitParser("C", false, false), ctx -> {
        boolean bit = ctx.ram.readBit(ctx.readRomAndIncrementPC() & 0xFF);
        boolean carry = ctx.ram.readBit(Constants.BIT_ADDRESS_CARRY);
        ctx.ram.writeBit(Constants.BIT_ADDRESS_CARRY, BitWriteMode.of(bit || carry));
    }),
    JMP             ("JMP",     1, 0, ParseHelpers.makeOneConstArgParser("@A+DPTR"), ctx -> {
        int dptr = OpcodeHelpers.readDataPointer(ctx.ram);
        int acc = ctx.ram.read(Constants.ADDRESS_ACCUMULATOR);
        ctx.setProgramCounter(dptr + acc);
    }),
    //Regular 0x74-0x7F
    MOV_ACC_IMM     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneImmediateParser("A", false), ctx -> ctx.ram.writeByte(Constants.ADDRESS_ACCUMULATOR, ctx.readRomAndIncrementPC())),
    MOV_MEM_IMM     ("MOV",     2, 2, ParseHelpers.makeTwoArgOneAddressOneImmediateParser(), ctx -> ctx.ram.writeByte(ctx.readRomAndIncrementPC() & 0xFF, ctx.readRomAndIncrementPC())),
    MOV_IR0_IMM     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneImmediateParser("@R0", false), ctx -> OpcodeHelpers.writeRegisterIndirect(ctx.ram, ctx.romByte & 0x1, ctx.readRomAndIncrementPC())),
    MOV_IR1_IMM     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneImmediateParser("@R1", false), ctx -> OpcodeHelpers.writeRegisterIndirect(ctx.ram, ctx.romByte & 0x1, ctx.readRomAndIncrementPC())),
    MOV_DR0_IMM     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneImmediateParser("R0", false), 1, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.readRomAndIncrementPC())),
    MOV_DR1_IMM     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneImmediateParser("R1", false), 1, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.readRomAndIncrementPC())),
    MOV_DR2_IMM     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneImmediateParser("R2", false), 1, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.readRomAndIncrementPC())),
    MOV_DR3_IMM     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneImmediateParser("R3", false), 1, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.readRomAndIncrementPC())),
    MOV_DR4_IMM     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneImmediateParser("R4", false), 1, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.readRomAndIncrementPC())),
    MOV_DR5_IMM     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneImmediateParser("R5", false), 1, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.readRomAndIncrementPC())),
    MOV_DR6_IMM     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneImmediateParser("R6", false), 1, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.readRomAndIncrementPC())),
    MOV_DR7_IMM     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneImmediateParser("R7", false), 1, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.readRomAndIncrementPC())),
    //Irregular 0x80-0x83
    SJMP            ("SJMP",    1, 1, ParseHelpers.makeOneLabelArgJumpParser(), ctx -> {
        int offset = ctx.readRomAndIncrementPC();
        ctx.setProgramCounter(ctx.programCounter + offset);
    }),
    AJMP_100        ("AJMP",    1, 1, ParseHelpers.makeOneLabelArgJumpParser(), ctx -> {
        int address = OpcodeHelpers.calculateAjmpAddress(ctx.romByte, ctx.readRomAndIncrementPC());
        ctx.setProgramCounter(OpcodeHelpers.calculateAjmpTarget(ctx.programCounter, address));
    }),
    ANL_C_BIT       ("ANL",     2, 1, ParseHelpers.makeTwoArgOneConstOneBitParser("C", false, false), ctx -> {
        boolean bit = ctx.ram.readBit(ctx.readRomAndIncrementPC() & 0xFF);
        boolean carry = ctx.ram.readBit(Constants.BIT_ADDRESS_CARRY);
        ctx.ram.writeBit(Constants.BIT_ADDRESS_CARRY, BitWriteMode.of(bit && carry));
    }),
    MOVC_ACC_IAPC   ("MOVC",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "@A+PC"), ctx -> {
        int acc = ctx.ram.read(Constants.ADDRESS_ACCUMULATOR);
        int address = acc + ctx.programCounter;
        address %= Constants.ROM_SIZE;
        ctx.ram.writeByte(Constants.ADDRESS_ACCUMULATOR, ctx.rom[address]);
    }),
    //Regular 0x84-0x8F
    DIV_AB          ("DIV",     1, 0, ParseHelpers.makeOneConstArgParser("AB"), ctx -> {
        ctx.ram.writeBit(Constants.BIT_ADDRESS_CARRY, BitWriteMode.CLEAR);

        int acc = ctx.ram.read(Constants.ADDRESS_ACCUMULATOR);
        int b = ctx.ram.read(Constants.ADDRESS_REGISTER_B);
        if (b > 0)
        {
            ctx.ram.write(Constants.ADDRESS_ACCUMULATOR, acc / b);
            ctx.ram.write(Constants.ADDRESS_REGISTER_B, acc % b);
            ctx.ram.writeBit(Constants.BIT_ADDRESS_OVERFLOW, BitWriteMode.CLEAR);
        }
        else
        {
            ctx.ram.writeBit(Constants.BIT_ADDRESS_OVERFLOW, BitWriteMode.SET);
        }
    }),
    MOV_MEM_MEM     ("MOV",     2, 2, ParseHelpers.makeTwoAddressArgParser(), ctx -> {
        int destAddr = ctx.readRomAndIncrementPC() & 0xFF;
        int srcAddr = ctx.readRomAndIncrementPC() & 0xFF;
        ctx.ram.writeByte(destAddr, ctx.ram.readByte(srcAddr));
    }),
    MOV_MEM_IR0     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("@R0", true), ctx -> ctx.ram.writeByte(ctx.readRomAndIncrementPC() & 0xFF, OpcodeHelpers.readRegisterIndirect(ctx.ram, ctx.romByte & 0x1))),
    MOV_MEM_IR1     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("@R1", true), ctx -> ctx.ram.writeByte(ctx.readRomAndIncrementPC() & 0xFF, OpcodeHelpers.readRegisterIndirect(ctx.ram, ctx.romByte & 0x1))),
    MOV_MEM_DR0     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("R0", true), 1, ctx -> ctx.ram.writeByte(ctx.readRomAndIncrementPC() & 0xFF, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    MOV_MEM_DR1     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("R1", true), 1, ctx -> ctx.ram.writeByte(ctx.readRomAndIncrementPC() & 0xFF, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    MOV_MEM_DR2     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("R2", true), 1, ctx -> ctx.ram.writeByte(ctx.readRomAndIncrementPC() & 0xFF, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    MOV_MEM_DR3     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("R3", true), 1, ctx -> ctx.ram.writeByte(ctx.readRomAndIncrementPC() & 0xFF, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    MOV_MEM_DR4     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("R4", true), 1, ctx -> ctx.ram.writeByte(ctx.readRomAndIncrementPC() & 0xFF, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    MOV_MEM_DR5     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("R5", true), 1, ctx -> ctx.ram.writeByte(ctx.readRomAndIncrementPC() & 0xFF, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    MOV_MEM_DR6     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("R6", true), 1, ctx -> ctx.ram.writeByte(ctx.readRomAndIncrementPC() & 0xFF, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    MOV_MEM_DR7     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("R7", true), 1, ctx -> ctx.ram.writeByte(ctx.readRomAndIncrementPC() & 0xFF, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    //Irregular 0x90-0x93
    MOV_DPTR        ("MOV",     2, 2, ParseHelpers.makeMovDptrParser(), ctx ->
    {
        ctx.ram.writeByte(Constants.ADDRESS_DATA_POINTER_UPPER, ctx.readRomAndIncrementPC());
        ctx.ram.writeByte(Constants.ADDRESS_DATA_POINTER_LOWER, ctx.readRomAndIncrementPC());
    }),
    ACALL_100       ("ACALL",   1, 1, ParseHelpers.makeOneLabelArgJumpParser(), ctx -> {
        int address = OpcodeHelpers.calculateAjmpAddress(ctx.romByte, ctx.readRomAndIncrementPC());
        ctx.pushStateBeforeCall();
        ctx.setProgramCounter(OpcodeHelpers.calculateAjmpTarget(ctx.programCounter, address));
    }),
    MOV_BIT_C       ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneBitParser("C", true, false), ctx -> {
        boolean carry = ctx.ram.readBit(Constants.BIT_ADDRESS_CARRY);
        ctx.ram.writeBit(ctx.readRomAndIncrementPC(), BitWriteMode.of(carry));
    }),
    MOVC_ACC_IADPTR ("MOVC",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "@A+DPTR"), ctx -> {
        int acc = ctx.ram.read(Constants.ADDRESS_ACCUMULATOR);
        int address = acc + OpcodeHelpers.readDataPointer(ctx.ram);
        address %= Constants.ROM_SIZE;
        ctx.ram.writeByte(Constants.ADDRESS_ACCUMULATOR, ctx.rom[address]);
    }),
    //Regular 0x94-0x9F
    SUBB_IMM        ("SUBB",    2, 1, ParseHelpers.makeTwoArgOneConstOneImmediateParser("A", false), ctx -> OpcodeHelpers.subb(ctx.ram, ctx.readRomAndIncrementPC())),
    SUBB_MEM        ("SUBB",    2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("A", false), ctx -> OpcodeHelpers.subb(ctx.ram, ctx.ram.readByte(ctx.readRomAndIncrementPC() & 0xFF))),
    SUBB_IR0        ("SUBB",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "@R0"), ctx -> OpcodeHelpers.subb(ctx.ram, OpcodeHelpers.readRegisterIndirect(ctx.ram, ctx.romByte & 0x1))),
    SUBB_IR1        ("SUBB",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "@R1"), ctx -> OpcodeHelpers.subb(ctx.ram, OpcodeHelpers.readRegisterIndirect(ctx.ram, ctx.romByte & 0x1))),
    SUBB_DR0        ("SUBB",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "R0"), ctx -> OpcodeHelpers.subb(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    SUBB_DR1        ("SUBB",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "R1"), ctx -> OpcodeHelpers.subb(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    SUBB_DR2        ("SUBB",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "R2"), ctx -> OpcodeHelpers.subb(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    SUBB_DR3        ("SUBB",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "R3"), ctx -> OpcodeHelpers.subb(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    SUBB_DR4        ("SUBB",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "R4"), ctx -> OpcodeHelpers.subb(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    SUBB_DR5        ("SUBB",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "R5"), ctx -> OpcodeHelpers.subb(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    SUBB_DR6        ("SUBB",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "R6"), ctx -> OpcodeHelpers.subb(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    SUBB_DR7        ("SUBB",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "R7"), ctx -> OpcodeHelpers.subb(ctx.ram, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    //Irregular 0xA0-0xA3
    ORL_C_NBIT      ("ORL",     2, 1, ParseHelpers.makeTwoArgOneConstOneBitParser("C", false, true), ctx -> {
        boolean bit = ctx.ram.readBit(ctx.readRomAndIncrementPC() & 0xFF);
        boolean carry = ctx.ram.readBit(Constants.BIT_ADDRESS_CARRY);
        ctx.ram.writeBit(Constants.BIT_ADDRESS_CARRY, BitWriteMode.of(!bit || carry));
    }),
    AJMP_101        ("AJMP",    1, 1, ParseHelpers.makeOneLabelArgJumpParser(), ctx -> {
        int address = OpcodeHelpers.calculateAjmpAddress(ctx.romByte, ctx.readRomAndIncrementPC());
        ctx.setProgramCounter(OpcodeHelpers.calculateAjmpTarget(ctx.programCounter, address));
    }),
    MOV_C_BIT       ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneBitParser("C", false, false), ctx -> {
        boolean bit = ctx.ram.readBit(ctx.readRomAndIncrementPC());
        ctx.ram.writeBit(Constants.BIT_ADDRESS_CARRY, BitWriteMode.of(bit));
    }),
    INC_DPTR        ("INC",     1, 0, ParseHelpers.makeOneConstArgParser("DPTR"), ctx -> {
        int value = OpcodeHelpers.readDataPointer(ctx.ram);
        value++;
        OpcodeHelpers.writeDataPointer(ctx.ram, value);
    }),
    //Regular 0xA4-0xAF
    MUL_AB          ("MUL",     1, 0, ParseHelpers.makeOneConstArgParser("AB"), ctx -> {
        int acc = ctx.ram.read(Constants.ADDRESS_ACCUMULATOR);
        int b = ctx.ram.read(Constants.ADDRESS_REGISTER_B);
        int result = acc * b;
        ctx.ram.writeBit(Constants.BIT_ADDRESS_OVERFLOW, BitWriteMode.of(result > 255));
        ctx.ram.writeBit(Constants.BIT_ADDRESS_CARRY, BitWriteMode.CLEAR);
        ctx.ram.write(Constants.ADDRESS_ACCUMULATOR, result);
        ctx.ram.write(Constants.ADDRESS_REGISTER_B, result >> 8);
    }),
    RESERVED        ("",        0, 0, (line, op, operands) -> null, ctx -> {}),
    MOV_IR0_MEM     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("@R0", false), ctx -> OpcodeHelpers.writeRegisterIndirect(ctx.ram, ctx.romByte & 0x1, ctx.ram.readByte(ctx.readRomAndIncrementPC() & 0xFF))),
    MOV_IR1_MEM     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("@R1", false), ctx -> OpcodeHelpers.writeRegisterIndirect(ctx.ram, ctx.romByte & 0x1, ctx.ram.readByte(ctx.readRomAndIncrementPC() & 0xFF))),
    MOV_DR0_MEM     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("R0", false), 1, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.ram.readByte(ctx.readRomAndIncrementPC() & 0xFF))),
    MOV_DR1_MEM     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("R1", false), 1, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.ram.readByte(ctx.readRomAndIncrementPC() & 0xFF))),
    MOV_DR2_MEM     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("R2", false), 1, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.ram.readByte(ctx.readRomAndIncrementPC() & 0xFF))),
    MOV_DR3_MEM     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("R3", false), 1, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.ram.readByte(ctx.readRomAndIncrementPC() & 0xFF))),
    MOV_DR4_MEM     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("R4", false), 1, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.ram.readByte(ctx.readRomAndIncrementPC() & 0xFF))),
    MOV_DR5_MEM     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("R5", false), 1, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.ram.readByte(ctx.readRomAndIncrementPC() & 0xFF))),
    MOV_DR6_MEM     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("R6", false), 1, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.ram.readByte(ctx.readRomAndIncrementPC() & 0xFF))),
    MOV_DR7_MEM     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("R7", false), 1, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.ram.readByte(ctx.readRomAndIncrementPC() & 0xFF))),
    //Irregular 0xB0-0xB3
    ANL_C_NBIT      ("ANL",     2, 1, ParseHelpers.makeTwoArgOneConstOneBitParser("C", false, true), ctx -> {
        boolean bit = ctx.ram.readBit(ctx.readRomAndIncrementPC() & 0xFF);
        boolean carry = ctx.ram.readBit(Constants.BIT_ADDRESS_CARRY);
        ctx.ram.writeBit(Constants.BIT_ADDRESS_CARRY, BitWriteMode.of(!bit && carry));
    }),
    ACALL_101       ("ACALL",   1, 1, ParseHelpers.makeOneLabelArgJumpParser(), ctx -> {
        int address = OpcodeHelpers.calculateAjmpAddress(ctx.romByte, ctx.readRomAndIncrementPC());
        ctx.pushStateBeforeCall();
        ctx.setProgramCounter(OpcodeHelpers.calculateAjmpTarget(ctx.programCounter, address));
    }),
    CPL_BIT         ("CPL",     1, 1, ParseHelpers.makeOneBitArgParser(false), ctx -> ctx.ram.writeBit(ctx.readRomAndIncrementPC() & 0xFF, BitWriteMode.COMPLEMENT)),
    CPL_C           ("CPL",     1, 0, ParseHelpers.makeOneConstArgParser("C"), 1, ctx -> ctx.ram.writeBit(Constants.BIT_ADDRESS_CARRY, BitWriteMode.COMPLEMENT)),
    //Regular 0xB4-0xBF
    CJNE_ACC_IMM    ("CJNE",    3, 2, ParseHelpers.makeThreeArgJumpParser("A", ParseHelpers::parseImmediateOperand), ctx -> {
        byte left = ctx.ram.readByte(Constants.ADDRESS_ACCUMULATOR);
        OpcodeHelpers.compareJumpNotEqual(this, ctx.ram, left, ctx.readRomAndIncrementPC(), ctx.readRomAndIncrementPC());
    }),
    CJNE_ACC_MEM    ("CJNE",    3, 2, ParseHelpers.makeThreeArgJumpParser("A", ParseHelpers::parseAddressOperand), ctx -> {
        int address = ctx.readRomAndIncrementPC() & 0xFF;
        int offset = ctx.readRomAndIncrementPC();
        byte left = ctx.ram.readByte(Constants.ADDRESS_ACCUMULATOR);
        OpcodeHelpers.compareJumpNotEqual(this, ctx.ram, left, ctx.ram.readByte(address), offset);
    }),
    CJNE_IR0_IMM    ("CJNE",    3, 2, ParseHelpers.makeThreeArgJumpParser("@R0", ParseHelpers::parseImmediateOperand), ctx -> {
        byte left = OpcodeHelpers.readRegisterIndirect(ctx.ram, ctx.romByte & 0x1);
        OpcodeHelpers.compareJumpNotEqual(this, ctx.ram, left, ctx.readRomAndIncrementPC(), ctx.readRomAndIncrementPC());
    }),
    CJNE_IR1_IMM    ("CJNE",    3, 2, ParseHelpers.makeThreeArgJumpParser("@R1", ParseHelpers::parseImmediateOperand), ctx -> {
        byte left = OpcodeHelpers.readRegisterIndirect(ctx.ram, ctx.romByte & 0x1);
        OpcodeHelpers.compareJumpNotEqual(this, ctx.ram, left, ctx.readRomAndIncrementPC(), ctx.readRomAndIncrementPC());
    }),
    CJNE_DR0_IMM    ("CJNE",    3, 2, ParseHelpers.makeThreeArgJumpParser("R0", ParseHelpers::parseImmediateOperand), ctx -> {
        byte left = OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111);
        OpcodeHelpers.compareJumpNotEqual(this, ctx.ram, left, ctx.readRomAndIncrementPC(), ctx.readRomAndIncrementPC());
    }),
    CJNE_DR1_IMM    ("CJNE",    3, 2, ParseHelpers.makeThreeArgJumpParser("R1", ParseHelpers::parseImmediateOperand), ctx -> {
        byte left = OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111);
        OpcodeHelpers.compareJumpNotEqual(this, ctx.ram, left, ctx.readRomAndIncrementPC(), ctx.readRomAndIncrementPC());
    }),
    CJNE_DR2_IMM    ("CJNE",    3, 2, ParseHelpers.makeThreeArgJumpParser("R2", ParseHelpers::parseImmediateOperand), ctx -> {
        byte left = OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111);
        OpcodeHelpers.compareJumpNotEqual(this, ctx.ram, left, ctx.readRomAndIncrementPC(), ctx.readRomAndIncrementPC());
    }),
    CJNE_DR3_IMM    ("CJNE",    3, 2, ParseHelpers.makeThreeArgJumpParser("R3", ParseHelpers::parseImmediateOperand), ctx -> {
        byte left = OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111);
        OpcodeHelpers.compareJumpNotEqual(this, ctx.ram, left, ctx.readRomAndIncrementPC(), ctx.readRomAndIncrementPC());
    }),
    CJNE_DR4_IMM    ("CJNE",    3, 2, ParseHelpers.makeThreeArgJumpParser("R4", ParseHelpers::parseImmediateOperand), ctx -> {
        byte left = OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111);
        OpcodeHelpers.compareJumpNotEqual(this, ctx.ram, left, ctx.readRomAndIncrementPC(), ctx.readRomAndIncrementPC());
    }),
    CJNE_DR5_IMM    ("CJNE",    3, 2, ParseHelpers.makeThreeArgJumpParser("R5", ParseHelpers::parseImmediateOperand), ctx -> {
        byte left = OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111);
        OpcodeHelpers.compareJumpNotEqual(this, ctx.ram, left, ctx.readRomAndIncrementPC(), ctx.readRomAndIncrementPC());
    }),
    CJNE_DR6_IMM    ("CJNE",    3, 2, ParseHelpers.makeThreeArgJumpParser("R6", ParseHelpers::parseImmediateOperand), ctx -> {
        byte left = OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111);
        OpcodeHelpers.compareJumpNotEqual(this, ctx.ram, left, ctx.readRomAndIncrementPC(), ctx.readRomAndIncrementPC());
    }),
    CJNE_DR7_IMM    ("CJNE",    3, 2, ParseHelpers.makeThreeArgJumpParser("R7", ParseHelpers::parseImmediateOperand), ctx -> {
        byte left = OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111);
        OpcodeHelpers.compareJumpNotEqual(this, ctx.ram, left, ctx.readRomAndIncrementPC(), ctx.readRomAndIncrementPC());
    }),
    //Irregular 0xC0-0xC3
    PUSH            ("PUSH",    1, 1, ParseHelpers.makeOneAddressArgParser(), ctx -> {
        int address = ctx.readRomAndIncrementPC() & 0xFF;
        ctx.pushStack(ctx.ram.readByte(address));
    }),
    AJMP_110        ("AJMP",    1, 1, ParseHelpers.makeOneLabelArgJumpParser(), ctx -> {
        int address = OpcodeHelpers.calculateAjmpAddress(ctx.romByte, ctx.readRomAndIncrementPC());
        ctx.setProgramCounter(OpcodeHelpers.calculateAjmpTarget(ctx.programCounter, address));
    }),
    CLR_BIT         ("CLR",     1, 1, ParseHelpers.makeOneBitArgParser(false), ctx -> ctx.ram.writeBit(ctx.readRomAndIncrementPC() & 0xFF, BitWriteMode.CLEAR)),
    CLR_C           ("CLR",     1, 0, ParseHelpers.makeOneConstArgParser("C"), 1, ctx -> ctx.ram.writeBit(Constants.BIT_ADDRESS_CARRY, BitWriteMode.CLEAR)),
    //Regular 0xC4-0xCF
    SWAP            ("SWAP",    1, 0, ParseHelpers.makeOneConstArgParser("A"), ctx -> OpcodeHelpers.readModifyWriteAccumulator(ctx.ram, (modRam, value) ->
    {
        int upper = value & 0xF0;
        int lower = value & 0x0F;
        return (upper >> 4) | (lower << 4);
    })),
    XCH_MEM         ("XCH",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("A", false), ctx -> OpcodeHelpers.readModifyWriteAccumulatorWithArg(ctx.ram, ctx.readRomAndIncrementPC(), (modRam, value, arg) ->
    {
        int address = arg & 0xFF;
        int ramValue = modRam.read(address);
        modRam.write(address, value);
        return ramValue;
    })),
    XCH_IR0         ("XCH",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "@R0"), ctx -> OpcodeHelpers.readModifyWriteAccumulatorWithArg(ctx.ram, (byte) (ctx.romByte & 0x1), (modRam, value, arg) ->
    {
        int register = arg & 0xFF;
        byte regValue = OpcodeHelpers.readRegisterIndirect(modRam, register);
        OpcodeHelpers.writeRegisterIndirect(modRam, register, (byte) (value & 0xFF));
        return regValue;
    })),
    XCH_IR1         ("XCH",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "@R1"), ctx -> OpcodeHelpers.readModifyWriteAccumulatorWithArg(ctx.ram, (byte) (ctx.romByte & 0x1), (modRam, value, arg) ->
    {
        int register = arg & 0xFF;
        byte regValue = OpcodeHelpers.readRegisterIndirect(modRam, register);
        OpcodeHelpers.writeRegisterIndirect(modRam, register, (byte) (value & 0xFF));
        return regValue;
    })),
    XCH_DR0         ("XCH",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R0"), ctx -> OpcodeHelpers.readModifyWriteAccumulatorWithArg(ctx.ram, (byte) (ctx.romByte & 0b00000111), (modRam, value, arg) ->
    {
        int register = arg & 0xFF;
        byte regValue = OpcodeHelpers.readRegisterDirect(modRam, register);
        OpcodeHelpers.writeRegisterDirect(modRam, register, (byte) (value & 0xFF));
        return regValue;
    })),
    XCH_DR1         ("XCH",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R1"), ctx -> OpcodeHelpers.readModifyWriteAccumulatorWithArg(ctx.ram, (byte) (ctx.romByte & 0b00000111), (modRam, value, arg) ->
    {
        int register = arg & 0xFF;
        byte regValue = OpcodeHelpers.readRegisterDirect(modRam, register);
        OpcodeHelpers.writeRegisterDirect(modRam, register, (byte) (value & 0xFF));
        return regValue;
    })),
    XCH_DR2         ("XCH",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R2"), ctx -> OpcodeHelpers.readModifyWriteAccumulatorWithArg(ctx.ram, (byte) (ctx.romByte & 0b00000111), (modRam, value, arg) ->
    {
        int register = arg & 0xFF;
        byte regValue = OpcodeHelpers.readRegisterDirect(modRam, register);
        OpcodeHelpers.writeRegisterDirect(modRam, register, (byte) (value & 0xFF));
        return regValue;
    })),
    XCH_DR3         ("XCH",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R3"), ctx -> OpcodeHelpers.readModifyWriteAccumulatorWithArg(ctx.ram, (byte) (ctx.romByte & 0b00000111), (modRam, value, arg) ->
    {
        int register = arg & 0xFF;
        byte regValue = OpcodeHelpers.readRegisterDirect(modRam, register);
        OpcodeHelpers.writeRegisterDirect(modRam, register, (byte) (value & 0xFF));
        return regValue;
    })),
    XCH_DR4         ("XCH",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R4"), ctx -> OpcodeHelpers.readModifyWriteAccumulatorWithArg(ctx.ram, (byte) (ctx.romByte & 0b00000111), (modRam, value, arg) ->
    {
        int register = arg & 0xFF;
        byte regValue = OpcodeHelpers.readRegisterDirect(modRam, register);
        OpcodeHelpers.writeRegisterDirect(modRam, register, (byte) (value & 0xFF));
        return regValue;
    })),
    XCH_DR5         ("XCH",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R5"), ctx -> OpcodeHelpers.readModifyWriteAccumulatorWithArg(ctx.ram, (byte) (ctx.romByte & 0b00000111), (modRam, value, arg) ->
    {
        int register = arg & 0xFF;
        byte regValue = OpcodeHelpers.readRegisterDirect(modRam, register);
        OpcodeHelpers.writeRegisterDirect(modRam, register, (byte) (value & 0xFF));
        return regValue;
    })),
    XCH_DR6         ("XCH",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R6"), ctx -> OpcodeHelpers.readModifyWriteAccumulatorWithArg(ctx.ram, (byte) (ctx.romByte & 0b00000111), (modRam, value, arg) ->
    {
        int register = arg & 0xFF;
        byte regValue = OpcodeHelpers.readRegisterDirect(modRam, register);
        OpcodeHelpers.writeRegisterDirect(modRam, register, (byte) (value & 0xFF));
        return regValue;
    })),
    XCH_DR7         ("XCH",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R7"), ctx -> OpcodeHelpers.readModifyWriteAccumulatorWithArg(ctx.ram, (byte) (ctx.romByte & 0b00000111), (modRam, value, arg) ->
    {
        int register = arg & 0xFF;
        byte regValue = OpcodeHelpers.readRegisterDirect(modRam, register);
        OpcodeHelpers.writeRegisterDirect(modRam, register, (byte) (value & 0xFF));
        return regValue;
    })),
    //Irregular 0xD0-0xD3
    POP             ("POP",     1, 1, ParseHelpers.makeOneAddressArgParser(), ctx -> {
        int address = ctx.readRomAndIncrementPC() & 0xFF;
        ctx.ram.writeByte(address, ctx.popStack());
    }),
    ACALL_110       ("ACALL",   1, 1, ParseHelpers.makeOneLabelArgJumpParser(), ctx -> {
        int address = OpcodeHelpers.calculateAjmpAddress(ctx.romByte, ctx.readRomAndIncrementPC());
        ctx.pushStateBeforeCall();
        ctx.setProgramCounter(OpcodeHelpers.calculateAjmpTarget(ctx.programCounter, address));
    }),
    SETB_BIT        ("SETB",    1, 1, ParseHelpers.makeOneBitArgParser(false), ctx -> ctx.ram.writeBit(ctx.readRomAndIncrementPC() & 0xFF, BitWriteMode.SET)),
    SETB_C          ("SETB",    1, 0, ParseHelpers.makeOneConstArgParser("C"), 1, ctx -> ctx.ram.writeBit(Constants.BIT_ADDRESS_CARRY, BitWriteMode.SET)),
    //Regular 0xD4-0xDF
    DA              ("DA",      1, 0, ParseHelpers.makeOneConstArgParser("A"), ctx -> OpcodeHelpers.readModifyWriteAccumulator(ctx.ram, (modRam, value) ->
    {
        if ((value & 0x0F) > 9 || modRam.readBit(Constants.BIT_ADDRESS_AUX_CARRY))
        {
            value += 0x06;
            if (value > 255)
            {
                value -= 256;
                modRam.writeBit(Constants.BIT_ADDRESS_CARRY, BitWriteMode.SET);
            }
        }
        if (((value >> 4) & 0x0F) > 9 || modRam.readBit(Constants.BIT_ADDRESS_CARRY))
        {
            value += 0x60;
            if (value > 255)
            {
                value -= 256;
                modRam.writeBit(Constants.BIT_ADDRESS_CARRY, BitWriteMode.SET);
            }
        }
        return value;
    })),
    DJNZ_MEM        ("DJNZ",    2, 2, ParseHelpers.makeTwoArgJumpParser(ParseHelpers::parseAddressOperand), ctx -> {
        int address = ctx.readRomAndIncrementPC() & 0xFF;
        byte result = OpcodeHelpers.decrementJumpNotZero(this, ctx.ram.readByte(address), ctx.readRomAndIncrementPC());
        ctx.ram.writeByte(address, result);
    }),
    XCHD_IR0        ("XCHD",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "@R0"), ctx -> OpcodeHelpers.readModifyWriteAccumulatorWithArg(ram, (byte) (romByte & 0x1), (modRam, value, arg) ->
    {
        int register = arg & 0xFF;
        byte regValue = OpcodeHelpers.readRegisterIndirect(modRam, register);
        byte newRegValue = (byte) ((regValue & 0xF0) | (value & 0x0F));
        OpcodeHelpers.writeRegisterIndirect(modRam, register, newRegValue);
        return (value & 0xF0) | (regValue & 0x0F);
    })),
    XCHD_IR1        ("XCHD",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "@R1"), ctx -> OpcodeHelpers.readModifyWriteAccumulatorWithArg(ram, (byte) (romByte & 0x1), (modRam, value, arg) ->
    {
        int register = arg & 0xFF;
        byte regValue = OpcodeHelpers.readRegisterIndirect(modRam, register);
        byte newRegValue = (byte) ((regValue & 0xF0) | (value & 0x0F));
        OpcodeHelpers.writeRegisterIndirect(modRam, register, newRegValue);
        return (value & 0xF0) | (regValue & 0x0F);
    })),
    DJNZ_DR0        ("DJNZ",    2, 1, ParseHelpers.makeTwoArgJumpParser("R0"), ctx -> {
        int register = ctx.romByte & 0b00000111;
        byte value = OpcodeHelpers.readRegisterDirect(ctx.ram, register);
        byte result = OpcodeHelpers.decrementJumpNotZero(this, value, ctx.readRomAndIncrementPC());
        OpcodeHelpers.writeRegisterDirect(ctx.ram, register, result);
    }),
    DJNZ_DR1        ("DJNZ",    2, 1, ParseHelpers.makeTwoArgJumpParser("R1"), ctx -> {
        int register = ctx.romByte & 0b00000111;
        byte value = OpcodeHelpers.readRegisterDirect(ctx.ram, register);
        byte result = OpcodeHelpers.decrementJumpNotZero(this, value, ctx.readRomAndIncrementPC());
        OpcodeHelpers.writeRegisterDirect(ctx.ram, register, result);
    }),
    DJNZ_DR2        ("DJNZ",    2, 1, ParseHelpers.makeTwoArgJumpParser("R2"), ctx -> {
        int register = ctx.romByte & 0b00000111;
        byte value = OpcodeHelpers.readRegisterDirect(ctx.ram, register);
        byte result = OpcodeHelpers.decrementJumpNotZero(this, value, ctx.readRomAndIncrementPC());
        OpcodeHelpers.writeRegisterDirect(ctx.ram, register, result);
    }),
    DJNZ_DR3        ("DJNZ",    2, 1, ParseHelpers.makeTwoArgJumpParser("R3"), ctx -> {
        int register = ctx.romByte & 0b00000111;
        byte value = OpcodeHelpers.readRegisterDirect(ctx.ram, register);
        byte result = OpcodeHelpers.decrementJumpNotZero(this, value, ctx.readRomAndIncrementPC());
        OpcodeHelpers.writeRegisterDirect(ctx.ram, register, result);
    }),
    DJNZ_DR4        ("DJNZ",    2, 1, ParseHelpers.makeTwoArgJumpParser("R4"), ctx -> {
        int register = ctx.romByte & 0b00000111;
        byte value = OpcodeHelpers.readRegisterDirect(ctx.ram, register);
        byte result = OpcodeHelpers.decrementJumpNotZero(this, value, ctx.readRomAndIncrementPC());
        OpcodeHelpers.writeRegisterDirect(ctx.ram, register, result);
    }),
    DJNZ_DR5        ("DJNZ",    2, 1, ParseHelpers.makeTwoArgJumpParser("R5"), ctx -> {
        int register = ctx.romByte & 0b00000111;
        byte value = OpcodeHelpers.readRegisterDirect(ctx.ram, register);
        byte result = OpcodeHelpers.decrementJumpNotZero(this, value, ctx.readRomAndIncrementPC());
        OpcodeHelpers.writeRegisterDirect(ctx.ram, register, result);
    }),
    DJNZ_DR6        ("DJNZ",    2, 1, ParseHelpers.makeTwoArgJumpParser("R6"), ctx -> {
        int register = ctx.romByte & 0b00000111;
        byte value = OpcodeHelpers.readRegisterDirect(ctx.ram, register);
        byte result = OpcodeHelpers.decrementJumpNotZero(this, value, ctx.readRomAndIncrementPC());
        OpcodeHelpers.writeRegisterDirect(ctx.ram, register, result);
    }),
    DJNZ_DR7        ("DJNZ",    2, 1, ParseHelpers.makeTwoArgJumpParser("R7"), ctx -> {
        int register = ctx.romByte & 0b00000111;
        byte value = OpcodeHelpers.readRegisterDirect(ctx.ram, register);
        byte result = OpcodeHelpers.decrementJumpNotZero(this, value, ctx.readRomAndIncrementPC());
        OpcodeHelpers.writeRegisterDirect(ctx.ram, register, result);
    }),
    //Irregular 0xE0-0xE3
    MOVX_ACC_IDPTR  ("MOVX",    2, 0, ParseHelpers.makeTwoConstArgParser("A", "@DPTR"), ctx -> {
        int addr = OpcodeHelpers.readDataPointer(ctx.ram);
        addr %= Constants.EXT_RAM_SIZE;
        ctx.ram.writeByte(Constants.ADDRESS_ACCUMULATOR, ctx.extRam[addr]);
    }),
    AJMP_111        ("AJMP",    1, 1, ParseHelpers.makeOneLabelArgJumpParser(), ctx -> {
        int address = OpcodeHelpers.calculateAjmpAddress(ctx.romByte, ctx.readRomAndIncrementPC());
        ctx.setProgramCounter(OpcodeHelpers.calculateAjmpTarget(ctx.programCounter, address));
    }),
    MOVX_ACC_IR0    ("MOVX",    2, 1, ParseHelpers.makeTwoConstArgParser("A", "@R0"), ctx -> {
        int addr = OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000001);
        addr |= ctx.ram.read(Constants.ADDRESS_IO_PORT2) << 8;
        addr %= Constants.EXT_RAM_SIZE;
        ctx.ram.writeByte(Constants.ADDRESS_ACCUMULATOR, ctx.extRam[addr]);
    }),
    MOVX_ACC_IR1    ("MOVX",    2, 1, ParseHelpers.makeTwoConstArgParser("A", "@R1"), ctx -> {
        int addr = OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000001);
        addr |= ctx.ram.read(Constants.ADDRESS_IO_PORT2) << 8;
        addr %= Constants.EXT_RAM_SIZE;
        ctx.ram.writeByte(Constants.ADDRESS_ACCUMULATOR, ctx.extRam[addr]);
    }),
    //Regular 0xE4-0xEF
    CLR_ACC         ("CLR",     1, 0, ParseHelpers.makeOneConstArgParser("A"), ctx -> ctx.ram.write(Constants.ADDRESS_ACCUMULATOR, 0)),
    MOV_ACC_MEM     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("A", false), 2, ctx -> ctx.ram.writeByte(Constants.ADDRESS_ACCUMULATOR, ctx.ram.readByte(ctx.readRomAndIncrementPC() & 0xFF))),
    MOV_ACC_IR0     ("MOV",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "@R0"), ctx -> ctx.ram.writeByte(Constants.ADDRESS_ACCUMULATOR, OpcodeHelpers.readRegisterIndirect(ctx.ram, ctx.romByte & 0x1))),
    MOV_ACC_IR1     ("MOV",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "@R1"), ctx -> ctx.ram.writeByte(Constants.ADDRESS_ACCUMULATOR, OpcodeHelpers.readRegisterIndirect(ctx.ram, ctx.romByte & 0x1))),
    MOV_ACC_DR0     ("MOV",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R0"), 2, ctx -> ctx.ram.writeByte(Constants.ADDRESS_ACCUMULATOR, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    MOV_ACC_DR1     ("MOV",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R1"), 2, ctx -> ctx.ram.writeByte(Constants.ADDRESS_ACCUMULATOR, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    MOV_ACC_DR2     ("MOV",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R2"), 2, ctx -> ctx.ram.writeByte(Constants.ADDRESS_ACCUMULATOR, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    MOV_ACC_DR3     ("MOV",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R3"), 2, ctx -> ctx.ram.writeByte(Constants.ADDRESS_ACCUMULATOR, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    MOV_ACC_DR4     ("MOV",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R4"), 2, ctx -> ctx.ram.writeByte(Constants.ADDRESS_ACCUMULATOR, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    MOV_ACC_DR5     ("MOV",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R5"), 2, ctx -> ctx.ram.writeByte(Constants.ADDRESS_ACCUMULATOR, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    MOV_ACC_DR6     ("MOV",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R6"), 2, ctx -> ctx.ram.writeByte(Constants.ADDRESS_ACCUMULATOR, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    MOV_ACC_DR7     ("MOV",     2, 0, ParseHelpers.makeTwoConstArgParser("A", "R7"), 2, ctx -> ctx.ram.writeByte(Constants.ADDRESS_ACCUMULATOR, OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000111))),
    //Irregular 0xF0-0xF3
    MOVX_IDPTR_ACC  ("MOVX",    2, 0, ParseHelpers.makeTwoConstArgParser("@DPTR", "A"), ctx -> {
        int addr = OpcodeHelpers.readDataPointer(ctx.ram);
        addr %= Constants.EXT_RAM_SIZE;
        ctx.extRam[addr] = ctx.ram.readByte(Constants.ADDRESS_ACCUMULATOR);
    }),
    ACALL_111       ("ACALL",   1, 1, ParseHelpers.makeOneLabelArgJumpParser(), ctx -> {
        int address = OpcodeHelpers.calculateAjmpAddress(ctx.romByte, ctx.readRomAndIncrementPC());
        ctx.pushStateBeforeCall();
        ctx.setProgramCounter(OpcodeHelpers.calculateAjmpTarget(ctx.programCounter, address));
    }),
    MOVX_IR0_ACC    ("MOVX",    2, 1, ParseHelpers.makeTwoConstArgParser("@R0", "A"), ctx -> {
        int addr = OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000001);
        addr |= ctx.ram.read(Constants.ADDRESS_IO_PORT2) << 8;
        addr %= Constants.EXT_RAM_SIZE;
        ctx.extRam[addr] = ctx.ram.readByte(Constants.ADDRESS_ACCUMULATOR);
    }),
    MOVX_IR1_ACC    ("MOVX",    2, 1, ParseHelpers.makeTwoConstArgParser("@R1", "A"), ctx -> {
        int addr = OpcodeHelpers.readRegisterDirect(ctx.ram, ctx.romByte & 0b00000001);
        addr |= ctx.ram.read(Constants.ADDRESS_IO_PORT2) << 8;
        addr %= Constants.EXT_RAM_SIZE;
        ctx.extRam[addr] = ctx.ram.readByte(Constants.ADDRESS_ACCUMULATOR);
    }),
    //Regular 0xF4-0xFF
    CPL_ACC         ("CPL",     1, 0, ParseHelpers.makeOneConstArgParser("A"), ctx -> OpcodeHelpers.readModifyWriteAccumulator(ctx.ram, (modRam, value) -> ~value)),
    MOV_MEM_ACC     ("MOV",     2, 1, ParseHelpers.makeTwoArgOneConstOneAddressParser("A", true), 2, ctx -> ctx.ram.writeByte(ctx.readRomAndIncrementPC() & 0xFF, ctx.ram.readByte(Constants.ADDRESS_ACCUMULATOR))),
    MOV_IR0_ACC     ("MOV",     2, 0, ParseHelpers.makeTwoConstArgParser("@R0", "A"), ctx -> OpcodeHelpers.writeRegisterIndirect(ctx.ram, ctx.romByte & 0x1, ctx.ram.readByte(Constants.ADDRESS_ACCUMULATOR))),
    MOV_IR1_ACC     ("MOV",     2, 0, ParseHelpers.makeTwoConstArgParser("@R1", "A"), ctx -> OpcodeHelpers.writeRegisterIndirect(ctx.ram, ctx.romByte & 0x1, ctx.ram.readByte(Constants.ADDRESS_ACCUMULATOR))),
    MOV_DR0_ACC     ("MOV",     2, 0, ParseHelpers.makeTwoConstArgParser("R0", "A"), 2, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.ram.readByte(Constants.ADDRESS_ACCUMULATOR))),
    MOV_DR1_ACC     ("MOV",     2, 0, ParseHelpers.makeTwoConstArgParser("R1", "A"), 2, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.ram.readByte(Constants.ADDRESS_ACCUMULATOR))),
    MOV_DR2_ACC     ("MOV",     2, 0, ParseHelpers.makeTwoConstArgParser("R2", "A"), 2, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.ram.readByte(Constants.ADDRESS_ACCUMULATOR))),
    MOV_DR3_ACC     ("MOV",     2, 0, ParseHelpers.makeTwoConstArgParser("R3", "A"), 2, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.ram.readByte(Constants.ADDRESS_ACCUMULATOR))),
    MOV_DR4_ACC     ("MOV",     2, 0, ParseHelpers.makeTwoConstArgParser("R4", "A"), 2, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.ram.readByte(Constants.ADDRESS_ACCUMULATOR))),
    MOV_DR5_ACC     ("MOV",     2, 0, ParseHelpers.makeTwoConstArgParser("R5", "A"), 2, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.ram.readByte(Constants.ADDRESS_ACCUMULATOR))),
    MOV_DR6_ACC     ("MOV",     2, 0, ParseHelpers.makeTwoConstArgParser("R6", "A"), 2, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.ram.readByte(Constants.ADDRESS_ACCUMULATOR))),
    MOV_DR7_ACC     ("MOV",     2, 0, ParseHelpers.makeTwoConstArgParser("R7", "A"), 2, ctx -> OpcodeHelpers.writeRegisterDirect(ctx.ram, ctx.romByte & 0b00000111, ctx.ram.readByte(Constants.ADDRESS_ACCUMULATOR))),
    ;

    private static final I8051Opcode[] OPCODES = values();
    private static final Map<String, List<I8051Opcode>> OPCODES_BY_MNEMONIC = Util.make(new HashMap<>(), map ->
    {
        for (I8051Opcode opcode : OPCODES)
        {
            if (opcode == RESERVED) continue;
            map.computeIfAbsent(opcode.mnemonic.toLowerCase(Locale.ROOT), $ -> new ArrayList<>()).add(opcode);
        }
        map.values().forEach(list -> list.sort((a, b) ->
        {
            if (a.priority != b.priority)
            {
                return Integer.compare(b.priority, a.priority);
            }
            return Integer.compare(a.ordinal(), b.ordinal());
        }));
    });

    private final String mnemonic;
    // The amount of operands expected in mnemonic representation
    private final int operands;
    // The amount of program memory bytes consumed by operands in machine code representation
    private final int operandBytes;
    private final NodeParser parser;
    // Parser priority, opcodes with higher values are tried first
    private final int priority;
    // Execute the opcode.
    private final Consumer<Interpreter.InterpreterContext> func;

    I8051Opcode(String mnemonic, int operands, int operandBytes, NodeParser parser, Consumer<Interpreter.InterpreterContext> func)
    {
        this(mnemonic, operands, operandBytes, parser, 0, func);
    }

    I8051Opcode(String mnemonic, int operands, int operandBytes, NodeParser parser, int priority, Consumer<Interpreter.InterpreterContext> func)
    {
        this.mnemonic = mnemonic;
        this.operands = operands;
        this.operandBytes = operandBytes;
        this.parser = parser;
        this.priority = priority;
        this.func = func;
    }

    public String getMnemonic()
    {
        return mnemonic;
    }

    public int getOperands()
    {
        return operands;
    }

    public int getOperandBytes()
    {
        return operandBytes;
    }

    public byte toByte()
    {
        return (byte) ordinal();
    }


    public static Node parse(int line, String mnemonic, String[] operands)
    {
        List<I8051Opcode> opcodes = OPCODES_BY_MNEMONIC.get(mnemonic);
        if (opcodes == null)
        {
            return ErrorNode.unrecognizedOpcode(mnemonic, line);
        }

        for (I8051Opcode opcode : opcodes)
        {
            if (operands.length != opcode.getOperands()) continue;

            try
            {
                Node node = opcode.parser.parse(line, opcode, operands);
                if (node != null)
                {
                    return node;
                }
            }
            catch (Throwable t)
            {
                return ErrorNode.invalidOperand(opcode, operands, line);
            }
        }
        return ErrorNode.invalidOperand(opcodes.getFirst(), operands, line);
    }

    public I8051Opcode fromRomByte(byte romByte)
    {
        return OPCODES[romByte & 0xFF];
    }
}
