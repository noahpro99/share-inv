package com.example;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Data classes for synchronizing player state between shared inventory group
 * members
 */
public class PlayerSyncData {

    // Data class to hold hunger information
    public static class HungerData {
        public int foodLevel;
        public float saturationLevel;
        public float exhaustionLevel;

        public HungerData(int foodLevel, float saturationLevel, float exhaustionLevel) {
            this.foodLevel = foodLevel;
            this.saturationLevel = saturationLevel;
            this.exhaustionLevel = exhaustionLevel;
        }

        public static HungerData fromPlayer(PlayerEntity player) {
            var hungerManager = player.getHungerManager();
            return new HungerData(
                    hungerManager.getFoodLevel(),
                    hungerManager.getSaturationLevel(),
                    hungerManager.getExhaustion());
        }

        public void applyToPlayer(PlayerEntity player) {
            var hungerManager = player.getHungerManager();
            hungerManager.setFoodLevel(foodLevel);
            hungerManager.setSaturationLevel(saturationLevel);
            hungerManager.setExhaustion(exhaustionLevel);
        }

        public boolean isDifferentFrom(HungerData other) {
            if (other == null)
                return true;
            return foodLevel != other.foodLevel ||
                    Math.abs(saturationLevel - other.saturationLevel) > 0.01f ||
                    Math.abs(exhaustionLevel - other.exhaustionLevel) > 0.01f;
        }

        public HungerData copy() {
            return new HungerData(foodLevel, saturationLevel, exhaustionLevel);
        }
    }

    // Data class to hold health information
    public static class HealthData {
        public float health;
        public float absorption;

        public HealthData(float health, float absorption) {
            this.health = health;
            this.absorption = absorption;
        }

        public static HealthData fromPlayer(PlayerEntity player) {
            return new HealthData(
                    player.getHealth(),
                    player.getAbsorptionAmount());
        }

        public void applyToPlayer(PlayerEntity player) {
            player.setHealth(health);
            player.setAbsorptionAmount(absorption);
        }

        public boolean isDifferentFrom(HealthData other) {
            if (other == null)
                return true;
            return Math.abs(health - other.health) > 0.01f ||
                    Math.abs(absorption - other.absorption) > 0.01f;
        }

        public HealthData copy() {
            return new HealthData(health, absorption);
        }
    }
}
