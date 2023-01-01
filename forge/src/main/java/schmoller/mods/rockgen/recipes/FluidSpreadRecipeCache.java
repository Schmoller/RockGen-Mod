package schmoller.mods.rockgen.recipes;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;

import java.util.Collection;
import java.util.List;

public class FluidSpreadRecipeCache {
    private List<GroupedRecipes> groupedRegularRecipes;
    private List<GroupedRecipes> groupedFallingRecipes;

    public void invalidate() {
        groupedRegularRecipes = null;
        groupedFallingRecipes = null;
    }

    public void prepare(Collection<FluidSpreadRecipe> recipes) {
        ListMultimap<TagKey<Fluid>, FluidSpreadRecipe> recipesByFluid = ArrayListMultimap.create();

        for (var recipe : recipes) {
            recipesByFluid.put(recipe.getFluidToMatch(), recipe);
        }

        groupedRegularRecipes = recipesByFluid.keys().stream().distinct().map(key -> {
            var recipesOfSameFluidType = recipesByFluid.get(key)
                .stream()
                .filter(recipe -> recipe.fluidSpreadDirection() == FluidSpreadRecipe.FluidSpreadDirection.Regular)
                .sorted()
                .toList();
            return new GroupedRecipes(key, recipesOfSameFluidType);
        }).filter(group -> !group.recipes.isEmpty()).toList();

        groupedFallingRecipes = recipesByFluid.keys().stream().distinct().map(key -> {
            var recipesOfSameFluidType = recipesByFluid.get(key)
                .stream()
                .filter(recipe -> recipe.fluidSpreadDirection() == FluidSpreadRecipe.FluidSpreadDirection.Down)
                .sorted()
                .toList();
            return new GroupedRecipes(key, recipesOfSameFluidType);
        }).filter(group -> !group.recipes.isEmpty()).toList();
    }

    void prepare(LevelAccessor level) {
        Collection<FluidSpreadRecipe> recipes;
        if (level instanceof Level) {
            recipes = ((Level) level).getRecipeManager().getAllRecipesFor(FluidSpreadRecipe.Type);
        } else {
            var server = level.getServer();
            if (server != null) {
                recipes = server.getRecipeManager().getAllRecipesFor(FluidSpreadRecipe.Type);
            } else {
                throw new IllegalStateException("Not a level and not a server? Cannot get recipes");
            }
        }

        prepare(recipes);
    }

    public Iterable<FluidSpreadRecipe> getRegular(FlowingFluid fluidState, LevelAccessor level) {
        if (groupedRegularRecipes == null) {
            prepare(level);
        }

        for (var group : groupedRegularRecipes) {
            if (fluidState.is(group.fluid)) {
                return group.recipes;
            }
        }

        return List.of();
    }

    public Iterable<FluidSpreadRecipe> getFalling(FlowingFluid flowingType, LevelAccessor level) {
        if (groupedFallingRecipes == null) {
            prepare(level);
        }

        for (var group : groupedFallingRecipes) {
            if (flowingType.is(group.fluid)) {
                return group.recipes;
            }
        }

        return List.of();
    }

    private record GroupedRecipes(TagKey<Fluid> fluid, List<FluidSpreadRecipe> recipes) {}
}
