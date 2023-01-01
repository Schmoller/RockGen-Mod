package schmoller.mods.rockgen.recipes;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
public class DripstoneDrainRecipe implements Recipe<Container> {
    public static final String TypeId = "dripstone_draining";
    public static final RecipeType<DripstoneDrainRecipe> Type = new Type();
    public static final Serializer SerializerInstance = new Serializer();
    private final ResourceLocation id;
    private final BlockMatcher block;
    private final SimpleParticleType particleType;
    private final boolean allowUltraWarm;
    private final float probability;
    private final RecipeOutputSelector outputs;

    private DripstoneDrainRecipe(
        ResourceLocation id, BlockMatcher block, float probability, RecipeOutputSelector outputs,
        boolean allowUltraWarm, Optional<SimpleParticleType> particleType
    ) {
        this.id = id;
        this.block = block;
        this.probability = probability;
        this.outputs = outputs;
        this.allowUltraWarm = allowUltraWarm;
        this.particleType = particleType.orElse(ParticleTypes.DRIPPING_DRIPSTONE_WATER);
    }

    public BlockMatcher getBlock() {
        return block;
    }

    public SimpleParticleType getParticleType() {
        return particleType;
    }

    public float getProbability() {
        return probability;
    }

    public boolean getAllowUltraWarm() {
        return allowUltraWarm;
    }

    public RecipeOutputSelector getOutputs() {
        return outputs;
    }

    @Override
    public boolean matches(@NotNull Container inventory, @NotNull Level world) {
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Container inventory) {
        throw new IllegalStateException("Recipe cannot be used with inventories");
    }

    @Override
    public boolean canCraftInDimensions(int p_43999_, int p_44000_) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem() {
        return new ItemStack(outputs.selectFirst().asItem());
    }

    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(@NotNull Container p_44004_) {
        return Recipe.super.getRemainingItems(p_44004_);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public @NotNull ItemStack getToastSymbol() {
        return new ItemStack(Blocks.POINTED_DRIPSTONE);
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return SerializerInstance;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return Type;
    }

    public Optional<Block> tryMatch(BlockState blockState, LevelAccessor level, float randomNumber) {
        if (randomNumber > probability) {
            return Optional.empty();
        }

        if (!doesMatch(blockState, level)) {
            return Optional.empty();
        }

        return Optional.of(outputs.selectRandom());
    }

    public boolean doesMatch(BlockState blockState, LevelAccessor level) {
        if (!allowUltraWarm && level.dimensionType().ultraWarm()) {
            return false;
        }

        if (!block.matches(blockState)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "DripstoneDrainRecipe{" + "id=" + id + ", block=" + block + ", fluidParticleType=" + particleType + ", probability=" + probability + ", outputs=" + outputs + '}';
    }

    public static class Serializer implements RecipeSerializer<DripstoneDrainRecipe> {
        private final Gson gson = new Gson();

        @Override
        public @NotNull DripstoneDrainRecipe fromJson(@NotNull ResourceLocation id, JsonObject document) {
            var blockProperty = document.get("block_to_drain");
            var probabilityProperty = document.get("probability");
            var ultraWarmProperty = document.get("ultra_warm");

            if (!blockProperty.isJsonPrimitive()) {
                throw new IllegalArgumentException("'block_to_drain' must be a string");
            }

            var block = BlockMatcher.createFromJson(blockProperty.getAsJsonPrimitive(), "block_to_drain");

            if (probabilityProperty == null || !probabilityProperty.isJsonPrimitive() || !probabilityProperty
                .getAsJsonPrimitive()
                .isNumber()) {
                throw new IllegalArgumentException("'probability' must be a number");
            }

            var probability = probabilityProperty.getAsJsonPrimitive().getAsFloat();
            if (probability <= 0 || probability > 100) {
                throw new IllegalArgumentException("'probability' must be between 1 and 100");
            }

            probability /= 100;

            var allowUltraWarm = true;
            if (ultraWarmProperty != null) {
                if (!ultraWarmProperty.isJsonPrimitive() || !ultraWarmProperty.getAsJsonPrimitive().isBoolean()) {
                    throw new IllegalArgumentException("'ultra_warm' must be a boolean if provided");
                }

                allowUltraWarm = ultraWarmProperty.getAsBoolean();
            }

            Optional<SimpleParticleType> fluidParticles = Optional.empty();

            var fluidParticlesProperty = document.getAsJsonPrimitive("fluid_particles");

            if (fluidParticlesProperty != null) {
                if (!fluidParticlesProperty.isString()) {
                    throw new IllegalArgumentException("'fluid_particles' property is not a string");
                }

                var fluidParticlesId = new ResourceLocation(fluidParticlesProperty.getAsString());
                var particleType = Registry.PARTICLE_TYPE.get(fluidParticlesId);
                if (particleType == null) {
                    throw new IllegalArgumentException("Unknown particle type '" + fluidParticlesId + "'");
                }
                if (!(particleType instanceof SimpleParticleType)) {
                    throw new IllegalArgumentException("Particle type '" + fluidParticlesId + "' needs to be a simple particle");
                }

                fluidParticles = Optional.of((SimpleParticleType) particleType);
            }

            // Output
            var resultProperty = document.get("result");
            var outputs = RecipeOutputSelector.createFromJson(resultProperty, "result");

            return new DripstoneDrainRecipe(id, block, probability, outputs, allowUltraWarm, fluidParticles);
        }

        @Nullable
        @Override
        public DripstoneDrainRecipe fromNetwork(@NotNull ResourceLocation id, FriendlyByteBuf input) {
            var block = BlockMatcher.createFromByteBuf(input);
            var probability = input.readFloat();
            var fluidParticle = Registry.PARTICLE_TYPE.get(input.readResourceLocation());
            var allowUltraWarm = input.readBoolean();
            var outputs = RecipeOutputSelector.createFromBytes(input);

            return new DripstoneDrainRecipe(id,
                block,
                probability,
                outputs,
                allowUltraWarm,
                Optional.of((SimpleParticleType) fluidParticle)
            );
        }

        @Override
        public void toNetwork(FriendlyByteBuf output, DripstoneDrainRecipe recipe) {
            recipe.block.writeToByteBuf(output);
            output.writeFloat(recipe.probability);
            var particleId = Registry.PARTICLE_TYPE.getKey(recipe.particleType);
            assert (particleId != null);
            output.writeResourceLocation(particleId);
            output.writeBoolean(recipe.allowUltraWarm);
            recipe.outputs.writeToByteBuf(output);
        }
    }

    private static class Type implements RecipeType<DripstoneDrainRecipe> {
        @Override
        public String toString() {
            return TypeId;
        }
    }
}
