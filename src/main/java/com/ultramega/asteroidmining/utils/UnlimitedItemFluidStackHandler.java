//package com.ultramega.asteroidmining.utils;
//
//import com.ultramega.asteroidmining.blockentities.AbstractModuleBlockEntity;
//import com.ultramega.asteroidmining.storage.ClientConfigurationSavedData;
//import com.ultramega.asteroidmining.storage.ConfigurationSavedData;
//import com.ultramega.asteroidmining.storage.NetworkConfiguration;
//
//import javax.annotation.Nullable;
//
//import net.minecraft.core.HolderLookup;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.world.item.ItemStack;
//import net.neoforged.neoforge.common.util.INBTSerializable;
//import net.neoforged.neoforge.fluids.FluidStack;
//import net.neoforged.neoforge.fluids.capability.IFluidHandler;
//import net.neoforged.neoforge.items.IItemHandler;
//
//public class UnlimitedItemFluidStackHandler implements IItemHandler, INBTSerializable<CompoundTag>, IFluidHandler {
//    private final AbstractModuleBlockEntity blockEntity;
//
//    public UnlimitedItemFluidStackHandler(final AbstractModuleBlockEntity blockEntity) {
//        this.blockEntity = blockEntity;
//    }
//
//    /*public void addItemStack(final ItemStack stack) {
//        for (final ItemFluidStack existingStack : this.stacks) {
//            if (existingStack.getItemStack() != null) {
//                if (ItemStack.isSameItemSameComponents(existingStack.getItemStack(), stack)) {
//                    existingStack.getItemStack().grow(stack.getCount());
//                    this.onContentsChanged();
//
//                    return;
//                }
//            }
//        }
//
//        this.stacks.add(new ItemFluidStack(stack));
//        this.onContentsChanged();
//    }
//
//    public void addFluidStack(final FluidStack stack) {
//        for (final ItemFluidStack existingStack : this.stacks) {
//            if (existingStack.getFluidStack() != null) {
//                if (FluidStack.isSameFluidSameComponents(existingStack.getFluidStack(), stack)) {
//                    existingStack.getFluidStack().grow(stack.getAmount());
//                    this.onContentsChanged();
//
//                    return;
//                }
//            }
//        }
//
//        this.stacks.add(new ItemFluidStack(stack));
//        this.onContentsChanged();
//    }*/
//
//    @Override
//    public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
//        // Not allowed
//        return stack;
//    }
//
//    @Override
//    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
//        if (amount == 0) {
//            return ItemStack.EMPTY;
//        } else {
//            this.validateSlotIndex(slot);
//            final ItemFluidStack existingStack = this.getStack(slot);
//            final ItemStack existing = existingStack.getItemStack();
//            if (existing == null || existing.isEmpty()) {
//                return ItemStack.EMPTY;
//            } else {
//                final int toExtract = Math.min(amount, existing.getMaxStackSize());
//                if (existing.getCount() <= toExtract) {
//                    if (!simulate) {
//                        this.removeStack(slot);
//                        this.onContentsChanged();
//                        return existing;
//                    } else {
//                        return existing.copy();
//                    }
//                } else {
//                    if (!simulate) {
//                        this.setStack(slot, new ItemFluidStack(existing.copyWithCount(existing.getCount() - toExtract)));
//                        this.onContentsChanged();
//                    }
//
//                    return existing.copyWithCount(toExtract);
//                }
//            }
//        }
//    }
//
//    private ItemFluidStack getStack(final int index) {
//        final NetworkConfiguration configuration = this.getConfiguration();
//        if (configuration == null) {
//            return null;
//        }
//        return configuration.moduleProperties().inventory().get(index);
//    }
//
//    private int getStacksSize() {
//        final NetworkConfiguration configuration = this.getConfiguration();
//        if (configuration == null) {
//            return 0;
//        }
//        return configuration.moduleProperties().inventory().size();
//    }
//
//    private void setStack(final int index, final ItemFluidStack stack) {
//        final NetworkConfiguration configuration = this.getConfiguration();
//        if (configuration == null) {
//            return;
//        }
//        configuration.moduleProperties().inventory().set(index, stack);
//        this.setConfiguration(configuration);
//    }
//
//    private void removeStack(final int index) {
//        final NetworkConfiguration configuration = this.getConfiguration();
//        if (configuration == null) {
//            return;
//        }
//        configuration.moduleProperties().inventory().remove(index);
//        this.setConfiguration(configuration);
//    }
//
//    // TODO: put these into a Util/different class
//    @Nullable
//    private NetworkConfiguration getConfiguration() {
//        if (this.blockEntity.getLevel() instanceof ServerLevel serverLevel) {
//            return ConfigurationSavedData.getConfigurationData(serverLevel).get(this.blockEntity.getSelectedConfigurationUUID());
//        } else {
//            return ClientConfigurationSavedData.INSTANCE.get(this.blockEntity.getSelectedConfigurationUUID());
//        }
//    }
//
//    private void setConfiguration(final NetworkConfiguration configuration) {
//        if (this.blockEntity.getLevel() instanceof ServerLevel serverLevel) {
//            ConfigurationSavedData.getConfigurationData(serverLevel).set(this.blockEntity.getSelectedConfigurationUUID(), configuration);
//        } else {
//            ClientConfigurationSavedData.INSTANCE.put(this.blockEntity.getSelectedConfigurationUUID(), configuration);
//        }
//    }
//
//    @Override
//    public int fill(final FluidStack fluidStack, final FluidAction fluidAction) {
//        // Not allowed
//        return 0;
//    }
//
//    @Override
//    public FluidStack drain(final FluidStack resource, final FluidAction action) {
//        if (resource.isEmpty()) {
//            return FluidStack.EMPTY;
//        }
//        for (int i = 0; i < this.getStacksSize(); i++) {
//            final FluidStack fluidStack = this.getStack(i).getFluidStack();
//            if (fluidStack != null) {
//                if (FluidStack.isSameFluidSameComponents(resource, fluidStack)) {
//                    return this.drainFromTank(fluidStack, resource.getAmount(), action, i);
//                }
//            }
//        }
//
//        return FluidStack.EMPTY;
//    }
//
//    @Override
//    public FluidStack drain(final int maxDrain, final FluidAction action) {
//        for (int i = 0; i < this.getStacksSize(); i++) {
//            final FluidStack fluidStack = this.getStack(i).getFluidStack();
//            if (fluidStack != null && !fluidStack.isEmpty()) {
//                return this.drainFromTank(fluidStack, maxDrain, action, i);
//            }
//        }
//        return FluidStack.EMPTY;
//    }
//
//    private FluidStack drainFromTank(final FluidStack tank, final int maxDrain, final FluidAction action, final int index) {
//        final int drained = Math.min(maxDrain, tank.getAmount());
//        final FluidStack drainedStack = tank.copyWithAmount(drained);
//
//        if (action.execute() && drained > 0) {
//            tank.shrink(drained);
//            if (tank.getAmount() <= 0) {
//                this.removeStack(index);
//            }
//            this.onContentsChanged();
//        }
//
//        return drainedStack;
//    }
//
//    public ItemFluidStack getItemFluidStackInSlot(final int slot) {
//        this.validateSlotIndex(slot);
//        return this.getStack(slot);
//    }
//
//    @Override
//    @Nullable
//    public ItemStack getStackInSlot(final int slot) {
//        this.validateSlotIndex(slot);
//        final ItemStack itemStack = this.getStack(slot).getItemStack();
//        return itemStack != null ? itemStack : ItemStack.EMPTY;
//    }
//
//    @Override
//    @Nullable
//    public FluidStack getFluidInTank(final int slot) {
//        this.validateSlotIndex(slot);
//        final FluidStack fluidStack = this.getStack(slot).getFluidStack();
//        return fluidStack != null ? fluidStack : FluidStack.EMPTY;
//    }
//
//    @Override
//    public int getSlots() {
//        return this.getStacksSize();
//    }
//
//    @Override
//    public int getSlotLimit(final int slot) {
//        return Integer.MAX_VALUE;
//    }
//
//    @Override
//    public boolean isItemValid(final int slot, final ItemStack stack) {
//        return true;
//    }
//
//    @Override
//    public int getTanks() {
//        return this.getStacksSize();
//    }
//
//    @Override
//    public int getTankCapacity(final int i) {
//        return Integer.MAX_VALUE;
//    }
//
//    @Override
//    public boolean isFluidValid(final int index, final FluidStack fluidStack) {
//        return true;
//    }
//
//    @Override
//    public CompoundTag serializeNBT(final HolderLookup.Provider provider) {
//        return new CompoundTag();
//    }
//
//    @Override
//    public void deserializeNBT(final HolderLookup.Provider provider, final CompoundTag nbt) {
//        this.onContentsChanged();
//    }
//
//    protected void validateSlotIndex(final int slot) {
//        if (slot < 0 || slot >= this.getStacksSize()) {
//            throw new RuntimeException("Slot " + slot + " not in valid range - [0," + this.getStacksSize() + ")");
//        }
//    }
//
//    protected void onContentsChanged() {
//    }
//}
