package schmoller.mods.rockgen.recipes;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;

public class FluidSpreadRecipe implements Recipe<Container> {
    private enum FluidSourceState {
        RequireSource,
        RequireFlowing,
        DontCare
    }

    public static final Lazy<ItemStack> LazyLava = Lazy.of(() -> new ItemStack(Items.LAVA_BUCKET));
    public static final String TypeId = "fluid_spread";
    public static final RecipeType<FluidSpreadRecipe> Type = new Type();
    public static final Serializer SerializerInstance = new Serializer();

    private final ResourceLocation id;
    private final Fluid fluid;
    private final Block outputBlock;
    private final Optional<Block> requireBelow;
    private final Optional<Fluid> intoFluid;
    private final FluidSourceState fluidState;
    private final Optional<Block> intoBlock;

    private FluidSpreadRecipe(ResourceLocation id, Fluid fluid, Block outputBlock, Optional<Block> requireBelow,
                              Optional<Fluid> intoFluid, FluidSourceState fluidState, Optional<Block> intoBlock) {
        this.id = id;
        this.fluid = fluid;
        this.outputBlock = outputBlock;
        this.requireBelow = requireBelow;
        this.intoFluid = intoFluid;
        this.fluidState = fluidState;
        this.intoBlock = intoBlock;
    }

    @Override
    public boolean matches(Container inventory, Level world) {
        return false;
    }

    @Override
    public ItemStack assemble(Container inventory) {
        return null;
    }

    @Override
    public boolean canCraftInDimensions(int p_43999_, int p_44000_) {
        return true;
    }

