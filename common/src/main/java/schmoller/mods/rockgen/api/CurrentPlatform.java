package schmoller.mods.rockgen.api;

import org.jetbrains.annotations.NotNull;
import schmoller.mods.rockgen.recipes.DripstoneDrainRecipeCache;
import schmoller.mods.rockgen.recipes.FluidSpreadRecipeCache;

/**
 * A bridge between Forge and Fabric.
 * The mod must initialise this object at startup
 */
public class CurrentPlatform {
    private final static CurrentPlatform instance = new CurrentPlatform();
    private final FluidSpreadRecipeCache fluidSpreadRecipeCache = new FluidSpreadRecipeCache();
    private final DripstoneDrainRecipeCache dripstoneDrainRecipeCache = new DripstoneDrainRecipeCache();
    private PlatformHandlers platformHandlers;

    private CurrentPlatform() {
    }

    public static CurrentPlatform getInstance() {
        return instance;
    }

    public @NotNull PlatformHandlers getPlatformHandlers() {
        assert platformHandlers != null : "Interaction handler not set";
        return platformHandlers;
    }

    public void setPlatformHandlers(@NotNull PlatformHandlers handler) {
        assert platformHandlers == null : "Interaction handler already set";
        platformHandlers = handler;
    }

    public @NotNull FluidSpreadRecipeCache getFluidSpreadRecipeCache() {
        return fluidSpreadRecipeCache;
    }

    public @NotNull DripstoneDrainRecipeCache getDripstoneDrainRecipeCache() {
        return dripstoneDrainRecipeCache;
    }
}
