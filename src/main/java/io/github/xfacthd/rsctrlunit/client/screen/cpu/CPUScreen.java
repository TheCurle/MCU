package io.github.xfacthd.rsctrlunit.client.screen.cpu;

import io.github.xfacthd.rsctrlunit.client.screen.CardInventoryContainerScreen;
import io.github.xfacthd.rsctrlunit.common.menu.ControllerMenu;
import io.github.xfacthd.rsctrlunit.common.net.payload.serverbound.ServerboundSetPortConfigPayload;
import io.github.xfacthd.rsctrlunit.common.redstone.port.PortConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public abstract class CPUScreen extends CardInventoryContainerScreen<ControllerMenu> {

    public CPUScreen(ControllerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    public void setPortConfig(int port, PortConfig config) {
        PacketDistributor.sendToServer(new ServerboundSetPortConfigPayload(menu.containerId, port, config));
    }
}
