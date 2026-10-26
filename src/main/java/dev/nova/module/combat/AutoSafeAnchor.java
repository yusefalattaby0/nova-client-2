package dev.nova.module.combat;

import dev.nova.module.Module;
import dev.nova.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Charges a respawn anchor you are aiming at with glowstone, then only sets it off when it is safe for you:
 * far enough away, or with a solid block between you and it. Anchors only explode outside the Nether.
 */
public class AutoSafeAnchor extends Module {
    private final Setting.Bool charge = new Setting.Bool("Auto Charge", true);
    private final Setting.Bool explode = new Setting.Bool("Auto Explode When Safe", true);
    private final Setting.Num minDistance = new Setting.Num("Safe Distance", 7, 3, 12, 1, " blocks");
    private final Setting.Bool allowCover = new Setting.Bool("Safe If Block In Between", true);
    private final Setting.Bool switchBack = new Setting.Bool("Switch Back To Old Slot", true);
    private final Setting.Num delay = new Setting.Num("Delay", 2, 0, 10, 1, " ticks");

    private int cooldown;
    private int oldSlot = -1;

    public AutoSafeAnchor() {
        super("Auto Safe Anchor", "Charges an anchor you aim at and sets it off only when you are safe.", Category.COMBAT);
        add(charge);
        add(explode);
        add(minDistance, explode::get);
        add(allowCover, explode::get);
        add(switchBack);
        add(delay);
    }

    @Override
    public void onTick(Minecraft mc) {
        LocalPlayer p = mc.player;
        Level level = mc.level;
        if (p == null || level == null || mc.gameMode == null || mc.gui.screen() != null) return;
        if (cooldown > 0) { cooldown--; return; }

        HitResult hr = mc.hitResult;
        if (!(hr instanceof BlockHitResult bhr) || hr.getType() != HitResult.Type.BLOCK) { restore(p); return; }
        BlockPos pos = bhr.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.RESPAWN_ANCHOR)) { restore(p); return; }

        int charges = state.getValue(RespawnAnchorBlock.CHARGE);
        if (charges < 1) {
            if (!charge.get()) return;
            if (selectItem(p, true)) {
                mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, bhr);
                p.swing(InteractionHand.MAIN_HAND);
                cooldown = delay.i();
            }
            return;
        }

        if (!explode.get()) return;
        // In the Nether an anchor just fails to work; never "explode" there.
        if (level.dimension() == Level.NETHER) return;
        if (!isSafe(p, level, pos)) return;
        if (selectItem(p, false)) {
            mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, bhr);
            p.swing(InteractionHand.MAIN_HAND);
            cooldown = delay.i();
        }
    }

    private boolean isSafe(LocalPlayer p, Level level, BlockPos pos) {
        Vec3 eye = p.getEyePosition();
        Vec3 target = Vec3.atCenterOf(pos);
        if (eye.distanceTo(target) >= minDistance.d()) return true;
        if (!allowCover.get()) return false;
        BlockHitResult clip = level.clip(new ClipContext(eye, target, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, p));
        // something solid other than the anchor itself blocks the blast path
        return clip.getType() == HitResult.Type.BLOCK && !clip.getBlockPos().equals(pos);
    }

    /** Puts glowstone (true) or a harmless non-glowstone item (false) in the hand. */
    private boolean selectItem(LocalPlayer p, boolean glowstone) {
        var inv = p.getInventory();
        int current = inv.getSelectedSlot();
        if (glowstone == inv.getItem(current).is(Items.GLOWSTONE)) return true;
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getItem(i);
            boolean isGlow = s.is(Items.GLOWSTONE);
            if (glowstone ? isGlow : !isGlow) {
                if (oldSlot < 0) oldSlot = current;
                inv.setSelectedSlot(i);
                return true;
            }
        }
        return false;
    }

    private void restore(LocalPlayer p) {
        if (oldSlot >= 0 && switchBack.get()) p.getInventory().setSelectedSlot(oldSlot);
        oldSlot = -1;
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) restore(mc.player);
        oldSlot = -1;
    }
}
