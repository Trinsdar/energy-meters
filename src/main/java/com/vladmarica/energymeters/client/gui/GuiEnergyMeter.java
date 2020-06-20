package com.vladmarica.energymeters.client.gui;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.vladmarica.energymeters.EnergyMetersMod;
import com.vladmarica.energymeters.Util;
import com.vladmarica.energymeters.client.Sprites;
import com.vladmarica.energymeters.energy.EnergyType.EnergyAlias;
import com.vladmarica.energymeters.network.PacketUpdateMeterConfig;
import com.vladmarica.energymeters.network.PacketUpdateMeterSides;
import com.vladmarica.energymeters.tile.config.EnumRedstoneControlState;
import com.vladmarica.energymeters.tile.TileEntityEnergyMeter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiEnergyMeter extends GuiScreen {
  private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(
      EnergyMetersMod.MODID, "textures/gui/energymeter.png");

  private static final ResourceLocation SCREEN_TEXURE = new ResourceLocation(
      EnergyMetersMod.MODID, "textures/blocks/meter_screen.png");

  private static final ResourceLocation SIDE_TEXTURE = new ResourceLocation(
      EnergyMetersMod.MODID, "textures/blocks/meter.png");

  private static final ResourceLocation INPUT_TEXTURE = new ResourceLocation(
      EnergyMetersMod.MODID, "textures/blocks/meter_input.png");

  private static final ResourceLocation OUTPUT_TEXTURE = new ResourceLocation(
      EnergyMetersMod.MODID, "textures/blocks/meter_output.png");

  private static final int TEXTURE_WIDTH = 256;
  private static final int TEXTURE_HEIGHT = 148;
  private static final int COLOR_GREY = 4210752;
  private static final int COLOR_WHITE = 0xFFFFFF;

  private GuiButtonEnergyAlias energyAliasButton;
  private GuiButtonConfigEnum<EnumRedstoneControlState> redstoneControlButton;

  private TileEntityEnergyMeter tile;
  private BiMap<RelativeBlockSide, EnumFacing> sideToFaceMap;
  private Map<RelativeBlockSide, GuiButtonSideConfig> sideToButtonMap = new HashMap<>();

  public GuiEnergyMeter(TileEntityEnergyMeter tile) {
    this.tile = tile;
    this.updateSideMapping();
  }

  private void updateSideMapping() {
    EnumFacing screenFace = this.tile.getScreenSide();
    this.sideToFaceMap = HashBiMap.create();
    this.sideToFaceMap.put(RelativeBlockSide.TOP, EnumFacing.UP);
    this.sideToFaceMap.put(RelativeBlockSide.BOTTOM, EnumFacing.DOWN);
    this.sideToFaceMap.put(RelativeBlockSide.FRONT, screenFace);
    this.sideToFaceMap.put(RelativeBlockSide.LEFT, Util.getLeftFace(screenFace));
    this.sideToFaceMap.put(RelativeBlockSide.RIGHT, Util.getRightFace(screenFace));
    this.sideToFaceMap.put(RelativeBlockSide.BACK, Util.getBackFace(screenFace));
  }

  @Override
  public void initGui() {
    super.initGui();

    int x = (this.width - TEXTURE_WIDTH) / 2;
    int y = (this.height - TEXTURE_HEIGHT) / 2;

    this.buttonList.add(this.energyAliasButton = new GuiButtonEnergyAlias(0, x + 180, y + 24, this.tile.getEnergyAlias()));
    this.buttonList.add(this.redstoneControlButton = new GuiButtonConfigEnum<>(1, "Redstone Control", x + 180 + 25, y + 24, EnumRedstoneControlState.class, tile.getRedstoneControlState()));
    int startX = 195;
    int startY = 87;

    int buttonSize = 20;

    this.sideToButtonMap = new HashMap<>();
    this.sideToButtonMap.put(RelativeBlockSide.FRONT,
        new GuiButtonSideConfig(x + startX, y + startY, RelativeBlockSide.FRONT, SCREEN_TEXURE, true));
    this.sideToButtonMap.put(RelativeBlockSide.BACK,
        new GuiButtonSideConfig(x + startX + buttonSize, y + startY + buttonSize, RelativeBlockSide.BACK, SCREEN_TEXURE));
    this.sideToButtonMap.put(RelativeBlockSide.TOP,
        new GuiButtonSideConfig(x + startX, y + startY - buttonSize, RelativeBlockSide.TOP, SCREEN_TEXURE));
    this.sideToButtonMap.put(RelativeBlockSide.BOTTOM,
        new GuiButtonSideConfig(x + startX, y + startY + buttonSize, RelativeBlockSide.BOTTOM, SCREEN_TEXURE));
    this.sideToButtonMap.put(RelativeBlockSide.LEFT,
        new GuiButtonSideConfig(x + startX - buttonSize, y + startY, RelativeBlockSide.LEFT, SCREEN_TEXURE));
    this.sideToButtonMap.put(RelativeBlockSide.RIGHT,
        new GuiButtonSideConfig(x + startX + buttonSize, y + startY, RelativeBlockSide.RIGHT, SCREEN_TEXURE));

    this.updateConfigButtonTextures();
  }

  private void updateConfigButtonTextures() {
    for (GuiButtonSideConfig button: this.sideToButtonMap.values()) {
      button.setTexture(this.getTextureForSide(button.getSide()));
    }
  }

  private ResourceLocation getTextureForSide(RelativeBlockSide side) {
    if (side == RelativeBlockSide.FRONT) {
      return SCREEN_TEXURE;
    }

    EnumFacing face = this.sideToFaceMap.get(side);
    if (face == tile.getInputSide()) {
      return INPUT_TEXTURE;
    } else if (face == tile.getOutputSide()) {
      return OUTPUT_TEXTURE;
    }

    return SIDE_TEXTURE;
  }

  @Override
  public void updateScreen() {
    super.updateScreen();
  }

  private String getStatusString() {
    if (!this.tile.isFullyConnected()) {
      return TextFormatting.GOLD + "Not Connected";
    }
    if (this.tile.isDisabled()) {
      return TextFormatting.RED + "Disabled";
    }
    return TextFormatting.GREEN + "Active";
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    this.drawDefaultBackground();

    int x = (this.width - TEXTURE_WIDTH) / 2;
    int y = (this.height - TEXTURE_HEIGHT) / 2;

    this.mc.getTextureManager().bindTexture(BACKGROUND_TEXTURE);
    this.drawTexturedModalRect(x, y, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT);

    int titleWidth = this.fontRenderer.getStringWidth("Energy Meter");
    int titleX = (TEXTURE_WIDTH - titleWidth) / 2;
    this.fontRenderer.drawString("Energy Meter", x + titleX, y + 7, COLOR_GREY);

    int leftPanelWidth = 148;
    int leftPanelOffset = 18;

    String units = this.tile.getEnergyAlias().getDisplayName();
    this.fontRenderer.drawString(TextFormatting.GRAY + "Transfer Rate", x  + leftPanelOffset, y + 45, COLOR_WHITE);
    this.fontRenderer.drawString(tile.getTransferRate() + " " + units + "/t", x  + leftPanelOffset, y + 55, COLOR_WHITE);

    this.fontRenderer.drawString(TextFormatting.GRAY + "Total Transferred", x  + leftPanelOffset, y + 70, COLOR_WHITE);
    this.fontRenderer.drawString(tile.getTotalEnergyTransferred() + " " + units, x  + leftPanelOffset, y + 80, COLOR_WHITE);

    this.fontRenderer.drawString(TextFormatting.GRAY + "Status", x  + leftPanelOffset, y + 95, COLOR_WHITE);
    this.fontRenderer.drawString(getStatusString(), x  + leftPanelOffset, y + 105, COLOR_WHITE);

    this.updateConfigButtonTextures();
    for (GuiButtonSideConfig sideConfigButton : this.sideToButtonMap.values()) {
      GlStateManager.color(1, 1, 1, 1);
      sideConfigButton.draw(mouseX, mouseY);
    }

    // Render buttons and labels
    super.drawScreen(mouseX, mouseY, partialTicks);

    for (GuiButtonSideConfig sideConfigButton : this.sideToButtonMap.values()) {
      if (sideConfigButton.isMouseHovered()) {
        List<String> lines = new ArrayList<>(1);
        lines.add(sideConfigButton.getSide().getLabel());
        if (sideConfigButton.getSide() == RelativeBlockSide.FRONT) {
          lines.add(TextFormatting.GRAY + "Screen");
        }
        if (this.sideToFaceMap.get(sideConfigButton.getSide()) == tile.getInputSide()) {
          lines.add(TextFormatting.GRAY + "Input");
        }
        if (this.sideToFaceMap.get(sideConfigButton.getSide()) == tile.getOutputSide()) {
          lines.add(TextFormatting.GRAY + "Output");
        }
        this.drawHoveringText(lines, mouseX, mouseY);
        break;
      }
    }

    if (this.energyAliasButton.isMouseOver()) {
      this.energyAliasButton.drawTooltip(this, mouseX, mouseY);
    }

    if (this.redstoneControlButton.isMouseOver()) {
      this.redstoneControlButton.drawTooltip(this, mouseX, mouseY);
    }
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    // Left click
    if (mouseButton == 0) {
      for (GuiButtonSideConfig button : this.sideToButtonMap.values()) {
        if (!button.isDisabled() && button.isMouseHovered()) {
          this.sideConfigButtonClicked(button);
          break;
        }
      }
    }

    super.mouseClicked(mouseX, mouseY, mouseButton);
  }

  private void sideConfigButtonClicked(GuiButtonSideConfig button) {
    this.mc.getSoundHandler().playSound(
        PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));

    EnumFacing face = this.sideToFaceMap.get(button.getSide());
    if (face == this.tile.getInputSide()) {
      this.tile.setOutputSide(face);
      this.tile.setInputSide(null);
    } else if (face == this.tile.getOutputSide()) {
      this.tile.setOutputSide(null);
    } else {
      if (this.tile.getInputSide() != null && this.tile.getOutputSide() == null) {
        this.tile.setOutputSide(face);
      } else {
        this.tile.setInputSide(face);
      }
    }

    EnergyMetersMod.NETWORK.sendToServer(new PacketUpdateMeterSides(
        this.tile.getPos(),
        this.tile.getInputSide(),
        this.tile.getOutputSide()));
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    super.actionPerformed(button);

    boolean sendUpdatePacket = false;

    if (button == this.redstoneControlButton) {
      EnumRedstoneControlState newState = this.redstoneControlButton.cycle();
      this.tile.setRedstoneControlState(newState);
      sendUpdatePacket = true;
    }

    if (button == this.energyAliasButton) {
      EnergyAlias newAlias = this.energyAliasButton.cycle();
      this.tile.setEnergyAlias(newAlias);
      sendUpdatePacket = true;
    }

    if (sendUpdatePacket) {
      EnergyMetersMod.NETWORK.sendToServer(
          new PacketUpdateMeterConfig(
            this.tile.getPos(),
            this.tile.getRedstoneControlState(),
            this.tile.getEnergyAlias().getIndex()));
    }
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}
