package io.github.xfacthd.rsctrlunit.common.emulator.disassembler;

import io.github.xfacthd.rsctrlunit.common.emulator.util.Code;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Locale;

public abstract class Disassembler {
    @VisibleForTesting
    public static final String LABEL_LINE_TEMPLATE = "        %s:";
    @VisibleForTesting
    public static final String CODE_LINE_TEMPLATE = "%04X | %s";

    protected Disassembler() { }

    public abstract Disassembly disassemble(@Nullable Code code);

}
