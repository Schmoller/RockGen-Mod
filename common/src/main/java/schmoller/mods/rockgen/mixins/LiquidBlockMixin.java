package schmoller.mods.rockgen.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import schmoller.mods.rockgen.api.CurrentPlatform;

@Mixin(LiquidBlock.class)
public abstract class LiquidBlockMixin {
    @Shadow
    @Final
    private FlowingFluid fluid;

    @Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
    private void onPlace(
        BlockState p_54754_, Level level, BlockPos position, BlockState p_54757_, boolean p_54758_, CallbackInfo context
    ) {
        // NOTE: This will take priority over the FluidInteractionRegistry. This is so we can override behaviours using recipes
        if (rockgen$tryInteract(level, position)) {
            context.cancel();
        }
    }

    private boolean rockgen$tryInteract(Level level, BlockPos position) {
        for (var recipe : CurrentPlatform
            .getInstance()
            .getFluidSpreadRecipeCache()
            .getRegular(this.fluid, level)) {
            var blockToSet = recipe.tryMatch(level, position);
            if (blockToSet.isPresent()) {
                var block = blockToSet.get();
                var newState = CurrentPlatform
                    .getInstance()
                    .getPlatformHandlers()
                    .notifyAndGetFluidInteractionBlock(level, position, block.defaultBlockState());

                level.setBlockAndUpdate(position, newState);
                this.fizz(level, position);
                return true;
            }
        }

        return false;
    }

    @Shadow
    private void fizz(LevelAccessor p_54701_, BlockPos p_54702_) {
    }

    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    private void neighborChanged(
        BlockState p_54709_, Level level, BlockPos position, Block p_54712_, BlockPos p_54713_, boolean p_54714_,
        CallbackInfo context
    ) {
        // NOTE: This will take priority over the FluidInteractionRegistry. This is so we can override behaviours using recipes
        if (rockgen$tryInteract(level, position)) {
            context.cancel();
        }
    }
}
