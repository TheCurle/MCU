package io.github.xfacthd.rsctrlunit.client.model;

import io.github.xfacthd.rsctrlunit.common.util.property.PropertyHolder;
import io.github.xfacthd.rsctrlunit.common.util.property.RedstoneType;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ControllerModel extends BakedModelWrapper<BakedModel>
{
    private final BakedModel[] singleModels;
    private final BakedModel[] bundledModels;

    ControllerModel(BakedModel baseModel, BakedModel[] singleModels, BakedModel[] bundledModels)
    {
        super(baseModel);
        this.singleModels = singleModels;
        this.bundledModels = bundledModels;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType)
    {
        List<BakedQuad> quads = super.getQuads(state, side, rand, extraData, renderType);
        if (state != null)
        {
            boolean copied = false;
            for (int i = 0; i < 4; i++)
            {
                RedstoneType type = state.getValue(PropertyHolder.RS_CON_PROPS[i]);
                if (type == RedstoneType.NONE) continue;

                if (!copied)
                {
                    quads = new ArrayList<>(quads);
                    copied = true;
                }

                BakedModel model = type == RedstoneType.SINGLE ? singleModels[i] : bundledModels[i];
                quads.addAll(model.getQuads(state, side, rand, extraData, renderType));
            }
        }
        return quads;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand)
    {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }
}
