package dev.zorg.schizoswap.test;

import dev.zorg.schizoswap.DualStore;
import dev.zorg.schizoswap.ProfileType;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestHelper;

import java.util.UUID;

public class SchizoSwapGameTests implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void command_is_registered(GameTestHelper helper) {
        var server = helper.getWorld().getServer();
        var dispatcher = server.getCommandManager().getDispatcher();
        var node = dispatcher.getRoot().getChild("profileswap");
        if (node == null) {
            helper.fail("profileswap command missing from dispatcher");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void dualstore_defaults_to_survival(GameTestHelper helper) {
        var server = helper.getWorld().getServer();
        var store = DualStore.of(server);
        var profile = store.last(UUID.randomUUID());
        if (profile != ProfileType.SURVIVAL) {
            helper.fail("Expected SURVIVAL by default, got " + profile);
            return;
        }
        helper.succeed();
    }
}

