package schmoller.mods.rockgen;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import schmoller.mods.rockgen.api.CurrentPlatform;
import schmoller.mods.rockgen.api.PlatformHandlers;
import schmoller.mods.rockgen.recipes.DripstoneDrainRecipe;
import schmoller.mods.rockgen.recipes.FluidSpreadRecipe;

public class RockGenerationMod implements ModInitializer, PlatformHandlers {
    public static final String Id = "rockgen";

    @Override
    public void onInitialize() {
        CurrentPlatform.getInstance().setPlatformHandlers(this);

        registerRecipeTypes();
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new RecipeReloadListener());
    }

    private void registerRecipeTypes() {
        Registry.register(
            Registry.RECIPE_SERIALIZER,
            new ResourceLocation(Id, FluidSpreadRecipe.TypeId),
            FluidSpreadRecipe.SerializerInstance
        );
        Registry.register(
            Registry.RECIPE_TYPE,
            new ResourceLocation(Id, FluidSpreadRecipe.TypeId),
            FluidSpreadRecipe.Type
        );

        Registry.register(
            Registry.RECIPE_SERIALIZER,
            new ResourceLocation(Id, DripstoneDrainRecipe.TypeId),
            DripstoneDrainRecipe.SerializerInstance
        );
        Registry.register(
            Registry.RECIPE_TYPE,
            new ResourceLocation(Id, DripstoneDrainRecipe.TypeId),
            DripstoneDrainRecipe.Type
        );
    }

    @Override
    public BlockState notifyAndGetFluidInteractionBlock(
        LevelAccessor level, BlockPos position, BlockState state
    ) {
        // Fabric has no event for this
        return state;
    }

    @Override
    public TagKey<Block> getOrCreateBlockTag(
        ResourceLocation location
    ) {
        return TagKey.create(Registry.BLOCK_REGISTRY, location);
    }

    @Override
    public TagKey<Fluid> getOrCreateFluidTag(
        ResourceLocation location
    ) {
        return TagKey.create(Registry.FLUID_REGISTRY, location);
    }
}
