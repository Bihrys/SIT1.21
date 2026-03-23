package bhw.voident.xyz.sit1_21;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.Block;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

public class SIT1_21 implements ModInitializer {

    @Override
    public void onInitialize() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(entity instanceof PlayerEntity target)) return ActionResult.PASS;
            if (!player.getMainHandStack().isEmpty()) return ActionResult.PASS;

            player.startRiding(target, true);
            return ActionResult.SUCCESS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient) return ActionResult.PASS;

            BlockPos pos = hit.getBlockPos();
            var state = world.getBlockState(pos);
            Block block = state.getBlock();

            boolean isSlab = block instanceof SlabBlock;
            boolean isStair = block instanceof StairsBlock;
            if (!isSlab && !isStair) return ActionResult.PASS;
            if (!player.getMainHandStack().isEmpty()) return ActionResult.PASS;

            ServerWorld serverWorld = (ServerWorld) world;
            ArmorStandEntity seat = EntityType.ARMOR_STAND.create(serverWorld);
            if (seat == null) return ActionResult.PASS;

            NbtCompound nbt = new NbtCompound();
            nbt.putBoolean("Invisible", true);
            nbt.putBoolean("NoGravity", true);
            nbt.putBoolean("Invulnerable", true);
            nbt.putBoolean("Small", true);
            nbt.putBoolean("NoBasePlate", true);
            seat.readNbt(nbt);

            float yaw = SeatOrientation.getPlacementYaw(state, player.getYaw());
            if (isStair) {
                seat.addCommandTag(SeatOrientation.STAIR);
            } else {
                seat.addCommandTag(SeatOrientation.SLAB);
            }

            double y = pos.getY() + 0.15;
            if (state.contains(Properties.BLOCK_HALF) && state.get(Properties.BLOCK_HALF) == BlockHalf.TOP) {
                y += 0.5;
            }

            seat.refreshPositionAndAngles(
                    pos.getX() + 0.5,
                    y,
                    pos.getZ() + 0.5,
                    yaw,
                    0
            );
            seat.addCommandTag(SeatOrientation.SEAT);

            serverWorld.spawnEntity(seat);
            player.startRiding(seat, true);
            return ActionResult.SUCCESS;
        });

        ServerTickEvents.END_SERVER_TICK.register(this::tickSeats);
    }

    private void tickSeats(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            for (PlayerEntity player : world.getPlayers()) {
                if (player.getVehicle() instanceof PlayerEntity) {
                    SeatOrientation.syncFreeRotation(player, player.getYaw());
                }
            }

            for (Entity entity : world.iterateEntities()) {
                if (!(entity instanceof ArmorStandEntity seat)) continue;
                if (!SeatOrientation.isSeat(seat)) continue;

                Entity rider = seat.getFirstPassenger();
                if (!(rider instanceof PlayerEntity player)) {
                    seat.kill();
                    continue;
                }

                if (seat.getCommandTags().contains(SeatOrientation.SLAB)) {
                    float yaw = player.getYaw();
                    SeatOrientation.syncFreeRotation(seat, yaw);
                    SeatOrientation.syncFreeRotation(player, yaw);
                }

                if (seat.getCommandTags().contains(SeatOrientation.STAIR)) {
                    float locked = SeatOrientation.getLockedBodyYaw(world, seat, seat.getYaw());
                    SeatOrientation.syncFreeRotation(seat, locked);
                    SeatOrientation.syncBodyYaw(player, locked);
                }
            }
        }
    }
}
