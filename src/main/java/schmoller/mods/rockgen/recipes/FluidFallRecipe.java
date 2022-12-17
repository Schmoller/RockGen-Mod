package schmoller.mods.rockgen.recipes;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class FluidFallRecipe implements Recipe<Container> {
    private enum FluidSourceState {
        RequireSource,
        RequireFlowing,
        DontCare
    }

    public static final Lazy<ItemStack> LazyLava = Lazy.of(() -> new ItemStack(Items.LAVA_BUCKET));
    public static final String TypeId = "fluid_fall";
    public static final RecipeType<FluidFallRecipe> Type = new Type();
    public static final Serializer SerializerInstance = new Serializer();

    private final ResourceLocation id;
    private final TagKey<Fluid> fluid;
    private final Block outputBlock;
    private final Optional<TagKey<Fluid>> intoFluid;
    private final FluidSourceState fluidState;
    private final Optional<Block> intoBlock;

    private FluidFallRecipe(ResourceLocation id, TagKey<Fluid> fluid, Block outputBlock, Optional<TagKey<Fluid>> intoFluid,
                            FluidSourceState fluidState, Optional<Block> intoBlock) {
        this.id = id;
        this.fluid = fluid;
        this.outputBlock = outputBlock;
        this.intoFluid = intoFluid;
        this.fluidState = fluidState;
        this.intoBlock = intoBlock;
    }

    public TagKey<Fluid> fluidToMatch() {
        return fluid;
    }

    @Override
    public boolean matches(@NotNull Container inventory, @NotNull Level world) {
        return false;
    }

    public Optional<Block> tryMatch(LevelAccessor level, BlockPos position) {
        boolean isSourceBlock = false;


        if (intoFluid.isPresent()) {
            var fluidInWorld = level.getFluidState(position);

            if (!fluidInWorld.is(intoFluid.get())) {
                return Optional.empty();
            }

            if (fluidState != FluidSourceState.DontCare) {
                if (fluidState == FluidSourceState.RequireSource && !fluidInWorld.isSource()) {
                    return Optional.empty();
                }

                if (fluidState == FluidSourceState.RequireFlowing && fluidInWorld.isSource()) {
                    return Optional.empty();
                }
            }
        } else if (intoBlock.isPresent()) {
            if (!level.getBlockState(position).is(intoBlock.get())) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }

        return Optional.of(outputBlock);
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Container inventory) {
        throw new IllegalStateException("This recipe cannot be used with an inventory");
    }

    @Override
    public boolean canCraftInDimensions(int p_43999_, int p_44000_) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem() {
        return new ItemStack(outputBlock.asItem());
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
        return LazyLava.get();
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

    @Override
    public String toString() {
        return "FluidFallRecipe{" +
                "id=" + id +
                ", fluid=" + fluid +
                ", outputBlock=" + outputBlock +
                ", intoFluid=" + intoFluid +
                ", fluidState=" + fluidState +
                ", intoBlock=" + intoBlock +
                '}';
    }

    private static class Serializer extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<FluidFallRecipe> {
        Serializer() {
            setRegistryName(new ResourceLocation("rockgen", TypeId));
        }

        @Override
        public @NotNull FluidFallRecipe fromJson(@NotNull ResourceLocation id, JsonObject document) {
            var inputFluidProperty = document.getAsJsonPrimitive("fluid");
            var outputBlockProperty = document.getAsJsonPrimitive("result");
            var intoObject = document.getAsJsonObject("into");

            // Input fluid
            if (inputFluidProperty == null || !inputFluidProperty.isString()) {
                throw new IllegalArgumentException("'fluid' property is not a string");
            }

            var inputFluidId = new ResourceLocation(inputFluidProperty.getAsString());
            var inputFluid = FluidTags.create(inputFluidId);

            // Output block
            if (outputBlockProperty == null || !outputBlockProperty.isString()) {
                throw new IllegalArgumentException("'result' property is not a string");
            }

            var outputBlockId = new ResourceLocation(outputBlockProperty.getAsString());
            var outputBlock = ForgeRegistries.BLOCKS.getValue(outputBlockId);
            if (outputBlock == null || outputBlock == Blocks.AIR) {
                throw new IllegalStateException("Unknown block " + outputBlockId);
            }

            // Conditions
            if (intoObject == null) {
                throw new IllegalArgumentException("'into' property is not an object");
            }

            Optional<TagKey<Fluid>> intoFluid = Optional.empty();
            Optional<Block> intoBlock = Optional.empty();

            FluidSourceState intoFluidState = FluidSourceState.DontCare;

            var intoFluidProperty = intoObject.getAsJsonPrimitive("fluid");
            var intoBlockProperty = intoObject.getAsJsonPrimitive("block");

            if (intoFluidProperty != null) {
                if (!intoFluidProperty.isString()) {
                    throw new IllegalArgumentException("'into.fluid' property is not a string");
                }

                var intoFluidId = new ResourceLocation(intoFluidProperty.getAsString());
                var fluid = FluidTags.create(intoFluidId);
                intoFluid = Optional.of(fluid);

                var fluidTypeProperty = intoObject.getAsJsonPrimitive("type");
                if (fluidTypeProperty != null) {
                    if (!fluidTypeProperty.isString()) {
                        throw new IllegalArgumentException("'into.type' property is not a string");
                    }

                    var rawValue = fluidTypeProperty.getAsString();
                    intoFluidState = switch (rawValue) {
                        case "source" -> FluidSourceState.RequireSource;
                        case "flowing" -> FluidSourceState.RequireFlowing;
                        case "dont-care" -> FluidSourceState.DontCare;
                        default -> throw new IllegalArgumentException("'into.type' property is not one of: 'source', 'flowing', or 'dont-care'");
                    };
                }
            } else if (intoBlockProperty != null) {
                if (!intoBlockProperty.isString()) {
                    throw new IllegalArgumentException("'into.block' property is not a string");
                }

                var intoBlockId = new ResourceLocation(intoBlockProperty.getAsString());
                var block = ForgeRegistries.BLOCKS.getValue(intoBlockId);
                if (block == null || block == Blocks.AIR) {
                    throw new IllegalStateException("Unknown block " + intoBlockId);
                }

                intoBlock = Optional.of(block);

            } else {
                throw new IllegalStateException("Missing 'into.fluid' or 'into.block'");
            }

            return new FluidFallRecipe(id, inputFluid, outputBlock, intoFluid, intoFluidState, intoBlock);
        }

        @Nullable
        @Override
        public FluidFallRecipe fromNetwork(@NotNull ResourceLocation id, FriendlyByteBuf input) {
            TagKey<Fluid> inputFluid;
            Block outputBlock;
            Optional<TagKey<Fluid>> intoFluid = Optional.empty();
            Optional<Block> intoBlock = Optional.empty();

            FluidSourceState intoFluidState = FluidSourceState.DontCare;

            var inputFluidId = input.readResourceLocation();
            inputFluid = FluidTags.create(inputFluidId);

            var outputBlockId = input.readResourceLocation();
            outputBlock = ForgeRegistries.BLOCKS.getValue(outputBlockId);
            if (outputBlock == null) {
                throw new IllegalStateException("Unknown block " + outputBlockId);
            }

            var usesIntoFluid = input.readBoolean();
            if (usesIntoFluid) {
                var fluidId = input.readResourceLocation();
                var fluid = FluidTags.create(fluidId);
                intoFluid = Optional.of(fluid);

                intoFluidState = input.readEnum(FluidSourceState.class);
            } else {
                var blockId = input.readResourceLocation();
                var block = ForgeRegistries.BLOCKS.getValue(blockId);
                if (block == null) {
                    throw new IllegalStateException("Unknown block " + blockId);
                }
                intoBlock = Optional.of(block);
            }

            return new FluidFallRecipe(id, inputFluid, outputBlock, intoFluid, intoFluidState, intoBlock);
        }

        @Override
        public void toNetwork(FriendlyByteBuf output, FluidFallRecipe recipe) {
            // NOTE: These cannot be null
            output.writeResourceLocation(recipe.fluid.location());
            output.writeResourceLocation(recipe.outputBlock.getRegistryName());

            output.writeBoolean(recipe.intoFluid.isPresent());
            if (recipe.intoFluid.isPresent()) {
                output.writeResourceLocation(recipe.intoFluid.get().location());
                output.writeEnum(recipe.fluidState);
            } else {
                assert (recipe.intoBlock.isPresent());
                output.writeResourceLocation(recipe.intoBlock.get().getRegistryName());
            }
        }
    }

    private static class Type implements RecipeType<FluidFallRecipe> {
        @Override
        public String toString() {
            return TypeId;
        }
    }
}