    @Override
    public ItemStack getResultItem() {
        return new ItemStack(outputBlock.asItem());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(Container p_44004_) {
        return Recipe.super.getRemainingItems(p_44004_);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }


    @Override
    public ItemStack getToastSymbol() {
        return LazyLava.get();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SerializerInstance;
    }

    @Override
    public RecipeType<?> getType() {
        return Type;
    }

    @Override
    public String toString() {
        return "FluidSpreadRecipe{" +
                "id=" + id +
                ", fluid=" + fluid +
                ", outputBlock=" + outputBlock +
                ", requireBelow=" + requireBelow +
                ", intoFluid=" + intoFluid +
                ", fluidState=" + fluidState +
                ", intoBlock=" + intoBlock +
                '}';
    }

    private static class Serializer extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<FluidSpreadRecipe> {
        Serializer() {
            setRegistryName(new ResourceLocation("rockgen", TypeId));
        }

        @Override
        public FluidSpreadRecipe fromJson(@NotNull ResourceLocation id, JsonObject document) {
            var inputFluidProperty = document.getAsJsonPrimitive("fluid");
            var outputBlockProperty = document.getAsJsonPrimitive("result");
            var aboveBlockProperty = document.getAsJsonPrimitive("above_block");
            var intoObject = document.getAsJsonObject("into");

            // Input fluid
            if (inputFluidProperty == null || !inputFluidProperty.isString()) {
                throw new IllegalArgumentException("'fluid' property is not a string");
            }

            var inputFluidId = new ResourceLocation(inputFluidProperty.getAsString());
            var inputFluid = ForgeRegistries.FLUIDS.getValue(inputFluidId);
            if (inputFluid == null) {
                throw new IllegalStateException("Unknown fluid " + inputFluidId);
            }

            // Output block
            if (outputBlockProperty == null || !outputBlockProperty.isString()) {
                throw new IllegalArgumentException("'result' property is not a string");
            }

            var outputBlockId = new ResourceLocation(outputBlockProperty.getAsString());
            var outputBlock = ForgeRegistries.BLOCKS.getValue(outputBlockId);
            if (outputBlock == null || outputBlock == Blocks.AIR) {
                throw new IllegalStateException("Unknown block " + outputBlockId);
            }

            // Above block
            Optional<Block> aboveBlock = Optional.empty();

            if (aboveBlockProperty != null) {
                if (!aboveBlockProperty.isString()) {
                    throw new IllegalArgumentException("'result' property is not a string");
                }

                var aboveBlockId = new ResourceLocation(aboveBlockProperty.getAsString());
                var block = ForgeRegistries.BLOCKS.getValue(aboveBlockId);
                if (block == null || block == Blocks.AIR) {
                    throw new IllegalStateException("Unknown block " + aboveBlockId);
                }

                aboveBlock = Optional.of(block);
            }

            // Conditions
            if (intoObject == null) {
                throw new IllegalArgumentException("'into' property is not an object");
            }

            Optional<Fluid> intoFluid = Optional.empty();
            Optional<Block> intoBlock = Optional.empty();

            FluidSourceState intoFluidState = FluidSourceState.DontCare;

            var intoFluidProperty = intoObject.getAsJsonPrimitive("fluid");
            var intoBlockProperty = intoObject.getAsJsonPrimitive("block");

            if (intoFluidProperty != null) {
                if (!intoFluidProperty.isString()) {
                    throw new IllegalArgumentException("'into.fluid' property is not a string");
                }

                var intoFluidId = new ResourceLocation(intoFluidProperty.getAsString());
                var fluid = ForgeRegistries.FLUIDS.getValue(intoFluidId);
                if (fluid == null) {
                    throw new IllegalStateException("Unknown fluid " + intoFluidId);
                }
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

            return new FluidSpreadRecipe(id, inputFluid, outputBlock, aboveBlock, intoFluid, intoFluidState, intoBlock);
        }

        @Nullable
        @Override
        public FluidSpreadRecipe fromNetwork(@NotNull ResourceLocation id, FriendlyByteBuf input) {
            Fluid inputFluid;
            Block outputBlock;
            Optional<Fluid> intoFluid = Optional.empty();
            Optional<Block> intoBlock = Optional.empty();
            Optional<Block> aboveBlock = Optional.empty();

            FluidSourceState intoFluidState = FluidSourceState.DontCare;

            var inputFluidId = input.readResourceLocation();
            inputFluid = ForgeRegistries.FLUIDS.getValue(inputFluidId);
            if (inputFluid == null) {
                throw new IllegalStateException("Unknown fluid " + inputFluidId);
            }

            var outputBlockId = input.readResourceLocation();
            outputBlock = ForgeRegistries.BLOCKS.getValue(outputBlockId);
            if (outputBlock == null) {
                throw new IllegalStateException("Unknown block " + outputBlockId);
            }

            var hasAboveBlock = input.readBoolean();
            if (hasAboveBlock) {
                var blockId = input.readResourceLocation();
                var block = ForgeRegistries.BLOCKS.getValue(blockId);
                if (block == null) {
                    throw new IllegalStateException("Unknown block " + blockId);
                }
                aboveBlock = Optional.of(block);
            }

            var usesIntoFluid = input.readBoolean();
            if (usesIntoFluid) {
                var fluidId = input.readResourceLocation();
                var fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
                if (fluid == null) {
                    throw new IllegalStateException("Unknown fluid " + fluidId);
                }
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

            return new FluidSpreadRecipe(id, inputFluid, outputBlock, aboveBlock, intoFluid, intoFluidState, intoBlock);
        }

        @Override
        public void toNetwork(FriendlyByteBuf output, FluidSpreadRecipe recipe) {
            // NOTE: These cannot be null
            output.writeResourceLocation(recipe.fluid.getRegistryName());
            output.writeResourceLocation(recipe.outputBlock.getRegistryName());

            output.writeBoolean(recipe.requireBelow.isPresent());
            if (recipe.requireBelow.isPresent()) {
                output.writeResourceLocation(recipe.requireBelow.get().getRegistryName());
            }

            output.writeBoolean(recipe.intoFluid.isPresent());
            if (recipe.intoFluid.isPresent()) {
                output.writeResourceLocation(recipe.intoFluid.get().getRegistryName());
                output.writeEnum(recipe.fluidState);
            } else {
                assert (recipe.intoBlock.isPresent());
                output.writeResourceLocation(recipe.intoBlock.get().getRegistryName());
            }
        }
    }

    private static class Type implements RecipeType<FluidSpreadRecipe> {
        @Override
        public String toString() {
            return TypeId;
        }
    }
}
