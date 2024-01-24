package malilib.util.position;

public class HitPosition
{
    protected final BlockPos blockPos;
    protected final Vec3d exactPos;
    protected final Direction direction;

    public HitPosition(BlockPos blockPos, Vec3d exactPos, Direction direction)
    {
        this.blockPos = blockPos;
        this.exactPos = exactPos;
        this.direction = direction;
    }

    public BlockPos getBlockPos()
    {
        return this.blockPos;
    }

    public Vec3d getExactPos()
    {
        return this.exactPos;
    }

    public Direction getSide()
    {
        return this.direction;
    }

    public static HitPosition of(BlockPos blockPos, Vec3d exactPos, Direction direction)
    {
        return new HitPosition(blockPos, exactPos, direction);
    }
}
