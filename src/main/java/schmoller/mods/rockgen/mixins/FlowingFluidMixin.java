package schmoller.mods.rockgen.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import schmoller.mods.rockgen.RockGenerationMod;

@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin extends Fluid {
    @Inject(method = "spreadTo", at = @At("HEAD"), cancellable = true)
    private void handleSpreadRecipe(
        @NotNull LevelAccessor level, @NotNull BlockPos flowingToPosition, @NotNull BlockState p_76222_,
        @NotNull Direction spreadDirection, @NotNull FluidState p_76224_, CallbackInfo context
    ) {
        if (spreadDirection != Direction.DOWN) {
            return;
        }

        for (var recipe : RockGenerationMod.FluidSpreadRecipeCache.getFalling((FlowingFluid) (Object) this, level)) {
            var blockToSet = recipe.tryMatch(level, flowingToPosition);

            if (blockToSet.isEmpty()) {
                continue;
            }

            if (p_76222_.getBlock() instanceof LiquidBlock) {
                var initialState = blockToSet.get().defaultBlockState();
                var newState = net.minecraftforge.event.ForgeEventFactory.fireFluidPlaceBlockEvent(level,
                    flowingToPosition,
                    flowingToPosition,
                    initialState
                );

                if (!recipe.getAllowOverriding()) {
                    newState = initialState;
                }

                level.setBlock(flowingToPosition, newState, 3);
            }

            this.fizz(level, flowingToPosition);
            context.cancel();
            return;
        }
    }

    private void fizz(LevelAccessor level, BlockPos position) {
        level.levelEvent(1501, position, 0);
    }
}
