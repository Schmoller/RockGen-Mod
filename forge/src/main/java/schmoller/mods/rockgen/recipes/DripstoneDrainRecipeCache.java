package schmoller.mods.rockgen.recipes;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collection;

public class DripstoneDrainRecipeCache {
    private Multimap<Block, DripstoneDrainRecipe> blockToRecipeMap;
    private float maxProbability;

    public void invalidate() {
        blockToRecipeMap = null;
    }

    public float getMaxProbability() {
        return maxProbability;
    }

    public Iterable<DripstoneDrainRecipe> get(BlockState blockState, LevelAccessor level) {
        if (blockToRecipeMap == null) {
            prepare(level);
        }

        return blockToRecipeMap.get(blockState.getBlock());
    }

    void prepare(LevelAccessor level) {
        Collection<DripstoneDrainRecipe> recipes;
        if (level instanceof Level) {
            recipes = ((Level) level).getRecipeManager().getAllRecipesFor(DripstoneDrainRecipe.Type);
        } else {
            var server = level.getServer();
            if (server != null) {
                recipes = server.getRecipeManager().getAllRecipesFor(DripstoneDrainRecipe.Type);
            } else {
                throw new IllegalStateException("Not a level and not a server? Cannot get recipes");
            }
        }

        prepare(recipes);
    }

    public void prepare(Collection<DripstoneDrainRecipe> recipes) {
        blockToRecipeMap = MultimapBuilder.hashKeys().arrayListValues().build();

        maxProbability = 0;
        for (var recipe : recipes) {
            recipe.getBlock().forEachMatchingRegisteredBlock(block -> {
                blockToRecipeMap.put(block, recipe);
            });

            if (recipe.getProbability() > maxProbability) {
                maxProbability = recipe.getProbability();
            }
        }
    }
}
