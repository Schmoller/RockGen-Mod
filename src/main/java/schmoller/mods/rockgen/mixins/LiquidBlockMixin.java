package schmoller.mods.rockgen.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = LiquidBlock.class, remap = false)
public abstract class LiquidBlockMixin {
    @Overwrite
    private boolean shouldSpreadLiquid(Level level, BlockPos flowingToPosition, BlockState p_54699_) {
        System.out.println("Are we here???");
        if (this.getFluid().is(FluidTags.LAVA)) {
            boolean flag = level.getBlockState(flowingToPosition.below()).is(Blocks.SOUL_SOIL);

            for(Direction direction : LiquidBlock.POSSIBLE_FLOW_DIRECTIONS) {
                BlockPos blockpos = flowingToPosition.relative(direction.getOpposite());
                if (level.getFluidState(blockpos).is(FluidTags.WATER)) {
                    System.out.println("Here 1");
                    Block block = level.getFluidState(flowingToPosition).isSource() ? Blocks.OBSIDIAN : Blocks.COBBLESTONE;
                    level.setBlockAndUpdate(flowingToPosition, net.minecraftforge.event.ForgeEventFactory.fireFluidPlaceBlockEvent(level, flowingToPosition, flowingToPosition, block.defaultBlockState()));
                    this.fizz(level, flowingToPosition);
                    return false;
                }

                if (flag && level.getBlockState(blockpos).is(Blocks.BLUE_ICE)) {
                    System.out.println("Here 2");
                    level.setBlockAndUpdate(flowingToPosition, net.minecraftforge.event.ForgeEventFactory.fireFluidPlaceBlockEvent(level, flowingToPosition, flowingToPosition, Blocks.BASALT.defaultBlockState()));
                    this.fizz(level, flowingToPosition);
                    return false;
                }
            }
        }

        return true;
    }

    @Shadow
    public abstract FlowingFluid getFluid();

    @Shadow
    private void fizz(LevelAccessor p_54701_, BlockPos p_54702_) {}


}
