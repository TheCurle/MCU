package io.github.xfacthd.rsctrlunit.common.datagen.provider;

import io.github.xfacthd.rsctrlunit.RedstoneControllerUnit;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class RCUItemModelProvider extends ItemModelProvider
{
    public RCUItemModelProvider(PackOutput output, ExistingFileHelper fileHelper)
    {
        super(output, RedstoneControllerUnit.MOD_ID, fileHelper);
    }

    @Override
    protected void registerModels()
    {
        singleTexture("memory_card", mcLoc("item/generated"), "layer0", modLoc("item/memory_card"));
        singleTexture("programmer", mcLoc("item/generated"), "layer0", modLoc("item/programmer"));
        singleTexture("processor8051", mcLoc("item/generated"), "layer0", modLoc("item/8051"));
        singleTexture("processor8080", mcLoc("item/generated"), "layer0", modLoc("item/8080"));
        singleTexture("processor8085", mcLoc("item/generated"), "layer0", modLoc("item/8085"));
        singleTexture("processorz80", mcLoc("item/generated"), "layer0", modLoc("item/z80"));
    }
}
