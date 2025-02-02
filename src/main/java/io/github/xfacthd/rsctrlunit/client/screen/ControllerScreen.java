package io.github.xfacthd.rsctrlunit.client.screen;

import io.github.xfacthd.rsctrlunit.client.screen.cpu.CPUScreen;
import io.github.xfacthd.rsctrlunit.client.screen.widget.*;
import io.github.xfacthd.rsctrlunit.common.menu.ControllerMenu;
import io.github.xfacthd.rsctrlunit.common.net.payload.serverbound.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class ControllerScreen extends CPUScreen
{
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/demo_background.png");
    private static final ResourceLocation SLOT_BACKGROUND = ResourceLocation.withDefaultNamespace("container/slot");
    private static final int IMAGE_WIDTH = 360;
    private static final int IMAGE_HEIGHT = 221;
    private static final int TAB_HEIGHT = 22;
    private static final int TAB_EDGE_HEIGHT = 4;
    private static final int BACKGROUND_Y = TAB_HEIGHT - TAB_EDGE_HEIGHT;
    private static final int DISASSEMBLY_X = IMAGE_WIDTH / 2 + 7;
    private static final int INVENTORY_WIDTH = 162;
    private static final int INVENTORY_X = DISASSEMBLY_X / 2 - INVENTORY_WIDTH / 2 + 1;
    private static final int CARD_SLOT_Y = TAB_HEIGHT + 55;
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int INSERT_BUTTON_Y = TAB_HEIGHT + 78;
    private static final int BUTTON_X = INVENTORY_X + INVENTORY_WIDTH - BUTTON_WIDTH;
    public static final Component TITLE_NO_PROCESSOR = Component.translatable("title.rsctrlunit.controller.no_processor");
    public static final Component BUTTON_INSERT_PROCESSOR = Component.translatable("button.rsctrlunit.controller.insert_processor");

    private Button buttonInsert;

    public ControllerScreen(ControllerMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
        imageWidth = IMAGE_WIDTH;
        imageHeight = IMAGE_HEIGHT;
    }

    @Override
    protected void init()
    {
        super.init();

        buttonInsert = addRenderableWidget(ActionButton.builder(BUTTON_INSERT_PROCESSOR)
                .pos(leftPos + BUTTON_X, topPos + INSERT_BUTTON_Y)
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .build(this, ServerboundControllerActionPayload.Action.LOAD_ROM)
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.blitSprite(SLOT_BACKGROUND, leftPos + INVENTORY_X, topPos + CARD_SLOT_Y, SLOT_SIZE, SLOT_SIZE);
        Slot slot = menu.slots.getFirst();

        if (!slot.hasItem()) {
            buttonInsert.active = false;
        }

        buttonInsert.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        graphics.blitWithBorder(BACKGROUND, leftPos, topPos + BACKGROUND_Y, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT - BACKGROUND_Y, 248, 166, 4, 4, 4, 4);
        graphics.drawString(font, TITLE_NO_PROCESSOR, IMAGE_WIDTH / 2 - 20, topPos + 25, 0xFF404040, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {

    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY)
    {
        super.renderTooltip(graphics, mouseX, mouseY);

    }

    @Override
    protected void containerTick()
    {
        boolean running = menu.isRunning();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int btn)
    {

        return super.mouseClicked(mouseX, mouseY, btn);
    }


    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static int scroll(int offset, double scrollOffset, int codeSize, int frameSize)
    {
        return (int) Mth.clamp(offset - scrollOffset * 2, 0, Math.max(codeSize - (frameSize - 4), 0));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

}
