package com.vladmarica.energymeters.tile;

import com.vladmarica.energymeters.EnergyMetersMod;
import com.vladmarica.energymeters.energy.EnergyTypes;
import com.vladmarica.energymeters.energy.storage.EUStorage;
import com.vladmarica.energymeters.tile.config.EnumRedstoneControlState;
import ic2.api.energy.EnergyNet;
import ic2.api.energy.NodeStats;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyTile;
import javax.annotation.Nullable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;

public class TileEntityEnergyMeterEU extends TileEntityEnergyMeterBase {

  private EUStorage storage;
  private boolean addedToIC2EnergyNet = false;

  public TileEntityEnergyMeterEU() {
    super(EnergyTypes.EU);
  }

  @Override
  public void onLoad() {
    this.storage = new EUStorage(this);

    super.onLoad();

    if (!this.world.isRemote) {
      this.addToEnergyNet();
    }
  }

  @Override
  public void update() {
    if (!this.world.isRemote) {
      NodeStats stats = EnergyNet.instance.getNodeStats(this.storage);
      if (stats != null) {
        this.totalEnergyTransferred += stats.getEnergyIn();
      }
    }

    super.update();
  }

  @Override
  public int getEnergyScale() {
    return 1;
  }

  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    this.removeFromEnergyNet();
  }

  @Override
  public void invalidate() {
    super.invalidate();
    this.removeFromEnergyNet();
  }

  private void removeFromEnergyNet() {
    if (!this.world.isRemote && this.addedToIC2EnergyNet) {
      MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this.storage));
      this.addedToIC2EnergyNet = false;
      EnergyMetersMod.LOGGER.debug("Remove EU meter {} from energy net", this.pos);
    }
  }

  private void addToEnergyNet() {
    if (!this.world.isRemote && !this.addedToIC2EnergyNet) {
      MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this.storage));
      this.addedToIC2EnergyNet = true;
      EnergyMetersMod.LOGGER.debug("Added EU meter {} from energy net", this.pos);
    }
  }

  @Override
  public void handleSideUpdateRequest(@Nullable EnumFacing inputSide,
      @Nullable EnumFacing outputSide) {

    if (!this.world.isRemote) {
      this.removeFromEnergyNet();
      this.addToEnergyNet();
    }

    super.handleSideUpdateRequest(inputSide, outputSide);
  }

  @Override
  protected void checkConnections() {
    boolean connected = false;

    if (this.inputSide != null && this.outputSide != null) {
      BlockPos inputNeighbor = this.pos.offset(this.inputSide);
      BlockPos outputNeightbor = this.pos.offset(this.outputSide);

      IEnergyTile inputEnergyTile = EnergyNet.instance.getTile(world, inputNeighbor);
      IEnergyTile outputEnergyTile = EnergyNet.instance.getTile(world, outputNeightbor);
      connected = inputEnergyTile != null
          && outputEnergyTile != null;
    }

    if (connected != this.fullyConnected) {
      this.fullyConnected = connected;
      this.notifyUpdate();
    }
  }

  @Override
  public void handleConfigUpdateRequest(EnumRedstoneControlState redstoneControlState,
      int energyAliasIndex) {
    super.handleConfigUpdateRequest(redstoneControlState, energyAliasIndex);
    this.checkRedstone();
  }


  @Override
  protected void checkRedstone() {
    super.checkRedstone();

    if (this.isDisabled()) {
      this.removeFromEnergyNet();
    } else {
      this.addToEnergyNet();
    }
  }

  @Override
  public boolean canReceiveEnergy(EnumFacing side) {
    return side == this.inputSide;
  }

  @Override
  public boolean canEmitEnergy(EnumFacing side) {
    return side == this.outputSide;
  }
  
  @Override
  public void setInputSide(@Nullable EnumFacing side) {
    this.removeFromEnergyNet();
    super.setInputSide(side);
    this.addToEnergyNet();
  }

  @Override
  public void setOutputSide(@Nullable EnumFacing side) {
    this.removeFromEnergyNet();
    super.setOutputSide(side);
    this.addToEnergyNet();
  }

  /**
   * Unused in EU meters since the calculation is done in {@link #update()} instead.
   */
  @Override
  public long receiveEnergy(long amount, boolean simulate, EnumFacing side) {
    return 0;
  }
}
