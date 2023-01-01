package schmoller.mods.rockgen.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public interface PlatformHandlers {
    /**
     * Notify of a fluid creating a block. This also allows for the block to be overridden.
     * @param level A LevelAccessor to access the level info
     * @param position The location of the block being changed
     * @param state The new block state to set
     * @return A block state to use instead of `state`
     */
    BlockState notifyAndGetFluidInteractionBlock(LevelAccessor level, BlockPos position, BlockState state);

    TagKey<Block> getOrCreateBlockTag(ResourceLocation location);
    TagKey<Fluid> getOrCreateFluidTag(ResourceLocation location);
}
