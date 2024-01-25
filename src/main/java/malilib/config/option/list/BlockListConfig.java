package malilib.config.option.list;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;

import net.minecraft.block.Block;

import malilib.util.game.wrap.RegistryUtils;

public class BlockListConfig extends ValueListConfig<Block>
{
    public BlockListConfig(String name, ImmutableList<Block> defaultValues,
                           Function<Block, String> toStringConverter,
                           Function<String, Block> fromStringConverter)
    {
        super(name, defaultValues, toStringConverter, fromStringConverter);
    }

    public BlockListConfig(String name, ImmutableList<Block> defaultValues,
                           Function<Block, String> toStringConverter,
                           Function<String, Block> fromStringConverter,
                           @Nullable String commentTranslationKey, Object... commentArgs)
    {
        super(name, defaultValues, toStringConverter, fromStringConverter, commentTranslationKey, commentArgs);
    }

    @Override
    public BlockListConfig copy()
    {
        BlockListConfig config = new BlockListConfig(this.name, this.defaultValue, this.toStringConverter, this.fromStringConverter);
        config.copyValuesFrom(this);
        return config;
    }

    public static BlockListConfig fromNames(String cfgName, String... blockNames)
    {
        return fromNames(cfgName, Arrays.asList(blockNames));
    }

    public static BlockListConfig fromNames(String cfgName, List<String> blockNames)
    {
        ImmutableList.Builder<Block> builder = ImmutableList.builder();

        for (String name : blockNames)
        {
            Block block = RegistryUtils.getBlockByIdStr(name);

            if (block != null)
            {
                builder.add(block);
            }
        }

        return create(cfgName, builder.build());
    }

    public static BlockListConfig create(String cfgName, ImmutableList<Block> blocks)
    {
        return new BlockListConfig(cfgName, blocks, b -> String.valueOf(b.id), RegistryUtils::getBlockByIdStr);
    }
}
