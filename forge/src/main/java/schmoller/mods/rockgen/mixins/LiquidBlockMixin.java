package schmoller.mods.rockgen.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import schmoller.mods.rockgen.RockGenerationMod;

@Mixin(LiquidBlock.class)
public abstract class LiquidBlockMixin {
    private boolean rockgen$tryInteract(Level level, BlockPos position) {
        for (var recipe : RockGenerationMod.FluidSpreadRecipeCache.getRegular(this.getFluid(), level)) {
            var blockToSet = recipe.tryMatch(level, position);
            if (blockToSet.isPresent()) {
                var block = blockToSet.get();
                level.setBlockAndUpdate(position,
                    net.minecraftforge.event.ForgeEventFactory.fireFluidPlaceBlockEvent(level,
                        position,
                        position,
                        block.defaultBlockState()
                    )
                );
                this.fizz(level, position);
                return true;
            }
        }

        return false;
    }

    @Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
    private void onPlace(
        BlockState p_54754_, Level level, BlockPos position, BlockState p_54757_, boolean p_54758_, CallbackInfo context
    ) {
        // NOTE: This will take priority over the FluidInteractionRegistry. This is so we can override behaviours using recipes
        if (rockgen$tryInteract(level, position)) {
            context.cancel();
        }
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

    @Shadow(remap = false) // Forge method
    public abstract FlowingFluid getFluid();

    @Shadow
    private void fizz(LevelAccessor p_54701_, BlockPos p_54702_) {
    }
}
