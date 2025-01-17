package io.github.xfacthd.rsctrlunit.common.block;

import io.github.xfacthd.rsctrlunit.common.RCUContent;
import io.github.xfacthd.rsctrlunit.common.blockentity.ControllerBlockEntity;
import io.github.xfacthd.rsctrlunit.common.emulator.util.Code;
import io.github.xfacthd.rsctrlunit.common.item.ProgrammerItem;
import io.github.xfacthd.rsctrlunit.common.menu.ControllerMenu;
import io.github.xfacthd.rsctrlunit.common.redstone.RedstoneInterface;
import io.github.xfacthd.rsctrlunit.common.redstone.port.PortMapping;
import io.github.xfacthd.rsctrlunit.common.util.Utils;
import io.github.xfacthd.rsctrlunit.common.util.property.PropertyHolder;
import io.github.xfacthd.rsctrlunit.common.util.property.RedstoneType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class ControllerBlock extends Block implements EntityBlock
{
    private static final VoxelShape[] SHAPES = makeShapes(2D);
    // Make the collision shape slightly higher to avoid playing step sound and particles of the block below
    private static final VoxelShape[] COLLISION_SHAPES = makeShapes(3.3D);

    public ControllerBlock()
    {
        super(Properties.of().strength(1.5F, 6.0F));
        registerDefaultState(defaultBlockState().setValue(PropertyHolder.SHOW_PORT_MAPPING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(BlockStateProperties.FACING, PropertyHolder.SHOW_PORT_MAPPING);
        builder.add(PropertyHolder.RS_CON_PROPS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx)
    {
        return defaultBlockState().setValue(BlockStateProperties.FACING, ctx.getClickedFace().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack)
    {
        if (stack.has(DataComponents.BLOCK_ENTITY_DATA) && level.getBlockEntity(pos) instanceof ControllerBlockEntity be)
        {
            BlockState newState = be.getRedstoneInterface().updateStateFromConfigs(state);
            if (newState != state)
            {
                level.setBlockAndUpdate(pos, newState);
            }
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx)
    {
        return SHAPES[state.getValue(BlockStateProperties.FACING).ordinal()];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx)
    {
        return COLLISION_SHAPES[state.getValue(BlockStateProperties.FACING).ordinal()];
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (stack.is(RCUContent.ITEM_PROGRAMMER) && level.getBlockEntity(pos) instanceof ControllerBlockEntity controller)
        {
            if (!level.isClientSide())
            {
                ProgrammerItem.openMenu(player, stack, controller);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
    {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ControllerBlockEntity be)
        {
            Direction facing = state.getValue(BlockStateProperties.FACING);
            player.openMenu(new MenuProvider()
            {
                @Override
                public Component getDisplayName()
                {
                    return ControllerBlockEntity.TITLE;
                }

                @Override
                public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player)
                {
                    return ControllerMenu.createServer(windowId, be, (ServerPlayer) player, facing);
                }
            }, buf ->
            {
                BlockPos.STREAM_CODEC.encode(buf, pos);
                Code.STREAM_CODEC.encode(buf, be.getInterpreter().getCode());
                Direction.STREAM_CODEC.encode(buf, facing);
                RedstoneType.PORT_ARRAY_STREAM_CODEC.encode(buf, be.getRedstoneInterface().getPortConfigs());
                RedstoneInterface.PORT_MAPPING_STREAM_CODEC.encode(buf, be.getRedstoneInterface().getPortMapping());
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction dir)
    {
        Direction facing = state.getValue(BlockStateProperties.FACING);
        if (dir != null && dir.getAxis() != facing.getAxis())
        {
            int port = PortMapping.getPortIndex(facing, dir.getOpposite());
            return state.getValue(PropertyHolder.RS_CON_PROPS[port]) == RedstoneType.SINGLE;
        }
        return false;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir)
    {
        return getSignal(state, level, pos, dir);
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir)
    {
        Direction facing = state.getValue(BlockStateProperties.FACING);
        Direction side = dir.getOpposite(); // The given direction is from the wire's view
        int port = PortMapping.getPortIndex(facing, side);
        if (port != -1 && state.getValue(PropertyHolder.RS_CON_PROPS[port]) == RedstoneType.SINGLE)
        {
            if (level.getBlockEntity(pos) instanceof ControllerBlockEntity be)
            {
                return be.getRedstoneOutput(side);
            }
        }
        return 0;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block adjBlock, BlockPos adjPos, boolean moved)
    {
        onNeighborChange(state, level, pos, adjPos);
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos adjPos)
    {
        if (level.isClientSide()) return;

        Direction facing = state.getValue(BlockStateProperties.FACING);
        Direction side = Utils.getDirection(pos, adjPos);
        if (side.getAxis() != facing.getAxis() && level.getBlockEntity(pos) instanceof ControllerBlockEntity be)
        {
            be.handleNeighborUpdate(adjPos, side);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new ControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        if (!level.isClientSide())
        {
            return Utils.createBlockEntityTicker(type, RCUContent.BE_TYPE_CONTROLLER.get(), ControllerBlockEntity::tick);
        }
        return null;
    }



    @SuppressWarnings("SuspiciousNameCombination")
    private static VoxelShape[] makeShapes(double height)
    {
        double inv = 16 - height;
        VoxelShape[] shapes = new VoxelShape[6];
        shapes[Direction.UP.ordinal()] =    box(  0, inv,   0,     16,     16,     16);
        shapes[Direction.DOWN.ordinal()] =  box(  0,   0,   0,     16, height,     16);
        shapes[Direction.NORTH.ordinal()] = box(  0,   0,   0,     16,     16, height);
        shapes[Direction.SOUTH.ordinal()] = box(  0,   0, inv,     16,     16,     16);
        shapes[Direction.WEST.ordinal()] =  box(  0,   0,   0, height,     16,     16);
        shapes[Direction.EAST.ordinal()] =  box(inv,   0,   0,     16,     16,     16);
        return shapes;
    }
}
