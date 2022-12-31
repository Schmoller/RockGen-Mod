package schmoller.mods.rockgen.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import schmoller.mods.rockgen.RockGenerationMod;

@Mixin(LiquidBlock.class)
public abstract class LiquidBlockMixin {
    /**
     * @author Steven Schmoll
     * @reason Allows us to override vanilla cobblestone, basalt, and obsidian generation
     * with a recipe based system.
     */
    @Overwrite
    private boolean shouldSpreadLiquid(Level level, BlockPos flowingToPosition, BlockState p_54699_) {
        for (var recipe : RockGenerationMod.FluidSpreadRecipeCache.getRegular(this.getFluid(), level)) {
            var blockToSet = recipe.tryMatch(level, flowingToPosition);
            if (blockToSet.isPresent()) {
                var block = blockToSet.get();

                var initialState = block.defaultBlockState();
                var newState = net.minecraftforge.event.ForgeEventFactory.fireFluidPlaceBlockEvent(level,
                    flowingToPosition,
                    flowingToPosition,
                    initialState
                );

                if (!recipe.getAllowOverriding()) {
                    newState = initialState;
                }

                level.setBlockAndUpdate(flowingToPosition, newState);
                this.fizz(level, flowingToPosition);
                return false;
            }
        }

        return true;
    }

    @Shadow(remap = false) // Forge method
    public abstract FlowingFluid getFluid();

    @Shadow
    private void fizz(LevelAccessor p_54701_, BlockPos p_54702_) {
    }
}
