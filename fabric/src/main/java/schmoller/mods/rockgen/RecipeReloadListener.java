package schmoller.mods.rockgen;

import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import schmoller.mods.rockgen.api.CurrentPlatform;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class RecipeReloadListener implements SimpleResourceReloadListener<Void> {
    @Override
    public CompletableFuture<Void> load(
        ResourceManager manager, ProfilerFiller profiler, Executor executor
    ) {
        CurrentPlatform.getInstance().getFluidSpreadRecipeCache().invalidate();
        CurrentPlatform.getInstance().getDripstoneDrainRecipeCache().invalidate();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> apply(
        Void data, ResourceManager manager, ProfilerFiller profiler, Executor executor
    ) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public ResourceLocation getFabricId() {
        return new ResourceLocation(RockGenerationMod.Id, "reload_listener");
    }

    @Override
    public Collection<ResourceLocation> getFabricDependencies() {
        return List.of(ResourceReloadListenerKeys.RECIPES);
    }
}
