package com.hivemc.chunker.conversion.intermediate.column.blockentity;

import com.google.gson.JsonElement;
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerBlockType;
import com.hivemc.chunker.nbt.tags.collection.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents a Block Entity (or sometimes known as BlockActor or Tile Entity).
 */
public abstract class BlockEntity {
    private int x;
    private int y;
    private int z;
    private boolean isMovable;
    @Nullable
    private JsonElement customName;
    @Nullable
    private CompoundTag originalNBT;

    // 新增：保存该 BlockEntity 对应的方块类型，便于在转换时获取方块上下文
    @Nullable
    private ChunkerBlockType blockType;

    /**
     * Get the X position of the block entity in global co-ordinates.
     *
     * @return the x co-ordinate.
     */
    public int getX() {
        return x;
    }

    /**
     * Set the X position of the block entity in global co-ordinates.
     *
     * @param x the x co-ordinate.
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Get the Y position of the block entity in global co-ordinates.
     *
     * @return the y co-ordinate.
     */
    public int getY() {
        return y;
    }

    /**
     * Set the Y position of the block entity in global co-ordinates.
     *
     * @param y the y co-ordinate.
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Get the Z position of the block entity in global co-ordinates.
     *
     * @return the z co-ordinate.
     */
    public int getZ() {
        return z;
    }

    /**
     * Set the Z position of the block entity in global co-ordinates.
     *
     * @param z the z co-ordinate.
     */
    public void setZ(int z) {
        this.z = z;
    }

    /**
     * Whether this block entity is movable.
     *
     * @return true if movable (Bedrock only).
     */
    public boolean isMovable() {
        return isMovable;
    }

    /**
     * Set whether this block entity is movable.
     *
     * @param movable true if movable (Bedrock only).
     */
    public void setMovable(boolean movable) {
        isMovable = movable;
    }

    /**
     * The custom name to use for the block entity.
     *
     * @return the custom name to use or null if one is not set.
     */
    @Nullable
    public JsonElement getCustomName() {
        return customName;
    }

    /**
     * Set the custom name to use for the block entity.
     *
     * @param customName the custom name to use or null if one is not set.
     */
    public void setCustomName(@Nullable JsonElement customName) {
        this.customName = customName;
    }

    /**
     * Get the original NBT tag for the block entity.
     *
     * @return if present the original NBT tag.
     */
    public @Nullable CompoundTag getOriginalNBT() {
        return originalNBT;
    }

    /**
     * Set the original NBT tag for the block entity.
     *
     * @param originalNBT the original NBT.
     */
    public void setOriginalNBT(@Nullable CompoundTag originalNBT) {
        this.originalNBT = originalNBT;
    }

    /**
     * 获取对应的方块类型。
     *
     * @return 对应的方块类型，如果未设置则为 null。
     */
    @Nullable
    public ChunkerBlockType getBlockType() {
        return blockType;
    }

    /**
     * 设置对应的方块类型。
     *
     * @param blockType 对应的方块类型。
     */
    public void setBlockType(@Nullable ChunkerBlockType blockType) {
        this.blockType = blockType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockEntity that)) return false;
        return getX() == that.getX() && getY() == that.getY() && getZ() == that.getZ() && isMovable() == that.isMovable() && Objects.equals(getCustomName(), that.getCustomName()) && Objects.equals(getBlockType(), that.getBlockType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getX(), getY(), getZ(), isMovable(), getCustomName(), getBlockType());
    }
}