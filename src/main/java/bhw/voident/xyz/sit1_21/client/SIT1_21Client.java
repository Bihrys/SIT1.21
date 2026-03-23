package bhw.voident.xyz.sit1_21.client;

import bhw.voident.xyz.sit1_21.SeatOrientation;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;

public class SIT1_21Client implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientPlayerEntity player = client.player;
            if (player == null) {
                return;
            }

            Entity vehicle = player.getVehicle();
            if (vehicle == null) {
                return;
            }

            SeatOrientation.RideMode rideMode = SeatOrientation.getRideMode(player.getWorld(), vehicle);
            if (rideMode == SeatOrientation.RideMode.FREE) {
                float yaw = player.getYaw();
                SeatOrientation.syncFreeRotation(player, yaw);

                if (vehicle instanceof ArmorStandEntity armorStand) {
                    SeatOrientation.syncFreeRotation(armorStand, yaw);
                }
            }

            if (rideMode == SeatOrientation.RideMode.LOCKED_STAIR) {
                float locked = SeatOrientation.getLockedBodyYaw(player.getWorld(), vehicle, vehicle.getYaw());
                SeatOrientation.syncBodyYaw(player, locked);

                if (vehicle instanceof ArmorStandEntity armorStand) {
                    SeatOrientation.syncFreeRotation(armorStand, locked);
                }
            }
        });
    }
}
