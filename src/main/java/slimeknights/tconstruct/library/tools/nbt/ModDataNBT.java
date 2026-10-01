package slimeknights.tconstruct.library.tools.nbt;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiFunction;

/**
 * NBT representing extra data on the tool for modifiers, with a wrapper around the compound for to enforce namespacing data.
 * On a typical tool, there are two copies of this class, one for persistent data, and one that rebuilds when the modifiers refresh.
 * Note unlike other NBT classes, the data inside this one is mutable as most of it is directly used by the tools.
 */
@EqualsAndHashCode(callSuper = true)
public class ModDataNBT extends NamespacedNBT implements IModDataView {
  /** Creates a new mod data containing empty data */
  public ModDataNBT() {
    super();
  }

  /** Creates a new mod data wrapping the given compound */
  protected ModDataNBT(CompoundTag data) {
    super(data);
  }

  /** Constructor to clone from another instance, needed to deal with an API conflict */
  public ModDataNBT(NamespacedNBT nbt) {
    super(nbt.getData());
  }

  @Override
  public <T> T get(ResourceLocation name, BiFunction<CompoundTag,String,T> function) {
    return function.apply(getData(), name.toString());
  }

  /** Both view interfaces declare the same default, so name the one to use explicitly */
  @Override
  public CompoundTag getCompound(ResourceLocation name) {
    return get(name, CompoundTag::getCompound);
  }

  @Override
  public ListTag getList(ResourceLocation name, int type) {
    // save generation of the extra lambda object
    return getData().getList(name.toString(), type);
  }

  @Override
  public boolean contains(ResourceLocation name) {
    return getData().contains(name.toString());
  }

  @Override
  public boolean contains(ResourceLocation name, int type) {
    return getData().contains(name.toString(), type);
  }

  /**
   * Sets the given NBT into the data
   * @param name  Key name
   * @param nbt   NBT value
   */
  public void put(ResourceLocation name, Tag nbt) {
    getData().put(name.toString(), nbt);
  }

  /**
   * Sets an integer from the mod data
   * @param name  Name
   * @param value  Integer value
   */
  public void putInt(ResourceLocation name, int value) {
    getData().putInt(name.toString(), value);
  }

  /**
   * Sets an boolean from the mod data
   * @param name  Name
   * @param value  Boolean value
   */
  public void putBoolean(ResourceLocation name, boolean value) {
    getData().putBoolean(name.toString(), value);
  }

  /**
   * Sets an float from the mod data
   * @param name  Name
   * @param value  Float value
   */
  public void putFloat(ResourceLocation name, float value) {
    getData().putFloat(name.toString(), value);
  }

  /**
   * Reads a string from the mod data
   * @param name  Name
   * @param value  String value
   */
  public void putString(ResourceLocation name, String value) {
    getData().putString(name.toString(), value);
  }

  /**
   * Removes the given key from the NBT
   * @param name  Key to remove
   */
  public void remove(ResourceLocation name) {
    getData().remove(name.toString());
  }


  /* Networking */

  /** Gets a copy of the internal data, generally should only be used for syncing, no reason to call directly */
  public CompoundTag getCopy() {
    return getData().copy();
  }

  /**
   * Called to merge this NBT data from another
   * @param data  data
   */
  public void copyFrom(CompoundTag data) {
    getData().getAllKeys().clear();
    getData().merge(data);
  }

  /**
   * Parses the data from NBT
   * @param data  data
   * @return  Parsed mod data
   */
  public static ModDataNBT readFromNBT(CompoundTag data) {
    return new ModDataNBT(data);
  }
}
