package io.github.xfacthd.rsctrlunit.common;

import io.github.xfacthd.rsctrlunit.RedstoneControllerUnit;
import io.github.xfacthd.rsctrlunit.common.block.ControllerBlock;
import io.github.xfacthd.rsctrlunit.common.blockentity.ControllerBlockEntity;
import io.github.xfacthd.rsctrlunit.common.emulator.util.Code;
import io.github.xfacthd.rsctrlunit.common.item.MemoryCardItem;
import io.github.xfacthd.rsctrlunit.common.item.ProgrammerItem;
import io.github.xfacthd.rsctrlunit.common.menu.ControllerMenu;
import io.github.xfacthd.rsctrlunit.common.menu.ProgrammerMenu;
import io.github.xfacthd.rsctrlunit.common.util.registration.DeferredBlockEntity;
import io.github.xfacthd.rsctrlunit.common.util.registration.DeferredBlockEntityRegister;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class RCUContent
{
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RedstoneControllerUnit.MOD_ID);
    private static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(RedstoneControllerUnit.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RedstoneControllerUnit.MOD_ID);
    private static final DeferredBlockEntityRegister BLOCK_ENTITIES = DeferredBlockEntityRegister.create(RedstoneControllerUnit.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, RedstoneControllerUnit.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RedstoneControllerUnit.MOD_ID);

    // region Blocks
    public static final Holder<Block> BLOCK_CONTROLLER = registerBlock("controller", ControllerBlock::new);
    // endregion

    // region DataComponents
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Code>> COMPONENT_TYPE_CODE = DATA_COMPONENTS.registerComponentType(
            "code", builder -> builder.persistent(Code.CODEC).networkSynchronized(Code.STREAM_CODEC).cacheEncoding()
    );
    // endregion

    // region Items
    public static final Holder<Item> ITEM_MEMORY_CARD = ITEMS.registerItem("memory_card", MemoryCardItem::new);
    public static final Holder<Item> ITEM_PROGRAMMER = ITEMS.registerItem("programmer", ProgrammerItem::new);
    public static final Holder<Item> ITEM_PROCESSOR_8051 = ITEMS.registerItem("processor8051", Item::new);
    public static final Holder<Item> ITEM_PROCESSOR_8080 = ITEMS.registerItem("processor8080", Item::new);
    public static final Holder<Item> ITEM_PROCESSOR_8085 = ITEMS.registerItem("processor8085", Item::new);
    public static final Holder<Item> ITEM_PROCESSOR_z80 = ITEMS.registerItem("processorz80", Item::new);
    // endregion

    // region BlockEntities
    public static final DeferredBlockEntity<ControllerBlockEntity> BE_TYPE_CONTROLLER = BLOCK_ENTITIES.registerBlockEntity(
            "controller", ControllerBlockEntity::new, () -> new Block[] { BLOCK_CONTROLLER.value() }
    );
    // endregion

    // region MenuTypes
    public static final DeferredHolder<MenuType<?>, MenuType<ControllerMenu>> MENU_TYPE_CONTROLLER = MENU_TYPES.register(
            "controller", () -> IMenuTypeExtension.create(ControllerMenu::createClient)
    );
    public static final DeferredHolder<MenuType<?>, MenuType<ProgrammerMenu>> MENU_TYPE_PROGRAMMER = MENU_TYPES.register(
            "programmer", () -> IMenuTypeExtension.create(ProgrammerMenu::createClient)
    );
    // endregion

    // region CreativeModeTabs
    public static final Holder<CreativeModeTab> CREATIVE_TAB = CREATIVE_TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.rsctrlunit"))
                    .icon(() -> BLOCK_CONTROLLER.value().asItem().getDefaultInstance())
                    .displayItems((params, output) ->
                    {
                        output.accept(BLOCK_CONTROLLER.value());
                        output.accept(ITEM_MEMORY_CARD.value());
                        output.accept(ITEM_PROGRAMMER.value());
                        output.accept(ITEM_PROCESSOR_8051.value());
                        output.accept(ITEM_PROCESSOR_8080.value());
                        output.accept(ITEM_PROCESSOR_8085.value());
                        output.accept(ITEM_PROCESSOR_z80.value());
                    })
                    .build()
    );
    // endregion

    private static Holder<Block> registerBlock(String name, Supplier<Block> blockFactory)
    {
        Holder<Block> block = BLOCKS.register(name, blockFactory);
        ITEMS.register(name, () -> new BlockItem(block.value(), new Item.Properties()));
        return block;
    }

    public static void init(IEventBus modBus)
    {
        BLOCKS.register(modBus);
        DATA_COMPONENTS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENU_TYPES.register(modBus);
        CREATIVE_TABS.register(modBus);
    }

    private RCUContent() { }
}
