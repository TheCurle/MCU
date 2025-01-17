package io.github.xfacthd.rsctrlunit.common.emulator.interpreter;

import io.github.xfacthd.rsctrlunit.common.emulator.core.CPUCore;
import io.github.xfacthd.rsctrlunit.common.emulator.core.i8051.I8051Opcode;
import io.github.xfacthd.rsctrlunit.common.emulator.opcode.Opcode;
import io.github.xfacthd.rsctrlunit.common.emulator.opcode.OpcodeHelpers;
import io.github.xfacthd.rsctrlunit.common.emulator.util.*;
import io.github.xfacthd.rsctrlunit.common.util.Utils;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.*;

/**
 * Handles, manages and executes instructions for a single CPU core.
 * Execution can be paused and stepped, and coordinated with an external control system.
 */
public final class Interpreter {

    /**
     * Holds all of the data that the interpreter uses.
     * This is:
     *  * the type of CPU Core being executed
     *  * the registers
     *  * the ROM being executed
     *  * the program RAM
     *  * the external RAM
     *  * I/O port handlers
     *  * system timers
     *  * interrupt vectors & control units
     * These are outside of the interpreter so that we can pass them around to Opcode functions very simply.
     */
    public static class InterpreterContext {
        public final CPUCore core;
        public final ReentrantLock lock;
        public final byte[] rom;
        public final IOPorts ioPorts;
        public final RAM ram;
        public final Timers timers;
        public final Interrupts interrupts;
        public final byte[] extRam;

        public InterpreterContext(CPUCore core, int romSize, int ramSize, int sfrSize, int extRAMSize) {
            this.core = core;
            this.lock = new ReentrantLock();
            this.rom = new byte[romSize];
            this.ioPorts = new IOPorts();
            this.ram = new RAM(ioPorts, ramSize, sfrSize);
            this.timers = new Timers(ram, ioPorts);
            this.interrupts = new Interrupts(ram);
            this.extRam = new byte[extRAMSize];
        }

        public Code code = Code.EMPTY;
        public int programCounter = 0;

        public byte romByte;

        public byte readRomAndIncrementPC()
        {
            byte data = rom[programCounter];
            setProgramCounter(programCounter + 1);
            return data;
        }

        public void pushStateBeforeCall()
        {
            pushStack((byte) (programCounter & 0xFF));
            pushStack((byte) (programCounter >> 8 & 0xFF));
        }

        public void pushStack(byte data)
        {
            int pointer = ram.read(Constants.ADDRESS_STACK_POINTER);
            pointer++;
            ram.write(Constants.ADDRESS_STACK_POINTER, pointer);
            ram.writeByte(pointer, data);
        }

        public byte popStack()
        {
            int pointer = ram.read(Constants.ADDRESS_STACK_POINTER);
            byte data = ram.readByte(pointer);
            ram.write(Constants.ADDRESS_STACK_POINTER, pointer - 1);
            return data;
        }

        @VisibleForTesting
        public void setProgramCounter(int pc)
        {
            programCounter = Math.floorMod(pc, Constants.ROM_SIZE);
        }

        public int getProgramCounter()
        {
            return programCounter;
        }

        public void reset(boolean clearRom)
        {
            programCounter = Constants.INITIAL_PROGRAM_COUNTER;
            ram.reset();
            Arrays.fill(extRam, (byte) 0);
            if (clearRom)
            {
                Arrays.fill(rom, (byte) 0);
            }
        }
    }

    private final InterpreterContext context;
    private volatile boolean running = false;
    private volatile boolean paused = false;
    private volatile boolean stepRequested = false;

    public Interpreter(InterpreterContext context) {
        this.context = context;
    }

    public InterpreterContext getContext() {
        return context;
    }

    public void run()
    {
        context.lock.lock();
        try
        {
            runInternal();
        }
        finally
        {
            context.lock.unlock();
        }
    }

    private void runInternal()
    {
        context.timers.run();
        context.ioPorts.run(context.ram);
        int isrAddress = context.interrupts.run();
        if (isrAddress != -1)
        {
            context.pushStateBeforeCall();
            context.setProgramCounter(isrAddress);
        }

        context.romByte = context.readRomAndIncrementPC();
        Opcode opcode = context.core.opcodeFunc.apply(context.romByte);
        opcode.getOpcodeFunc().accept(context);
    }

    public void loadCode(Code code)
    {
        context.reset(true);
        this.context.code = code;
        System.arraycopy(code.rom(), 0, this.context.rom, 0, Math.min(code.rom().length, Constants.ROM_SIZE));
    }

    public Code getCode()
    {
        return context.code;
    }

    public byte[] getRam()
    {
        return context.ram.getRamArray();
    }

    public byte[] getSfr()
    {
        return context.ram.getSfrArray();
    }

    @VisibleForTesting
    public byte[] getExtRam()
    {
        return context.extRam;
    }

    public IOPorts getIoPorts()
    {
        return context.ioPorts;
    }

    public Timers getTimers()
    {
        return context.timers;
    }

    boolean isRunning()
    {
        return running;
    }

    public boolean isPaused()
    {
        return paused;
    }

    boolean isStepRequested()
    {
        boolean step = stepRequested;
        stepRequested = false;
        return step;
    }

    public void pause()
    {
        paused = true;
    }

    public void resume()
    {
        paused = false;
    }

    public void step()
    {
        stepRequested = true;
    }

    public void startup()
    {
        running = true;
    }

    public void shutdown()
    {
        running = false;
    }

    public <T> void writeLockGuarded(T data, BiConsumer<Interpreter, T> operation)
    {
        context.lock.lock();
        try
        {
            operation.accept(this, data);
        }
        finally
        {
            context.lock.unlock();
        }
    }

    public <R> R readLockGuarded(Function<Interpreter, R> operation)
    {
        context.lock.lock();
        try
        {
            return operation.apply(this);
        }
        finally
        {
            context.lock.unlock();
        }
    }

    public void load(CompoundTag tag)
    {
        context.code = Utils.fromNbt(Code.CODEC, tag.getCompound("code"), Code.EMPTY);
        Utils.copyByteArray(context.code.rom(), context.rom);
        Utils.copyByteArray(tag.getByteArray("ram"), context.ram.getRamArray());
        Utils.copyByteArray(tag.getByteArray("sfr"), context.ram.getSfrArray());
        context.ioPorts.load(tag.getCompound("io"));
        context.timers.load(tag.getCompound("timers"));
        context.interrupts.load(tag.getCompound("interrupts"));
        Utils.copyByteArray(tag.getByteArray("external_ram"), context.extRam);
        context.programCounter = tag.getInt("program_counter");
        paused = tag.getBoolean("paused");
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.put("code", Utils.toNbt(Code.CODEC, context.code));
        tag.putByteArray("ram", Arrays.copyOf(context.ram.getRamArray(), context.ram.getRamArray().length));
        tag.putByteArray("sfr", Arrays.copyOf(context.ram.getSfrArray(), context.ram.getSfrArray().length));
        tag.put("io", context.ioPorts.save());
        tag.put("timers", context.timers.save());
        tag.put("interrupts", context.interrupts.save());
        tag.putByteArray("external_ram", Arrays.copyOf(context.extRam, context.extRam.length));
        tag.putInt("program_counter", context.programCounter);
        tag.putBoolean("paused", paused);
        return tag;
    }
}
